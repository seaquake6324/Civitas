package com.seaquake6324.civitas.presentation.client;

import com.seaquake6324.civitas.infrastructure.network.RegionDebugPayload;
import com.seaquake6324.civitas.infrastructure.network.TerritoryDebugPayload;
import com.seaquake6324.civitas.infrastructure.network.SystemDebugPayload;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** 八个同色系主题页；这里只展示服务端领域诊断，不在客户端重算规则。 */
public final class RegionDebugOverlay {
    private static final int PAGE_COUNT = 15;
    private static final int[] COLORS = {0xFFFF3B3B, 0xFF55A98F, 0xFFC56C9B, 0xFFA47DD2,
            0xFF579FC3, 0xFF65A979, 0xFFB58C52, 0xFFFFA726,
            0xFF2EC4B6, 0xFFA4C639, 0xFFB3261E, 0xFFE85D04, 0xFFEC6F91, 0xFFD946EF, 0xFF4F46E5};
    private static final String[] TITLES = {"刷怪预算", "区域分类", "文礼演化", "文礼证据",
            "扫描调度", "活动脉络", "领土生命周期", "边境威胁",
            "治安", "巡逻", "守城战斗", "威胁性能", "人口", "生育", "迁移"};
    private static RegionDebugPayload latest;
    private static RegionDebugPayload displayed;
    private static TerritoryDebugPayload territory = TerritoryDebugPayload.empty();
    private static SystemDebugPayload latestSystem=SystemDebugPayload.disabled(),displayedSystem=SystemDebugPayload.disabled();
    private static int page;
    private static boolean frozen;

    public static void accept(RegionDebugPayload payload) {
        if (!payload.enabled()) { hide(); return; }
        latest = payload;
        if (!frozen || displayed == null) displayed = payload;
    }
    public static void acceptTerritory(TerritoryDebugPayload payload) { if (!frozen) territory = payload; }
    public static void acceptSystem(SystemDebugPayload payload){latestSystem=payload;if(!frozen)displayedSystem=payload;}
    public static void hide() { latest = null; displayed = null; territory = TerritoryDebugPayload.empty();latestSystem=SystemDebugPayload.disabled();displayedSystem=SystemDebugPayload.disabled(); frozen = false; }
    public static boolean isVisible() { return displayed != null; }
    public static void cyclePage(int direction) { if (isVisible() && direction != 0) page = Math.floorMod(page + Integer.signum(direction), PAGE_COUNT); }
    public static void toggleFrozen() { if (!isVisible()) return; frozen = !frozen; if (!frozen && latest != null){displayed = latest;displayedSystem=latestSystem;} }

    public static void render(GuiGraphicsExtractor graphics) {
        if (displayed == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) return;
        Font font = minecraft.font;
        int panelWidth = Math.max(300, Math.min(470, graphics.guiWidth() - 24));
        List<Line> lines = lines(displayed, font, panelWidth - 20);
        int height = 20 + lines.size() * 10;
        int x = (graphics.guiWidth() - panelWidth) / 2;
        int y = 8;
        graphics.fill(x, y, x + panelWidth, y + height, 0xE5111419);
        graphics.fill(x, y, x + 4, y + height, COLORS[page]);
        graphics.fill(x + panelWidth - 4, y, x + panelWidth, y + height, COLORS[page]);
        int textY = y + 6;
        for (Line line : lines) {
            int textX = line.center ? x + (panelWidth - font.width(line.text)) / 2 : x + 10;
            graphics.text(font, line.text, textX, textY, line.color, line.shadow);
            textY += 10;
        }
    }

    private static List<Line> lines(RegionDebugPayload payload, Font font, int width) {
        List<Line> out = new ArrayList<>();
        add(out, font, width, "Civitas Debug [" + (page + 1) + "/"+PAGE_COUNT+"] · " + TITLES[page]
                + (frozen ? " · 已冻结" : ""), COLORS[page], true, true);
        switch (page) {
            case 0 -> spawn(out, payload.spawn(), font, width);
            case 1 -> region(out, payload.region(), font, width);
            case 2 -> civility(out, payload.civilization(), payload.serverGameTime(), font, width);
            case 3 -> evidence(out, payload.evidence(), payload.serverGameTime(), font, width);
            case 4 -> schedule(out, payload.schedule(), font, width);
            case 5 -> activity(out, payload.activity(), font, width);
            case 6 -> territory(out, territory, font, width);
            case 7 -> threat(out, territory, font, width);
            default -> system(out,displayedSystem,page-8,font,width);
        }
        add(out, font, width, "滚轮 / Ctrl+Debug 切页 · Shift+Debug 冻结 · Debug 键关闭",
                tone(.74F), false, true);
        return out;
    }

    private static void system(List<Line>out,SystemDebugPayload payload,int index,Font font,int width){
        if(!payload.enabled()||index<0||index>=payload.pages().size()){add(out,font,width,"系统诊断快照尚未到达。",0xFFBFC5CC,false,false);return;}
        SystemDebugPayload.PageData data=payload.pages().get(index);
        for(String line:data.lines())add(out,font,width,line,0xFFD5E8F7,false,false);
    }

    private static void spawn(List<Line> out, RegionDebugPayload.SpawnData d, Font f, int w) {
        add(out,f,w,"实体输入：地表 " + d.surfaceCount() + " · 地下 " + d.undergroundCount() + " · 总数 " + d.totalCount(),0xFFD5E8F7,false,false);
        add(out,f,w,"预算上限：单池 " + d.poolLimit() + " · 总量 " + d.totalLimit() + " · 可刷区块 " + d.spawnableChunks(),0xFFFFD98A,false,false);
        add(out,f,w,"局部半径 " + d.localRadius() + " · 地表/地下 " + d.localSurfaceCount() + "/" + d.localUndergroundCount(),0xFFD5E8F7,false,false);
        add(out,f,w,"尝试 " + d.attempts() + " → 成功 " + d.successful() + "；总量/单池/文礼拒绝 " + d.totalCapRejected() + "/" + d.poolCapRejected() + "/" + d.civilityRejected(),0xFFFFB3A8,false,false);
        add(out,f,w,"最终效果：原版清理 " + d.vanillaDespawns() + " · Civitas 加速清理 " + d.acceleratedDespawns(),0xFFB9F3C9,false,false);
    }

    private static void region(List<Line> out, RegionDebugPayload.RegionData d, Font f, int w) {
        add(out,f,w,"位置 " + d.x() + "," + d.y() + "," + d.z() + " · 单元 " + d.cellX() + "," + d.cellY() + "," + d.cellZ(),0xFFD5E8F7,false,false);
        add(out,f,w,"分类 " + regionName(d.regionType()) + " · 原因编号 " + d.reason() + " · 地下置信 " + pct(d.confidence()),0xFF000000 | d.color(),true,false);
        add(out,f,w,"原始输入：地表中位 " + d.surfaceMedian() + " · 埋深 " + d.burialDepth() + " · 天空样本 " + d.skySamples() + "/9",0xFFD5E8F7,false,false);
        add(out,f,w,"路径/遮蔽：室外距离 " + d.outdoorPath() + " · 覆盖 " + d.coverageMedian() + "/" + d.coverageMaximum() + " · 封闭 " + pct(d.enclosure()),0xFFE7D7FF,false,false);
        add(out,f,w,"工作量：访问 " + d.visitedNodes() + "/" + d.nodeLimit() + " · 本次/平均/最大 " + duration(d.computeNanos()) + "/" + duration(d.rollingAverageNanos()) + "/" + duration(d.rollingMaximumNanos()),0xFFBFC5CC,false,false);
    }

    private static void civility(List<Line> out, RegionDebugPayload.CivilizationData d, long now, Font f, int w) {
        add(out,f,w,"层：" + (d.layer()==1?"地下":"地表") + " · 建筑/设施/安全/连通 " + one(d.building()) + "/" + one(d.facilities()) + "/" + one(d.safety()) + "/" + one(d.connectivity()),0xFFD5E8F7,false,false);
        add(out,f,w,"配置权重 " + pct(d.buildingWeight()) + "/" + pct(d.facilitiesWeight()) + "/" + pct(d.safetyWeight()) + "/" + pct(d.connectivityWeight()) + " → 目标 " + one(d.targetCivility()),0xFFFFD98A,false,false);
        add(out,f,w,"当前文礼 " + one(d.currentCivility()) + " · 活跃度 " + one(d.activity()),0xFFC58BA8,true,false);
        add(out,f,w,"刷怪效果：基础抑制 " + pct(d.baseSuppression()) + " × 活跃修正 " + two(d.activityModifier()) + " = " + pct(d.finalSuppression()),0xFFFFD98A,false,false);
        add(out,f,w,"稳定起点 " + d.stableSince() + " · 最近演化 " + d.lastEvaluated() + " · 距今 " + seconds(Math.max(0,now-d.lastEvaluated())),0xFFBFC5CC,false,false);
    }

    private static void evidence(List<Line> out, RegionDebugPayload.EvidenceData d, long now, Font f, int w) {
        add(out,f,w,"空间：访问/站立/通行/最大连通 " + d.visited() + "/" + d.standable() + "/" + d.passable() + "/" + d.largestConnected(),0xFFD5E8F7,false,false);
        add(out,f,w,"安全：封闭 " + pct(d.enclosure()) + " · 安全通行 " + d.safePassable() + " · 危险防护 " + d.protectedHazards() + "/" + d.hazards(),0xFFE7D7FF,false,false);
        add(out,f,w,"设施分布 " + Arrays.toString(d.facilityPoints()) + " · 连入主空间 " + d.connectedFacilities(),0xFFB9F3C9,false,false);
        add(out,f,w,"领土连接 " + d.connectedEdges() + "/" + d.territoryEdges() + " · 端口 N/E/S/W " + Integer.toHexString(d.northPorts()) + "/" + Integer.toHexString(d.eastPorts()) + "/" + Integer.toHexString(d.southPorts()) + "/" + Integer.toHexString(d.westPorts()),0xFFA990C9,false,false);
        add(out,f,w,d.candidateFingerprint().isEmpty()?"无待稳定候选":"候选 " + d.candidateFingerprint() + " · 目标 " + one(d.candidateTarget()) + " · 已观察 " + seconds(now-d.candidateFirstSeen()),0xFFFFD98A,false,false);
    }

    private static void schedule(List<Line> out, RegionDebugPayload.ScheduleData d, Font f, int w) {
        add(out,f,w,"队列 " + d.queueLength() + "/4096 · 最老任务 " + seconds(d.oldestAge()) + " · 渐进复查 " + d.progressiveRescan(),0xFFD5E8F7,false,false);
        add(out,f,w,"耗时：最近/平均/最大 " + duration(d.lastNanos()) + "/" + duration(d.averageNanos()) + "/" + duration(d.maxNanos()),0xFFE7D7FF,false,false);
        add(out,f,w,"访问单元 " + d.visitedCells() + " · 未加载延期 " + d.unloadedDeferrals() + " · 当前延期 " + d.lastDeferrals(),0xFFFFD98A,false,false);
        add(out,f,w,"文明 schema v" + d.schemaVersion() + " · 迁移 " + d.migrationResult() + " · 只读保护 " + d.readOnly(),d.readOnly()?0xFFFFB3A8:0xFFB9F3C9,false,false);
    }

    private static void activity(List<Line> out, RegionDebugPayload.ActivityData d, Font f, int w) {
        add(out,f,w,"层 " + (d.layer()==1?"地下":"地表") + " · 当前 " + one(d.activity()) + " · 等级 " + d.activityTier(),0xFF79B28E,true,false);
        add(out,f,w,"窗口剩余 " + seconds(d.windowRemaining()) + " · 类别掩码 0x" + Integer.toHexString(d.categoryMask()) + " · 贡献者 " + d.contributors(),0xFFD5E8F7,false,false);
        add(out,f,w,"最近收益：直接 " + one(d.lastDirectGain()) + " · 传播 " + one(d.lastPropagatedGain()) + " · 宽限 " + seconds(d.graceRemaining()),0xFFB9F3C9,false,false);
        add(out,f,w,"衰减 " + one(d.decayPerWindow()) + "/窗口 · 最近补算 " + d.lastDecayPeriods() + " · 传播掩码 0x" + Integer.toHexString(d.propagationMask()),0xFFFFD98A,false,false);
        add(out,f,w,"拒绝：非成员/荒野/自动化/重复/未加载/层不符 " + d.rejectedNonMember() + "/" + d.rejectedWilderness() + "/" + d.rejectedAutomation() + "/" + d.rejectedDuplicate() + "/" + d.rejectedUnloaded() + "/" + d.rejectedLayer(),0xFFFFB3A8,false,false);
    }

    private static void territory(List<Line> out, TerritoryDebugPayload d, Font f, int w) {
        if (!d.owned()) { add(out,f,w,"当前 X/Z 区块不属于任何 Civitas 城池。",0xFFBFC5CC,false,false); return; }
        add(out,f,w,"城池 " + d.city() + " · 领土/腹地 " + d.territory() + "/" + d.heartland() + " · 四向连通 " + d.connected(),0xFFB79B69,true,false);
        add(out,f,w,"城市 schema v" + d.citySchema() + " · 迁移 " + d.cityMigration() + " · 只读保护 " + d.cityReadOnly(),d.cityReadOnly()?0xFFFFB3A8:0xFFB9F3C9,false,false);
        add(out,f,w,"当前区块：外部边数 " + d.externalEdges() + " · 阶段 " + neglectStageName(d.stage()) + " · 认领刻 " + d.claimedAt(),0xFFD5E8F7,false,false);
        add(out,f,w,"认领来源 " + (d.sourceChunk()==Long.MIN_VALUE?"初始腹地":Long.toString(d.sourceChunk())) + " · 沉寂起点 " + d.neglectStartedAt(),0xFFE7D7FF,false,false);
        add(out,f,w,"恢复剩余 " + seconds(d.recoveryRemaining()) + " · 当前文礼/目标/活动 " + one(d.civility()) + "/" + one(d.targetCivility()) + "/" + one(d.activity()),0xFFB9F3C9,false,false);
        add(out,f,w,"最终规则：仅非腹地边境可恶化；回退前再次验证核心与四向连通。",0xFFBFC5CC,false,false);
    }

    private static void threat(List<Line> out, TerritoryDebugPayload d, Font f, int w) {
        if (!d.owned()) { add(out,f,w,"当前区块没有边境数据。",0xFFBFC5CC,false,false); return; }
        add(out,f,w,"方向 " + d.direction() + " · 压力 " + one(d.pressure()) + " · 阶段 " + d.threatPhase() + " · 波次 " + d.wave() + " · 失守 " + d.failedDefenses(),0xFF6FA7B5,true,false);
        add(out,f,w,"压力/秒：低战备 +" + three(d.pressureReadinessGain()) + " · 城市规模 +" + three(d.pressureSizeGain()) + " · 缓冲 -" + three(d.pressureBufferReduction()) + " = +" + three(d.pressureFinalGain()),0xFFFFB3A8,false,false);
        add(out,f,w,"守城/失守冷却剩余 " + seconds(d.threatCooldownRemaining()),0xFFB9F3C9,false,false);
        add(out,f,w,"战备 " + one(d.readiness()) + " = 文礼 " + one(d.readinessCivility()) + " + 活跃 " + one(d.readinessActivity()) + " + 安全 " + one(d.readinessSafety()) + " + 工事 " + one(d.readinessFortification()),0xFFFFD98A,false,false);
        add(out,f,w,"边缘扫描：列 " + d.scannedColumns() + " · 连续墙列 " + d.wallColumns() + " · 受控入口 " + d.entrances() + " · 无防护缺口 " + d.gaps(),0xFFD5E8F7,false,false);
        add(out,f,w,"可通行城门 " + d.gates() + " · 内侧安全通道 " + d.insidePaths() + " · 单次检查上限 80",0xFFE7D7FF,false,false);
        add(out,f,w,"生成点候选 " + d.spawnX() + "," + d.spawnY() + "," + d.spawnZ() + "；正式生成仍会检查实体碰撞。",0xFFB9F3C9,false,false);
        add(out,f,w,"最终规则：无成员在线时只积压；事件生成不占自然刷怪额度。",0xFFBFC5CC,false,false);
    }

    private static void add(List<Line> out, Font font, int width, String text, int color, boolean shadow, boolean center) {
        int themed=themed(color);
        for (FormattedCharSequence part : font.split(Component.literal(text), width)) out.add(new Line(part,themed,shadow,center));
    }
    private static int themed(int requested){int rgb=requested&0xFFFFFF,accent=COLORS[page]&0xFFFFFF;if(rgb==accent)return COLORS[page];float mix=switch(rgb){case 0xFFB3A8->.20F;case 0xFFD98A->.34F;case 0xB9F3C9->.46F;case 0xD5E8F7->.57F;case 0xE7D7FF->.66F;case 0xBFC5CC->.76F;default->.42F;};return tone(mix);}
    private static int tone(float whiteMix){int color=COLORS[page],r=(color>>16)&255,g=(color>>8)&255,b=color&255;r=Math.round(r+(255-r)*whiteMix);g=Math.round(g+(255-g)*whiteMix);b=Math.round(b+(255-b)*whiteMix);return 0xFF000000|(r<<16)|(g<<8)|b;}
    private static String regionName(int value) { return switch(value) { case 0 -> "地表"; case 1 -> "室内"; case 2 -> "地下"; default -> "未知"; }; }
    private static String neglectStageName(String value){return switch(value){case "HEALTHY"->"健康";case "WARNING"->"沉寂";case "ABANDONED"->"荒废";case "RETRACTABLE"->"可回退";default->value;};}
    private static String pct(float value) { return String.format(Locale.ROOT,"%.0f%%",value*100); }
    private static String one(float value) { return String.format(Locale.ROOT,"%.1f",value); }
    private static String two(float value) { return String.format(Locale.ROOT,"%.2f",value); }
    private static String three(float value) { return String.format(Locale.ROOT,"%.3f",value); }
    private static String seconds(long ticks) { return String.format(Locale.ROOT,"%.1fs",ticks/20.0); }
    private static String duration(long nanos) { return nanos<1000?nanos+"ns":nanos<1_000_000?String.format(Locale.ROOT,"%.1fμs",nanos/1000.0):String.format(Locale.ROOT,"%.2fms",nanos/1_000_000.0); }
    private record Line(FormattedCharSequence text, int color, boolean shadow, boolean center) {}
    private RegionDebugOverlay() {}
}
