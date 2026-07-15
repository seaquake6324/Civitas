package com.seaquake6324.civitas.infrastructure.debug;

import com.seaquake6324.civitas.domain.civilization.ChunkCivilization;
import com.seaquake6324.civitas.domain.civilization.CivilizationFactors;
import com.seaquake6324.civitas.domain.civilization.CivilizationLayer;
import com.seaquake6324.civitas.domain.civilization.CivilityEvidence;
import com.seaquake6324.civitas.domain.civilization.CivilityCandidate;
import com.seaquake6324.civitas.domain.civilization.SpawnSuppressionCurve;
import com.seaquake6324.civitas.domain.region.RegionDiagnostics;
import com.seaquake6324.civitas.domain.region.RegionEvidence;
import com.seaquake6324.civitas.domain.region.RegionType;
import com.seaquake6324.civitas.infrastructure.civilization.CivilizationAccess;
import com.seaquake6324.civitas.infrastructure.activity.ActivityManager;
import com.seaquake6324.civitas.infrastructure.civilization.CivilityScanScheduler;
import com.seaquake6324.civitas.infrastructure.persistence.CivilizationSavedData;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.network.RegionDebugPayload;
import com.seaquake6324.civitas.infrastructure.spawn.RegionSpawnQuota;
import com.seaquake6324.civitas.infrastructure.spawn.ServerRegionClassifier;
import com.seaquake6324.civitas.infrastructure.network.TerritoryDebugPayload;
import com.seaquake6324.civitas.infrastructure.network.SystemDebugPayload;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.persistence.ThreatSavedData;
import com.seaquake6324.civitas.infrastructure.border.BoundedBorderScanner;
import com.seaquake6324.civitas.infrastructure.border.BorderThreatManager;
import com.seaquake6324.civitas.domain.border.BorderReadiness;
import com.seaquake6324.civitas.domain.border.BorderFortificationEvidence;
import com.seaquake6324.civitas.domain.territory.TerritoryTopology;
import com.seaquake6324.civitas.domain.territory.TerritoryChunkState;
import com.seaquake6324.civitas.domain.City;
import net.minecraft.world.level.ChunkPos;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/** Maintains the opt-in, per-player server-authoritative three-page debugging snapshot. */
public final class RegionDebugManager {
    private static final Map<UUID, State> ENABLED = new HashMap<>();
    private static final int UPDATE_INTERVAL_TICKS = 20;

    public static void enable(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        ENABLED.put(player.getUUID(), State.start(level));
        update(player, true);
    }

    public static void disable(ServerPlayer player) {
        ENABLED.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, RegionDebugPayload.disabled());
        PacketDistributor.sendToPlayer(player, SystemDebugPayload.disabled());
    }

    public static void toggle(ServerPlayer player) {
        if (ENABLED.containsKey(player.getUUID())) disable(player); else enable(player);
    }

    public static void remove(ServerPlayer player) { ENABLED.remove(player.getUUID()); }

    public static void tick(ServerPlayer player) {
        State state = ENABLED.get(player.getUUID());
        if (state == null || !(player.level() instanceof ServerLevel level)) return;
        if (!state.dimension.equals(level.dimension())) state.restart(level);
        long now = level.getGameTime();
        long cell = cellKey(player.blockPosition());
        if (cell != state.lastCell || now - state.lastUpdate >= UPDATE_INTERVAL_TICKS) update(player, false);
    }

    private static void update(ServerPlayer player, boolean force) {
        if (!(player.level() instanceof ServerLevel level)) return;
        State state = ENABLED.get(player.getUUID());
        if (state == null) return;
        if (!state.dimension.equals(level.dimension())) state.restart(level);
        BlockPos pos = player.blockPosition();
        long now = level.getGameTime();
        long cell = cellKey(pos);
        if (!force && state.lastCell == cell && now - state.lastUpdate < UPDATE_INTERVAL_TICKS) return;

        ServerRegionClassifier.ClassificationSample classification = ServerRegionClassifier.classifyWithMetrics(level, pos);
        RegionDiagnostics diagnostics = classification.diagnostics();
        int color = switch (diagnostics.type()) {
            case SURFACE -> 0x46DF75;
            case INTERIOR -> 0xF0C44D;
            case UNDERGROUND -> 0xB66CFF;
        };
        CivilizationLayer layer = diagnostics.type() == RegionType.UNDERGROUND
                ? CivilizationLayer.UNDERGROUND : CivilizationLayer.SURFACE;
        ChunkCivilization civilization = CivilizationAccess.state(level, pos, layer);
        SpawnSuppressionCurve.Breakdown suppression = CivilizationAccess.suppressionBreakdown(civilization);
        RegionSpawnQuota.DebugSnapshot spawn = RegionSpawnQuota.debugSnapshot(level, player, state.baseline, state.windowStart);

        PacketDistributor.sendToPlayer(player, new RegionDebugPayload(true, now, spawnData(spawn),
                regionData(pos, diagnostics, classification, color), civilizationData(layer, civilization, suppression),
                evidenceData(civilization), scheduleData(level, now), activityData(level, pos, layer, civilization, now)));
        PacketDistributor.sendToPlayer(player,territoryData(level,pos));
        PacketDistributor.sendToPlayer(player,systemData(level));
        renderCell(level, player, pos, color);
        state.lastCell = cell;
        state.lastUpdate = now;
    }

    private static SystemDebugPayload systemData(ServerLevel level){
        var security=com.seaquake6324.civitas.infrastructure.security.SecurityCellManager.metrics();
        var guards=com.seaquake6324.civitas.infrastructure.security.GuardAssignmentManager.metrics();
        var patrol=com.seaquake6324.civitas.infrastructure.security.PatrolExecutionManager.metrics();
        var background=com.seaquake6324.civitas.infrastructure.security.BackgroundBattleManager.metrics();
        var visible=com.seaquake6324.civitas.infrastructure.security.VisibleInvasionCombatManager.metrics();
        var commitments=com.seaquake6324.civitas.infrastructure.security.InvasionCommitmentManager.metrics();
        var threat=com.seaquake6324.civitas.infrastructure.border.BorderThreatManager.metrics();
        var infiltration=com.seaquake6324.civitas.infrastructure.security.InfiltrationPlanner.metrics();
        var population=com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData.get(level.getServer());
        var aging=com.seaquake6324.civitas.infrastructure.population.PopulationAgingManager.metrics();
        var materialization=com.seaquake6324.civitas.infrastructure.population.CitizenMaterializationManager.metrics();
        var reproduction=com.seaquake6324.civitas.infrastructure.population.ReproductionManager.metrics();
        var conditions=com.seaquake6324.civitas.infrastructure.population.ReproductionConditionResolver.metrics();
        var migration=com.seaquake6324.civitas.infrastructure.population.MigrationManager.metrics();
        var outMigration=com.seaquake6324.civitas.infrastructure.population.OutMigrationManager.metrics();
        return new SystemDebugPayload(true,java.util.List.of(
                page("security","队列/评估/延后/缺失 "+security.queued()+"/"+security.assessed()+"/"+security.deferred()+"/"+security.missing(),"发现轮次/截断 "+security.discoveryCycles()+"/"+security.truncatedDiscoveries()+"；当前截断="+security.discoveryTruncated(),"建筑证据截断="+security.buildingEvidenceTruncations()+"；渗透样本/截断/未加载="+security.infiltrationSamples()+"/"+security.infiltrationTruncations()+"/"+security.infiltrationUnloaded(),"平均/最大耗时 "+security.averageMicros()+"/"+security.maxMicros()+" μs"),
                page("patrol","任命 查看/尝试/成功/拒绝/强制="+guards.views()+"/"+guards.attempts()+"/"+guards.assigned()+"/"+guards.rejected()+"/"+guards.forced(),"执行 tick/检查/活动/离班/无装备="+patrol.ticks()+"/"+patrol.examined()+"/"+patrol.active()+"/"+patrol.offShift()+"/"+patrol.unequipped(),"虚拟推进/可见寻路/到点="+patrol.virtualAdvances()+"/"+patrol.visibleDrives()+"/"+patrol.nodesReached(),"失效路线/缺少守卫="+patrol.staleRoutes()+"/"+patrol.missingGuards()+"；最近="+patrol.lastReason()),
                page("battle","承诺 尝试/创建/拒绝="+commitments.attempts()+"/"+commitments.created()+"/"+commitments.rejected(),"参战检查/守卫/平民/截断="+commitments.citizensExamined()+"/"+commitments.guards()+"/"+commitments.civilians()+"/"+commitments.truncatedRosters(),"后台 检查/轮次/敌亡/居民伤害/居民死亡="+background.examined()+"/"+background.rounds()+"/"+background.enemyDefeats()+"/"+background.citizenDamage()+"/"+background.citizenDeaths(),"可见 检查/目标指派/缺承诺/缺目标="+visible.examined()+"/"+visible.assigned()+"/"+visible.noCommitment()+"/"+visible.noTarget()),
                page("threat","tick/区块/边缘/城市轮次="+threat.ticks()+"/"+threat.chunksExamined()+"/"+threat.edgesProcessed()+"/"+threat.cityCycles(),"渗透 拦截/生成样本/生成拒绝="+threat.infiltrationIntercepted()+"/"+threat.infiltrationSpawnSamples()+"/"+threat.infiltrationSpawnRejected(),"计划 尝试/单元/世界样本/候选/选择="+infiltration.attempts()+"/"+infiltration.examinedCells()+"/"+infiltration.worldSamples()+"/"+infiltration.candidates()+"/"+infiltration.selected(),"索引实体/入侵/溢出/拒绝="+threat.indexedMobs()+"/"+threat.indexedInvasions()+"/"+threat.mobIndexOverflow()+"/"+threat.mobIndexRejections()+"；最近="+infiltration.lastReason()),
                page("population","居民/家庭/孕期/孤儿="+population.citizenCount()+"/"+population.householdCount()+"/"+population.pregnancyCount()+"/"+population.orphanCount(),"年龄 处理/阶段变化/死亡/批次="+aging.processed()+"/"+aging.stageChanges()+"/"+aging.deaths()+"/"+aging.batches(),"后台 工作队列/意图队列/进行中/线程="+aging.workQueue()+"/"+aging.intentQueue()+"/"+aging.inFlight()+"/"+aging.workerThreads(),"显现 决策/生成/虚拟化/预热="+materialization.decisions()+"/"+materialization.spawned()+"/"+materialization.virtualized()+"/"+materialization.prewarming(),"schema="+com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData.SCHEMA_VERSION+"；迁移="+population.migrationResult()+"；只读="+population.readOnly()),
                page("reproduction","tick/家庭轮次/家庭检查/合格="+reproduction.tickRuns()+"/"+reproduction.householdRuns()+"/"+reproduction.householdsExamined()+"/"+reproduction.eligibleHouseholds(),"居民检查/非配偶对/配对截断="+reproduction.citizensExamined()+"/"+reproduction.nonSpousePairs()+"/"+reproduction.pairPlansTruncated(),"受孕/失败/随机失败/冷却跳过="+reproduction.conceptions()+"/"+reproduction.conceptionFailures()+"/"+reproduction.failedRolls()+"/"+reproduction.cooldownSkips(),"孕期检查/出生/孕者死亡/出生失败="+reproduction.pregnanciesExamined()+"/"+reproduction.births()+"/"+reproduction.carrierDeaths()+"/"+reproduction.birthFailures(),"条件解析/居民/建筑/截断="+conditions.resolutions()+"/"+conditions.citizensExamined()+"/"+conditions.buildingsExamined()+"/"+conditions.truncatedResolutions()+"；最近="+reproduction.lastReason()),
                page("migration","出现判定/出现/池复用/失败="+migration.rolls()+"/"+migration.appeared()+"/"+migration.reusedFromPool()+"/"+migration.rollFailures(),"吸引力 居民/家庭/建筑/治安检查="+migration.attractionCitizensExamined()+"/"+migration.attractionHouseholdsExamined()+"/"+migration.attractionBuildingsExamined()+"/"+migration.attractionSecurityExamined(),"来源 区块/列/截断="+migration.originChunksExamined()+"/"+migration.originColumnsExamined()+"/"+migration.originScanTruncations(),"组 处理/显现/到达/过期/离开="+migration.groupsProcessed()+"/"+migration.spawned()+"/"+migration.arrived()+"/"+migration.expired()+"/"+migration.departed(),"迁出 家庭/单人检查/合格/启动/上限停="+outMigration.householdsExamined()+"/"+outMigration.singlesExamined()+"/"+outMigration.eligible()+"/"+outMigration.started()+"/"+outMigration.capStops()+"；最近="+migration.lastReason())));
    }
    private static SystemDebugPayload.PageData page(String id,String...lines){return new SystemDebugPayload.PageData(id,java.util.List.of(lines));}

    private static TerritoryDebugPayload territoryData(ServerLevel level,BlockPos pos){
        long chunk=ChunkPos.pack(pos);City city=CitySavedData.get(level.getServer()).cityAt(level.dimension().identifier().toString(),chunk).orElse(null);if(city==null)return TerritoryDebugPayload.empty();
        TerritoryChunkState lifecycle=city.territoryStates().getOrDefault(chunk,TerritoryChunkState.initial(city.activatedAt(),city.heartland().contains(chunk)));var edges=TerritoryTopology.borderEdges(city.territory(),chunk);var direction=edges.isEmpty()?null:edges.iterator().next();
        var surface=CivilizationSavedData.get(level.getServer()).get(city.dimension(),chunk,CivilizationLayer.SURFACE);BorderFortificationEvidence fort=direction==null?BorderFortificationEvidence.calculate(0,0,0,0,0,0):BoundedBorderScanner.scan(level,chunk,direction);BorderReadiness readiness=BorderReadiness.calculate(surface.civility(),surface.activity(),surface.factors().safety(),fort.score(),new BorderReadiness.Weights(CivitasConfig.READINESS_CIVILITY_WEIGHT.get(),CivitasConfig.READINESS_ACTIVITY_WEIGHT.get(),CivitasConfig.READINESS_SAFETY_WEIGHT.get(),CivitasConfig.READINESS_FORTIFICATION_WEIGHT.get()));
        var threat=ThreatSavedData.get(level.getServer()).edgesAt(city.id(),chunk).stream().max(java.util.Comparator.comparingDouble(e->e.getValue().pressure())).orElse(null);var threatState=threat==null?ThreatSavedData.State.calm(level.getGameTime()):threat.getValue();String threatDirection=threat==null?(direction==null?"INTERNAL":direction.name()):threat.getKey().direction().name();long recovery=lifecycle.recoveryStartedAt()==0?0:Math.max(0,CivitasConfig.NEGLECT_RECOVERY_TICKS.get()-(level.getGameTime()-lifecycle.recoveryStartedAt()));
        BlockPos spawn=direction==null?BlockPos.ZERO:BorderThreatManager.previewSpawn(level,city,chunk,direction);CitySavedData cityData=CitySavedData.get(level.getServer());double pressureBase=CivitasConfig.THREAT_BASE_PER_TICK.get()*20.0;double readinessGain=pressureBase*(1-readiness.total()/100.0);double sizeGain=pressureBase*Math.log1p(city.territory().size())*CivitasConfig.THREAT_CITY_SIZE_FACTOR.get();double bufferReduction=threatState.phase()==ThreatSavedData.Phase.COOLDOWN?readinessGain+sizeGain:0;long cooldownRemaining=Math.max(0,threatState.cooldownUntil()-level.getGameTime());
        return new TerritoryDebugPayload(true,city.name(),city.territory().size(),city.heartland().size(),TerritoryTopology.connected(city.territory()),edges.size(),CitySavedData.SCHEMA_VERSION,cityData.migrationResult(),cityData.readOnly(),lifecycle.neglectStage().name(),lifecycle.claimedAt(),lifecycle.sourceChunk()==null?Long.MIN_VALUE:lifecycle.sourceChunk(),lifecycle.neglectStartedAt(),recovery,(float)surface.civility(),(float)surface.targetCivility(),(float)surface.activity(),(float)readiness.total(),(float)readiness.civilityPart(),(float)readiness.activityPart(),(float)readiness.safetyPart(),(float)readiness.fortificationPart(),fort.scannedColumns(),fort.continuousWallColumns(),fort.controlledEntrances(),fort.unprotectedGaps(),fort.passableGates(),fort.safeInsidePathColumns(),(float)threatState.pressure(),(float)readinessGain,(float)sizeGain,(float)bufferReduction,(float)Math.max(0,readinessGain+sizeGain-bufferReduction),cooldownRemaining,threatState.phase().name(),threatDirection,threatState.wave(),threatState.failedDefenses(),spawn.getX(),spawn.getY(),spawn.getZ());
    }

    private static RegionDebugPayload.EvidenceData evidenceData(ChunkCivilization state) {
        CivilityEvidence e = state.evidence();
        CivilityCandidate candidate = state.candidate();
        return new RegionDebugPayload.EvidenceData(e.visitedCells(), e.standableCells(), e.passableCells(),
                e.largestConnectedCells(), (float)e.enclosureRatio(), e.safePassableCells(), e.hazardousBoundaries(),
                e.protectedHazardousBoundaries(), e.facilityDistributionPoints().stream().mapToInt(Integer::intValue).toArray(),
                e.connectedFacilities(), e.territoryEdges(), e.connectedTerritoryEdges(), e.ports().north(), e.ports().east(),
                e.ports().south(), e.ports().west(), state.score().limits().toString(),
                candidate == null ? "" : candidate.fingerprint(), candidate == null ? 0 : (float)candidate.score().target(),
                candidate == null ? 0 : candidate.firstSeen());
    }

    private static RegionDebugPayload.ScheduleData scheduleData(ServerLevel level, long now) {
        String dimension = level.dimension().identifier().toString();
        CivilityScanScheduler.Snapshot s = CivilityScanScheduler.snapshot(dimension, now);
        CivilizationSavedData data = CivilizationSavedData.get(level.getServer());
        return new RegionDebugPayload.ScheduleData(s.queueLength(), s.oldestAge(), s.lastNanos(), s.averageNanos(),
                s.maxNanos(), s.visitedCells(), s.unloadedDeferrals(), s.progressiveRescan(), s.lastReasons(),
                s.lastDeferrals(), 3, data.migrationResult(), data.readOnly());
    }

    private static RegionDebugPayload.ActivityData activityData(ServerLevel level, BlockPos pos,
            CivilizationLayer layer, ChunkCivilization state, long now) {
        ActivityManager.Snapshot snapshot = ActivityManager.snapshot(level, pos);
        var evidence = snapshot.evidence();
        var summary = state.activitySummary();
        var rejected = snapshot.rejections();
        long remaining = Math.max(0, CivitasConfig.ACTIVITY_WINDOW_TICKS.get() - (now - evidence.windowStart()));
        long grace = summary.lastActivityTime() <= 0 ? 0
                : Math.max(0, summary.lastActivityTime() + CivitasConfig.ACTIVITY_GRACE_TICKS.get() - now);
        return new RegionDebugPayload.ActivityData(layer.ordinal(), (float)state.activity(), state.activityTier().ordinal(),
                remaining, evidence.categoryMask(), evidence.contributorCount(), (float)summary.lastDirectGain(),
                (float)summary.lastPropagatedGain(), summary.lastActivityTime(), grace,
                CivitasConfig.ACTIVITY_DECAY_PER_WINDOW.get().floatValue(), summary.lastDecayPeriods(),
                snapshot.propagationMask(), snapshot.cachedWindows(),
                rejected.getOrDefault(ActivityManager.Rejection.NON_MEMBER, 0L),
                rejected.getOrDefault(ActivityManager.Rejection.WILDERNESS, 0L),
                rejected.getOrDefault(ActivityManager.Rejection.NO_PLAYER_SOURCE, 0L),
                rejected.getOrDefault(ActivityManager.Rejection.DUPLICATE_CATEGORY, 0L),
                rejected.getOrDefault(ActivityManager.Rejection.UNLOADED_CHUNK, 0L),
                rejected.getOrDefault(ActivityManager.Rejection.LAYER_MISMATCH, 0L));
    }

    private static RegionDebugPayload.SpawnData spawnData(RegionSpawnQuota.DebugSnapshot snapshot) {
        RegionSpawnQuota.Counters counters = snapshot.counters();
        return new RegionDebugPayload.SpawnData(snapshot.surfaceCount(), snapshot.undergroundCount(),
                snapshot.poolLimit(), snapshot.totalCount(), snapshot.totalLimit(), snapshot.localSurfaceCount(),
                snapshot.localUndergroundCount(), snapshot.localRadius(), snapshot.spawnableChunks(), snapshot.multiplier(),
                snapshot.poolShare(), snapshot.windowTicks(), counters.attempts(), counters.totalCapRejected(),
                counters.poolCapRejected(), counters.civilityRejected(), counters.successful(),
                counters.vanillaDespawns(), counters.acceleratedDespawns());
    }

    private static RegionDebugPayload.RegionData regionData(BlockPos pos, RegionDiagnostics diagnostics,
            ServerRegionClassifier.ClassificationSample sample, int color) {
        RegionEvidence evidence = diagnostics.evidence();
        BlockPos origin = sample.sampleOrigin();
        return new RegionDebugPayload.RegionData(diagnostics.type().ordinal(), diagnostics.reason().ordinal(),
                pos.getX(), pos.getY(), pos.getZ(), sample.cellX(), sample.cellY(), sample.cellZ(),
                origin.getX(), origin.getY(), origin.getZ(), (float)diagnostics.undergroundConfidence(),
                evidence.localSurfaceMedian(), evidence.burialDepth(), evidence.skyExposedSamples(),
                evidence.outdoorReachable() ? evidence.outdoorPathDistance() : -1, evidence.coverageMedian(),
                evidence.coverageMaximum(), (float)evidence.enclosureRatio(), evidence.visitedNodes(),
                evidence.searchExhausted(), CivitasConfig.REGION_NODE_LIMIT.get(), sample.cachedCells(), sample.cacheHit(),
                sample.cacheHits(), sample.cacheMisses(), sample.lastCalculationNanos(), sample.rollingAverageNanos(),
                sample.rollingMaximumNanos(), sample.lastInvalidationReason().ordinal(),
                CivitasConfig.REGION_SURFACE_PATH.get(), CivitasConfig.REGION_INTERIOR_PATH.get(),
                CivitasConfig.REGION_SHALLOW_DEPTH.get(), CivitasConfig.REGION_THIN_COVERAGE.get(),
                CivitasConfig.REGION_UNDERGROUND_DEPTH.get(), CivitasConfig.REGION_UNDERGROUND_COVERAGE.get(),
                CivitasConfig.REGION_ENCLOSURE_THRESHOLD.get().floatValue(),
                CivitasConfig.REGION_UNDERGROUND_SCORE.get().floatValue(), color);
    }

    private static RegionDebugPayload.CivilizationData civilizationData(CivilizationLayer layer,
            ChunkCivilization state, SpawnSuppressionCurve.Breakdown suppression) {
        CivilizationFactors factors = state.factors();
        return new RegionDebugPayload.CivilizationData(layer.ordinal(), (float)factors.building(),
                (float)factors.facilities(), (float)factors.safety(), (float)factors.connectivity(),
                CivitasConfig.CIVILITY_BUILDING_WEIGHT.get().floatValue(),CivitasConfig.CIVILITY_FACILITIES_WEIGHT.get().floatValue(),CivitasConfig.CIVILITY_SAFETY_WEIGHT.get().floatValue(),CivitasConfig.CIVILITY_CONNECTIVITY_WEIGHT.get().floatValue(),
                (float)state.targetCivility(), (float)state.civility(), (float)state.activity(),
                (float)suppression.baseSuppression(), (float)suppression.activityModifier(),
                (float)suppression.finalSuppression(), state.stableSince(), state.lastEvaluated());
    }

    private static void renderCell(ServerLevel level, ServerPlayer player, BlockPos pos, int color) {
        int size = ServerRegionClassifier.CELL_SIZE;
        int minX = Math.floorDiv(pos.getX(), size) * size;
        int minY = Math.floorDiv(pos.getY(), size) * size;
        int minZ = Math.floorDiv(pos.getZ(), size) * size;
        DustParticleOptions dust = new DustParticleOptions(color, 1.0F);
        for (int i = 0; i <= size * 2; i++) {
            double t = i / 2.0;
            for (int a : new int[]{0, size}) for (int b : new int[]{0, size}) {
                particle(level, player, dust, minX + t, minY + a, minZ + b);
                particle(level, player, dust, minX + a, minY + t, minZ + b);
                particle(level, player, dust, minX + a, minY + b, minZ + t);
            }
        }
    }

    private static void particle(ServerLevel level, ServerPlayer player, DustParticleOptions dust, double x, double y, double z) {
        level.sendParticles(player, dust, true, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private static long cellKey(BlockPos pos) {
        int size = ServerRegionClassifier.CELL_SIZE;
        return BlockPos.asLong(Math.floorDiv(pos.getX(), size), Math.floorDiv(pos.getY(), size), Math.floorDiv(pos.getZ(), size));
    }

    private static final class State {
        private ResourceKey<Level> dimension;
        private long lastCell;
        private long lastUpdate;
        private long windowStart;
        private RegionSpawnQuota.Counters baseline;

        private static State start(ServerLevel level) {
            State state = new State();
            state.restart(level);
            return state;
        }

        private void restart(ServerLevel level) {
            dimension = level.dimension();
            lastCell = Long.MIN_VALUE;
            lastUpdate = Long.MIN_VALUE;
            windowStart = level.getGameTime();
            baseline = RegionSpawnQuota.counters(level);
        }
    }

    private RegionDebugManager() {}
}
