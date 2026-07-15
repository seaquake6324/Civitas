package com.seaquake6324.civitas.infrastructure.debug;

import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.civilization.CivilizationLayer;
import com.seaquake6324.civitas.domain.territory.ExpansionEligibility;
import com.seaquake6324.civitas.domain.territory.NeglectStage;
import com.seaquake6324.civitas.domain.territory.TerritoryChunkState;
import com.seaquake6324.civitas.domain.territory.TerritoryTopology;
import com.seaquake6324.civitas.infrastructure.activity.ActivityManager;
import com.seaquake6324.civitas.infrastructure.border.BorderThreatManager;
import com.seaquake6324.civitas.infrastructure.civilization.CivilityDirtyQueue;
import com.seaquake6324.civitas.infrastructure.civilization.CivilityScanScheduler;
import com.seaquake6324.civitas.infrastructure.civilization.CivilizationAccess;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.persistence.CivilizationSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.ThreatSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.BuildingSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.CitizenEquipmentSavedData;
import com.seaquake6324.civitas.domain.building.BuildingCapacitySummary;
import com.seaquake6324.civitas.domain.building.BuildingPurpose;
import com.seaquake6324.civitas.domain.building.BuildingStatus;
import com.seaquake6324.civitas.infrastructure.world.BuildingRegistrationManager;
import com.seaquake6324.civitas.infrastructure.world.StorageAuthorizationManager;
import com.seaquake6324.civitas.infrastructure.building.BuildingRevalidationManager;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import com.seaquake6324.civitas.infrastructure.population.PopulationAgingManager;
import com.seaquake6324.civitas.infrastructure.population.CitizenMaterializationManager;
import com.seaquake6324.civitas.infrastructure.population.MarriageInteractionManager;
import com.seaquake6324.civitas.infrastructure.population.MarriageProposalMaintenanceManager;
import com.seaquake6324.civitas.infrastructure.population.NpcMarriageManager;
import com.seaquake6324.civitas.infrastructure.population.ReproductionManager;
import com.seaquake6324.civitas.infrastructure.population.ReproductionInteractionManager;
import com.seaquake6324.civitas.infrastructure.population.AdoptionManager;
import com.seaquake6324.civitas.infrastructure.population.PlayerAdoptionManager;
import com.seaquake6324.civitas.infrastructure.population.CitizenCombatDeathManager;
import com.seaquake6324.civitas.infrastructure.population.CitizenEquipmentDeathDropManager;
import com.seaquake6324.civitas.infrastructure.population.MigrationManager;
import com.seaquake6324.civitas.infrastructure.population.OutMigrationManager;
import com.seaquake6324.civitas.infrastructure.security.SecurityCellManager;
import com.seaquake6324.civitas.infrastructure.security.GuardAssignmentManager;
import com.seaquake6324.civitas.infrastructure.security.PatrolExecutionManager;
import com.seaquake6324.civitas.infrastructure.security.InfiltrationPlanner;
import com.seaquake6324.civitas.infrastructure.world.PatrolRouteManager;
import com.seaquake6324.civitas.application.BuildPopulationDashboardService;
import com.seaquake6324.civitas.domain.population.PopulationSummary;
import com.seaquake6324.civitas.domain.population.AgePhysicalRules;
import com.seaquake6324.civitas.domain.population.CitizenRace;
import com.seaquake6324.civitas.domain.population.Gender;
import com.seaquake6324.civitas.domain.population.CitizenNameGenerator;
import com.seaquake6324.civitas.domain.population.CitizenRuntimeState;
import com.seaquake6324.civitas.application.CreateCitizenService;
import com.seaquake6324.civitas.infrastructure.registry.CivitasEntities;
import net.minecraft.world.entity.EntitySpawnReason;
import com.seaquake6324.civitas.infrastructure.entity.MaterializationLeaseManager;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/** 管理员权限由命令根统一校验；所有结果都只发往 Action Bar。 */
public final class AdminDebugTools {
    public static int rescan(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        ChunkPos chunk = ChunkPos.containing(player.blockPosition());
        CivilityScanScheduler.markDirty(level, chunk.x(), chunk.z(), CivilityDirtyQueue.Reason.ADMIN_RESCAN);
        return ok(player, "重扫已排队：区块 " + chunk.x() + "," + chunk.z()
                + "，层 " + CivilizationAccess.layer(level, player.blockPosition()));
    }

    public static int submitCandidate(ServerPlayer player) {
        rescan(player);
        return ok(player, "文礼候选已排队；证据页将显示指纹、目标值和稳定等待时间");
    }

    public static int evolve(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        long chunk = ChunkPos.pack(player.blockPosition());
        CivilizationLayer layer = CivilizationAccess.layer(level, player.blockPosition());
        CivilizationSavedData data = CivilizationSavedData.get(level.getServer());
        var before = data.get(level.dimension().identifier().toString(), chunk, layer);
        var after = before.approachTarget(CivitasConfig.CIVILITY_GROWTH_PER_CYCLE.get(),
                CivitasConfig.CIVILITY_DECLINE_PER_CYCLE.get(), level.getGameTime());
        data.put(level.dimension().identifier().toString(), chunk, layer, after);
        return ok(player, "文礼推进：" + one(before.civility()) + " → " + one(after.civility())
                + "，目标 " + one(after.targetCivility()));
    }

    public static int settleActivity(ServerPlayer player) {
        double gain = ActivityManager.debugSettleCurrentWindow((ServerLevel) player.level(), player.blockPosition());
        return ok(player, "活动窗口已结算：本次增益 " + one(gain));
    }

    public static int setActivity(ServerPlayer player, int value) {
        ServerLevel level = (ServerLevel) player.level();
        long chunk = ChunkPos.pack(player.blockPosition());
        CivilizationLayer layer = CivilizationAccess.layer(level, player.blockPosition());
        CivilizationSavedData data = CivilizationSavedData.get(level.getServer());
        var before = data.get(level.dimension().identifier().toString(), chunk, layer);
        int clamped = Math.max(0, Math.min(100, value));
        data.put(level.dimension().identifier().toString(), chunk, layer,
                before.withActivity(clamped, level.getGameTime()));
        return ok(player, "活跃度：" + one(before.activity()) + " → " + clamped + "，层 " + layer);
    }

    public static int expansionBreakdown(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        City city = city(player);
        if (city == null) return fail(player, "当前区块和成员身份没有对应城池");
        long target = ChunkPos.pack(player.blockPosition());
        CivilizationSavedData civilizations = CivilizationSavedData.get(level.getServer());
        Map<Long, ExpansionEligibility.ChunkHealth> health = new HashMap<>();
        for (long chunk : city.territory()) {
            var surface = civilizations.get(city.dimension(), chunk, CivilizationLayer.SURFACE);
            var underground = civilizations.get(city.dimension(), chunk, CivilizationLayer.UNDERGROUND);
            health.put(chunk, new ExpansionEligibility.ChunkHealth(surface.civility(), surface.activity(),
                    underground.civility(), underground.activity()));
        }
        CitySavedData cities = CitySavedData.get(level.getServer());
        boolean claimed = cities.cityAt(city.dimension(), target).isPresent();
        boolean buffer = cities.cities().stream()
                .filter(other -> !other.id().equals(city.id()) && other.dimension().equals(city.dimension()))
                .flatMap(other -> other.territory().stream())
                .anyMatch(chunk -> ChunkCoordinate.unpack(chunk).chebyshevDistance(ChunkCoordinate.unpack(target))
                        <= CivitasConfig.BORDER_BUFFER_CHUNKS.get());
        var rules = new ExpansionEligibility.Rules(CivitasConfig.EXPANSION_CIVILITY_MINIMUM.get(),
                CivitasConfig.EXPANSION_ACTIVITY_MINIMUM.get(), CivitasConfig.EXPANSION_DEVELOPED_COVERAGE.get(),
                CivitasConfig.EXPANSION_ACTIVE_COVERAGE.get(), CivitasConfig.EXPANSION_COOLDOWN_TICKS.get());
        long source = city.territory().stream().filter(chunk -> {
            for (var direction : TerritoryTopology.Direction.values())
                if (TerritoryTopology.adjacent(chunk, direction) == target) return true;
            return false;
        }).sorted((a,b)->Boolean.compare(health.getOrDefault(b,new ExpansionEligibility.ChunkHealth(0,0,0,0)).developed(rules.civilityMinimum(),rules.activityMinimum()),health.getOrDefault(a,new ExpansionEligibility.ChunkHealth(0,0,0,0)).developed(rules.civilityMinimum(),rules.activityMinimum()))).findFirst().orElse(Long.MIN_VALUE);
        ChunkCoordinate coordinate = ChunkCoordinate.unpack(target);
        var context = new ExpansionEligibility.Context(city.mayManage(player.getUUID()),
                CivitasConfig.ALLOWED_DIMENSIONS.get().contains(city.dimension()),
                level.hasChunk(coordinate.x(), coordinate.z()), claimed, buffer, level.getGameTime(),
                target, source, health);
        var result = ExpansionEligibility.evaluate(city, context, rules);
        return ok(player, "扩张分解：合格=" + result.eligible() + "，失败项=" + result.failures()
                + "，开发=" + result.developed() + "/" + result.total()
                + "，活跃=" + result.active() + "/" + result.total() + "，来源=" + source);
    }

    public static int pressure(ServerPlayer player, double value, boolean relative, boolean start) {
        City city = city(player);
        if (city == null) return fail(player, "没有可操作的城池");
        long chunk = ChunkPos.pack(player.blockPosition());
        var edges = TerritoryTopology.borderEdges(city.territory(), chunk);
        if (edges.isEmpty()) return fail(player, "当前区块不是边境");
        var direction = edges.iterator().next();
        ThreatSavedData data = ThreatSavedData.get(player.level().getServer());
        var key = new ThreatSavedData.EdgeKey(city.id(), chunk, direction);
        long now = player.level().getGameTime();
        var old = data.get(key, now);
        double next = Math.max(0, Math.min(100, relative ? old.pressure() + value : value));
        var phase = start ? ThreatSavedData.Phase.WARNING : next <= 0 ? ThreatSavedData.Phase.CALM : old.phase();
        long started = start ? now - CivitasConfig.INVASION_WARNING_TICKS.get() : now;
        data.put(key, new ThreatSavedData.State(next, phase, started, 0, start ? 0 : old.wave(),
                start ? null : old.invasionId(), start ? 0 : old.remainingMobs(), old.failedDefenses(), now));
        return ok(player, (start ? "测试入侵已进入预警结算" : "边境压力：" + one(old.pressure())
                + " → " + one(next)) + "，方向 " + direction);
    }

    public static int neglect(ServerPlayer player, boolean reset) {
        City city = city(player);
        if (city == null) return fail(player, "没有可操作的城池");
        long chunk = ChunkPos.pack(player.blockPosition());
        TerritoryChunkState state = city.territoryStates().get(chunk);
        if (state == null) return fail(player, "当前区块不属于该城池");
        NeglectStage next = reset ? NeglectStage.HEALTHY : state.neglectStage().worsen();
        CitySavedData.get(player.level().getServer()).add(city.withTerritoryState(chunk,
                state.withNeglect(next, player.level().getGameTime())));
        return ok(player, "荒废阶段：" + state.neglectStage() + " → " + next);
    }

    public static int topology(ServerPlayer player) {
        City city = city(player);
        if (city == null) return fail(player, "没有可验证的城池");
        return ok(player, "拓扑验证：四向连通=" + TerritoryTopology.connected(city.territory())
                + "，核心在领土内=" + city.territory().contains(city.coreChunk())
                + "，区块=" + city.territory().size());
    }

    public static int threatPerformance(ServerPlayer player){var metrics=BorderThreatManager.metrics();var planner=InfiltrationPlanner.metrics();City current=city(player);ThreatSavedData threat=ThreatSavedData.get(player.level().getServer());var page=current==null?null:threat.edgePage(current.id(),CivitasConfig.THREAT_EDGE_QUERY_LIMIT.get());int persistedRemaining=page==null?0:page.entries().stream().mapToInt(e->e.getValue().remainingMobs()).sum();var activePlan=page==null?null:page.entries().stream().map(e->threat.infiltrationPlan(e.getKey()).orElse(null)).filter(java.util.Objects::nonNull).findFirst().orElse(null);String plan=activePlan==null?"无":"来源="+activePlan.selected().source()+"，位置="+BlockPos.of(activePlan.selected().position())+"，候选="+activePlan.candidates().stream().map(c->c.source()+"@"+BlockPos.of(c.position())+":来源"+one(c.sourceWeight())+":风险"+one(c.securityRisk())+":黑暗"+one(c.darkness())+":无守卫"+one(c.unguarded())+":最终"+one(c.finalWeight())).toList()+"，选择="+activePlan.selectedIndex()+"，城市修订="+activePlan.cityRevision()+"，单元/世界样本="+activePlan.examinedCells()+"/"+activePlan.worldSamples()+"，截断="+activePlan.truncated();return ok(player,"威胁性能：tick="+metrics.ticks()+"，检查区块/处理边缘="+metrics.chunksExamined()+"/"+metrics.edgesProcessed()+"，城市轮次="+metrics.cityCycles()+"，过期检查/移除="+metrics.cleanupExamined()+"/"+metrics.cleanupRemoved()+"，平均/最大="+metrics.averageMicros()+"/"+metrics.maxMicros()+"μs；渗透计划="+plan+"；计划 尝试/单元/世界样本/候选/选择/无来源/截断/失效/未加载="+planner.attempts()+"/"+planner.examinedCells()+"/"+planner.worldSamples()+"/"+planner.candidates()+"/"+planner.selected()+"/"+planner.noCandidate()+"/"+planner.truncatedPlans()+"/"+planner.staleRevalidations()+"/"+planner.unloadedCells()+"，平均/最大="+planner.averageMicros()+"/"+planner.maxMicros()+"μs，最近="+planner.lastReason()+"；拦截/生成样本/生成拒绝="+metrics.infiltrationIntercepted()+"/"+metrics.infiltrationSpawnSamples()+"/"+metrics.infiltrationSpawnRejected()+"；入侵实体索引="+metrics.indexedMobs()+"，事件="+metrics.indexedInvasions()+"，持久剩余敌人="+persistedRemaining+"，证据截断="+(page!=null&&page.truncated())+"，溢出="+metrics.mobIndexOverflow()+"，拒绝="+metrics.mobIndexRejections()+"，清理失效="+metrics.staleMobsRemoved()+"；区块/过期/实体上限="+CivitasConfig.THREAT_BORDER_CHUNKS_PER_TICK.get()+"/"+CivitasConfig.THREAT_STALE_EDGES_PER_TICK.get()+"/"+CivitasConfig.THREAT_MOB_INDEX_CAP.get()+"，schema/计划隔离="+ThreatSavedData.SCHEMA_VERSION+"/"+threat.quarantinedInfiltrationPlans());}

    public static int battle(ServerPlayer player){var server=player.level().getServer();var threat=ThreatSavedData.get(server);var background=com.seaquake6324.civitas.infrastructure.security.BackgroundBattleManager.metrics();var visible=com.seaquake6324.civitas.infrastructure.security.VisibleInvasionCombatManager.metrics();var commitments=com.seaquake6324.civitas.infrastructure.security.InvasionCommitmentManager.metrics();return ok(player,"守城战斗诊断：承诺数="+threat.invasionCommitmentCount()+"，创建/拒绝/检查居民/守卫/平民/截断="+commitments.created()+"/"+commitments.rejected()+"/"+commitments.citizensExamined()+"/"+commitments.guards()+"/"+commitments.civilians()+"/"+commitments.truncatedRosters()+"；后台 检查/轮次/可见跳过/索引不全/间隔跳过/敌亡/居民伤害/居民死亡/失效="+background.examined()+"/"+background.rounds()+"/"+background.loadedSkips()+"/"+background.incompleteSkips()+"/"+background.intervalSkips()+"/"+background.enemyDefeats()+"/"+background.citizenDamage()+"/"+background.citizenDeaths()+"/"+background.stale()+"，平均/最大="+(background.batches()==0?0:background.totalMicros()/background.batches())+"/"+background.maximumMicros()+"μs，最近="+background.lastReason()+"；可见 检查/目标指派/缺承诺/缺显现目标="+visible.examined()+"/"+visible.assigned()+"/"+visible.noCommitment()+"/"+visible.noTarget()+"；上限 承诺/tick参战者/恢复敌人/可见敌人="+CivitasConfig.BACKGROUND_BATTLES_PER_TICK.get()+"/"+CivitasConfig.BACKGROUND_BATTLE_PARTICIPANT_CAP.get()+"/"+CivitasConfig.VIRTUAL_INVASION_MATERIALIZATIONS_PER_TICK.get()+"/"+CivitasConfig.VISIBLE_INVASION_MOBS_PER_TICK.get()+"；schema/承诺隔离/只读="+ThreatSavedData.SCHEMA_VERSION+"/"+threat.quarantinedInvasionCommitments()+"/"+threat.readOnly());}

    public static int building(ServerPlayer player) {
        City city = city(player);
        if (city == null) return fail(player, "没有可诊断的城池");
        BuildingSavedData data = BuildingSavedData.get(player.level().getServer());
        int scanLimit = CivitasConfig.BUILDING_DASHBOARD_SCAN_LIMIT.get();
        var page = data.cityPage(city.id(), scanLimit);var records=page.records();
        boolean truncated = page.truncated();
        var summary = BuildingCapacitySummary.from(records);
        var requirements = java.util.Arrays.stream(BuildingPurpose.values()).collect(java.util.stream.Collectors.toMap(
                purpose -> purpose, purpose -> com.seaquake6324.civitas.domain.building.BuildingRequirement.forPurpose(purpose),
                (left, right) -> left, java.util.LinkedHashMap::new));
        var metrics = BuildingRegistrationManager.metrics();
        var revalidation = BuildingRevalidationManager.metrics();
        int ports=records.stream().mapToInt(record->record.features().boundaryPorts().size()).sum();int workstations=records.stream().mapToInt(record->record.features().workstations().size()).sum();int storage=records.stream().mapToInt(record->record.features().storageEndpoints().size()).sum();int authorizedStorage=records.stream().mapToInt(record->record.authorizedStorageEndpoints().size()).sum();long connected=records.stream().filter(record->record.features().entranceConnected()).count();var storageMetrics=StorageAuthorizationManager.metrics();
        return ok(player, "建筑诊断：有效=" + summary.validBuildings() + "，待复核=" + summary.staleBuildings() + "，确认失效=" + summary.invalidBuildings()
                + "，住房容量=" + summary.housingCapacity() + "，守卫容量=" + summary.guardCapacity()
                + "，用途容量=" + summary.capacityByPurpose() + "，端口/入口连通/工作站/已授权仓储/仓储端点="+ports+"/"+connected+"/"+workstations+"/"+authorizedStorage+"/"+storage+"，规则=" + requirements
                + "，读取上限=" + scanLimit + "，截断=" + truncated + "；扫描=" + metrics.scans()
                + "，接受=" + metrics.accepted() + "，拒绝=" + metrics.rejected()
                + "，过期=" + metrics.staleResults() + "，平均=" + metrics.averageMicros()
                + "μs，最大访问/队列峰值=" + metrics.maxVisited() + "/" + metrics.maxQueued() + "，活动会话=" + metrics.activeSessions()
                + "；仓储授权启动/接受/撤销/拒绝/超时/活动="+storageMetrics.started()+"/"+storageMetrics.accepted()+"/"+storageMetrics.revoked()+"/"+storageMetrics.rejected()+"/"+storageMetrics.expired()+"/"+storageMetrics.activeSessions()
                + "；重验队列/峰值=" + revalidation.queued() + "/" + revalidation.queuePeak() + "，尝试=" + revalidation.attempted()
                + "，恢复=" + revalidation.restored() + "，仍失效=" + revalidation.stillInvalid()
                + "，延后=" + revalidation.deferred() + "，丢弃=" + revalidation.dropped()
                + "，重验平均=" + revalidation.averageMicros() + "μs"
                + "；schema=" + BuildingSavedData.SCHEMA_VERSION + "，迁移=" + data.migrationResult()
                + "，隔离=" + data.quarantinedRecords() + "，只读=" + data.readOnly());
    }

    public static int population(ServerPlayer player) {
        PopulationSavedData data=PopulationSavedData.get(player.level().getServer());CitySavedData cityData=CitySavedData.get(player.level().getServer());var rules=PopulationAgingManager.rules();
        var aging=PopulationAgingManager.metrics();var leases=MaterializationLeaseManager.metrics();var visible=CitizenMaterializationManager.metrics();var marriage=MarriageInteractionManager.metrics();var proposalMaintenance=MarriageProposalMaintenanceManager.metrics();var npcMarriage=NpcMarriageManager.metrics();var reproduction=ReproductionManager.metrics();var reproductionInteraction=ReproductionInteractionManager.metrics();var adoption=AdoptionManager.metrics();var playerAdoption=PlayerAdoptionManager.metrics();var combatDeaths=CitizenCombatDeathManager.metrics();var migration=MigrationManager.metrics();var outMigration=OutMigrationManager.metrics();var proposalDiagnostics=data.proposalDiagnostics(CivitasConfig.MARRIAGE_MAX_STORED_PROPOSALS.get());var deathDiagnostics=data.deathDiagnostics(CivitasConfig.POPULATION_DASHBOARD_SCAN_LIMIT.get());
        City city=city(player);String summary="无本城摘要";if(city!=null){int limit=CivitasConfig.POPULATION_DASHBOARD_SCAN_LIMIT.get();var citizenPage=data.citizenPage(city.id(),limit);var householdPage=data.householdPage(city.id(),limit);var citizens=citizenPage.records();var households=householdPage.records();var buildingPage=BuildingSavedData.get(player.level().getServer()).cityPage(city.id(),limit);var buildings=buildingPage.records();boolean truncated=citizenPage.truncated()||householdPage.truncated()||buildingPage.truncated();var dashboard=new BuildPopulationDashboardService().build(city.id(),citizens,households,buildings,rules,truncated);var value=dashboard.composition();summary="本城总数="+value.total()+"，性别="+value.genders()+"，年龄="+value.ages()+"，种族="+value.races()+"，状态="+value.runtimeStates()+"；住房容量/有效入住/未分配/失效引用="+dashboard.housingCapacity()+"/"+dashboard.housedCitizens()+"/"+dashboard.unassignedCitizens()+"/"+dashboard.invalidResidenceReferences()+"，就业="+dashboard.employedCitizens()+"，家庭/伴侣家庭/有子女="+dashboard.householdCount()+"/"+dashboard.partneredHouseholds()+"/"+dashboard.householdsWithChildren()+"，永久死亡/最近 tick="+dashboard.permanentDeaths()+"/"+dashboard.latestDeathAtTick()+"，食物/住房保障="+one(dashboard.averageFoodCoverage())+"/"+one(dashboard.averageHousingCoverage())+"，定居/迁出意愿="+one(dashboard.averageSettlementWillingness())+"/"+one(dashboard.averageMigrationWillingness())+"，限制="+dashboard.limitations()+"，截断="+dashboard.truncated()+"，检查居民/家庭/建筑="+citizenPage.examined()+"/"+householdPage.examined()+"/"+buildingPage.examined();}
        return ok(player,"人口诊断："+summary+"；全局居民="+data.citizenCount()+"，家庭="+data.householdCount()+"，玩家档案="+data.profileCount()
                +"；年龄原始配置="+CivitasConfig.POPULATION_TICKS_PER_YEAR.get()+" tick/年，阶段="+rules.adolescentAt()+"/"+rules.youngAdultAt()+"/"+rules.matureAdultAt()+"/"+rules.elderAt()+"，体型/生命/移速/攻击/跨步="+java.util.Arrays.stream(com.seaquake6324.civitas.domain.population.AgeStage.values()).collect(java.util.stream.Collectors.toMap(Enum::name,s->{var p=new AgePhysicalRules().forStage(s);return one(p.bodyScale())+"/"+one(p.maxHealth())+"/"+one(p.movementSpeed())+"/"+one(p.attackDamage())+"/"+one(p.stepHeight());},(a,b)->a,java.util.LinkedHashMap::new))+"，配置修正="+aging.configAdjusted()
                +"；批次="+aging.batches()+"，处理="+aging.processed()+"，阶段变化="+aging.stageChanges()+"，永久死亡="+aging.deaths()+"，平均="+aging.averageBatchMicros()+"μs，游标="+data.simulationCursor()
                +"；后台线程="+aging.workerThreads()+"，快照/提交/完成/应用="+aging.snapshots()+"/"+aging.submitted()+"/"+aging.workerCompleted()+"/"+aging.processed()+"，工作/意图/在途队列="+aging.workQueue()+"/"+aging.intentQueue()+"/"+aging.inFlight()+"，工作拒绝/结果丢弃/异常="+aging.taskRejected()+"/"+aging.resultDropped()+"/"+aging.workerFailures()+"，重复在途/旧结果/过期/缺依赖/亡偶转换失败="+aging.duplicateInFlight()+"/"+aging.staleResults()+"/"+aging.expiredResults()+"/"+aging.missingDependencies()+"/"+aging.partnerDeathFailures()+"，后台平均/最大="+aging.averageWorkerMicros()+"/"+aging.maxWorkerMicros()+"μs，提交平均/最大="+aging.averageApplyMicros()+"/"+aging.maxApplyMicros()+"μs，最近原因="+aging.lastReason()
                +"；显现预热/决策/生成/虚拟化="+visible.prewarming()+"/"+visible.decisions()+"/"+visible.spawned()+"/"+visible.virtualized()
                +"，实体上限/预热上限/节点拒绝/过期="+visible.rejectedCap()+"/"+visible.rejectedPrewarm()+"/"+visible.rejectedNode()+"/"+visible.staleResults()+"，平均="+visible.averageDecisionMicros()+"μs，最近原因="+visible.lastReason()
                +"；实体租约="+leases.active()+"，获取/复用/拒绝/释放="+leases.acquired()+"/"+leases.reused()+"/"+leases.rejected()+"/"+leases.released()
                +"；婚姻提议="+data.proposalCount()+"/"+CivitasConfig.MARRIAGE_MAX_STORED_PROPOSALS.get()+"，冷却记录="+data.cooldownCount()+"，状态="+proposalDiagnostics.statusCounts()+"，诊断检查/截断="+proposalDiagnostics.examined()+"/"+proposalDiagnostics.truncated()+"，交互尝试/成立/拒绝/旧修订="+marriage.attempts()+"/"+marriage.accepted()+"/"+marriage.rejected()+"/"+marriage.stale()+"，近距实体检查="+marriage.examinedEntities()+"；维护批次/检查/到期/清理="+proposalMaintenance.batches()+"/"+proposalMaintenance.examined()+"/"+proposalMaintenance.expired()+"/"+proposalMaintenance.purged()+"，平均/最大="+proposalMaintenance.averageBatchMicros()+"/"+proposalMaintenance.maxBatchMicros()+"μs，游标="+data.proposalMaintenanceCursor()+"；NPC婚配轮次/检查/合格/成立/拒绝/旧修订="+npcMarriage.runs()+"/"+npcMarriage.examined()+"/"+npcMarriage.eligible()+"/"+npcMarriage.formed()+"/"+npcMarriage.rejected()+"/"+npcMarriage.stale()+"，平均/最大="+npcMarriage.averageMicros()+"/"+npcMarriage.maxMicros()+"μs，游标="+data.marriageMatchingCursor()+"；配置意愿/提议冷却/家庭子女容量="+CivitasConfig.MARRIAGE_NPC_WILLINGNESS_MINIMUM.get()+"/"+CivitasConfig.MARRIAGE_PROPOSAL_COOLDOWN_TICKS.get()+"/"+CivitasConfig.HOUSEHOLD_CHILD_CAPACITY.get()
                +"；生育 tick/家庭轮次/检查/合格/受孕/受孕失败/随机失败/玩家同意延后="+reproduction.tickRuns()+"/"+reproduction.householdRuns()+"/"+reproduction.householdsExamined()+"/"+reproduction.eligibleHouseholds()+"/"+reproduction.conceptions()+"/"+reproduction.conceptionFailures()+"/"+reproduction.failedRolls()+"/"+reproduction.playerConsentDeferred()+"，玩家交互 尝试/受孕/拒绝/实体检查/过期="+reproductionInteraction.attempts()+"/"+reproductionInteraction.conceived()+"/"+reproductionInteraction.rejected()+"/"+reproductionInteraction.examinedEntities()+"/"+reproductionInteraction.expiredConsents()+"，孕期检查/出生/孕者死亡/出生失败="+reproduction.pregnanciesExamined()+"/"+reproduction.births()+"/"+reproduction.carrierDeaths()+"/"+reproduction.birthFailures()+"，平均/最大="+reproduction.averageMicros()+"/"+reproduction.maxMicros()+"μs，最近原因="+reproduction.lastReason()+"，孕期/同意请求/亲子/孤儿/生育冷却记录="+data.pregnancyCount()+"/"+data.reproductionConsentCount()+"/"+data.parentageCount()+"/"+data.orphanCount()+"/"+data.reproductionCooldownCount()+"，孤儿创建/失败/截断="+aging.orphansCreated()+"/"+aging.orphanFailures()+"/"+aging.orphanTruncations()
                +"；收养 轮次/孤儿/候选/合格/成功/随机失败/提交失败/缺位置/玩家同意延后="+adoption.runs()+"/"+adoption.orphansExamined()+"/"+adoption.candidatesExamined()+"/"+adoption.eligibleCandidates()+"/"+adoption.adoptions()+"/"+adoption.failedRolls()+"/"+adoption.commitFailures()+"/"+adoption.missingPosition()+"/"+adoption.playerConsentDeferred()+"，最近原因="+adoption.lastReason()+"，最近评分="+(adoption.lastEvaluation()==null?"无":adoption.lastEvaluation().score()+" 距离="+adoption.lastEvaluation().distanceBlocks()+" 分项="+adoption.lastEvaluation().components())
                +"；玩家家庭收养 待确认/申请/确认/完成/拒绝/过期/失败/旧修订="+data.adoptionConsentCount()+"/"+playerAdoption.requests()+"/"+playerAdoption.confirmations()+"/"+playerAdoption.completed()+"/"+playerAdoption.declined()+"/"+playerAdoption.expired()+"/"+playerAdoption.rejected()+"/"+playerAdoption.stale()+"，平均/最大="+playerAdoption.averageMicros()+"/"+playerAdoption.maxMicros()+"μs，最近原因="+playerAdoption.lastReason()+"，最近评分="+(playerAdoption.lastEvaluation()==null?"无":playerAdoption.lastEvaluation().score()+" 分项="+playerAdoption.lastEvaluation().components())
                +"；入侵永久死亡 尝试/成功/拒绝/亡偶失败/孤儿/孤儿失败/截断="+combatDeaths.attempts()+"/"+combatDeaths.deaths()+"/"+combatDeaths.rejected()+"/"+combatDeaths.partnerFailures()+"/"+combatDeaths.orphansCreated()+"/"+combatDeaths.orphanFailures()+"/"+combatDeaths.orphanTruncations()+"，最近原因="+combatDeaths.lastReason()+"，死亡原因计数="+deathDiagnostics.counts()+"，检查/截断="+deathDiagnostics.examined()+"/"+deathDiagnostics.truncated()
                +"；流民出现判定/生成/池复用/随机未命中/无已加载来源="+migration.rolls()+"/"+migration.appeared()+"/"+migration.reusedFromPool()+"/"+migration.rollFailures()+"/"+migration.noLoadedOrigin()+"，总家庭/区域池/处理/实体生成/抵达/过期/入池/位置写回/旧修订="+data.activeMigrationCount()+"/"+data.regionalPoolCount()+"/"+migration.groupsProcessed()+"/"+migration.spawned()+"/"+migration.arrived()+"/"+migration.expired()+"/"+migration.departed()+"/"+migration.positionUpdates()+"/"+migration.stale()+"，来源/普通居民位置记录="+data.migrationOriginCount()+"/"+data.locationCount()+"，平均/最大="+migration.averageMicros()+"/"+migration.maxMicros()+"μs，最近原因="+migration.lastReason()
                +"；人口schema="+PopulationSavedData.SCHEMA_VERSION+"，迁移="+data.migrationResult()+"，隔离 档案/居民/家庭/提议/婚姻冷却/孕期/生育请求/亲子/孤儿/生育冷却/流民/流民来源/位置="+data.quarantinedProfiles()+"/"+data.quarantinedCitizens()+"/"+data.quarantinedHouseholds()+"/"+data.quarantinedProposals()+"/"+data.quarantinedCooldowns()+"/"+data.quarantinedPregnancies()+"/"+data.quarantinedReproductionConsents()+"/"+data.quarantinedParentage()+"/"+data.quarantinedOrphans()+"/"+data.quarantinedReproductionCooldowns()+"/"+data.quarantinedMigrations()+"/"+data.quarantinedMigrationOrigins()+"/"+data.quarantinedLocations()+"，只读="+data.readOnly()+"；城市schema="+CitySavedData.SCHEMA_VERSION+"，迁移="+cityData.migrationResult()+"，隔离="+cityData.quarantinedCities()+"，只读="+cityData.readOnly());
    }

    public static int reproduction(ServerPlayer player){var manager=ReproductionManager.metrics();var resolver=com.seaquake6324.civitas.infrastructure.population.ReproductionConditionResolver.metrics();var weights=com.seaquake6324.civitas.infrastructure.population.ReproductionSettings.weights();var last=resolver.last();String evidence=last==null?"none":"conditions="+last.conditions()+", recentDeaths="+last.recentDeaths()+", alive/capacity="+last.aliveCitizens()+"/"+last.housingCapacity()+", evidenceChunk="+ChunkCoordinate.unpack(last.evidenceChunk())+", residence/security="+last.residenceLinked()+"/"+last.securityLinked()+", examined citizens/buildings="+last.citizensExamined()+"/"+last.buildingsExamined()+", truncated="+last.truncated();var population=PopulationSavedData.get(player.level().getServer());var active=population.pregnancies().stream().filter(p->p.evaluation()!=null).findFirst().orElse(null);String persisted=active==null?(population.pregnancyCount()==0?"none":"legacy evidence unavailable"):"pregnancy="+active.id()+", conditions="+active.evaluation().conditions()+", weights="+active.evaluation().weights()+", components="+active.evaluation().components()+", stage/consent="+active.evaluation().stageModifier()+"/"+active.evaluation().consentModifier()+", effective/interval="+active.evaluation().effectiveWillingness()+"/"+active.evaluation().attemptIntervalTicks()+", infidelity="+active.infidelity();return ok(player,"Reproduction diagnostics: weights="+weights+"; latest="+evidence+"; persisted="+persisted+"; resolutions/citizens/buildings/truncated="+resolver.resolutions()+"/"+resolver.citizensExamined()+"/"+resolver.buildingsExamined()+"/"+resolver.truncatedResolutions()+"; household examined/eligible="+manager.householdsExamined()+"/"+manager.eligibleHouseholds()+"; autonomous citizens/non-spouse pairs/pair-plan truncations="+manager.citizensExamined()+"/"+manager.nonSpousePairs()+"/"+manager.pairPlansTruncated()+"; conceptions/failures/roll failures="+manager.conceptions()+"/"+manager.conceptionFailures()+"/"+manager.failedRolls()+"; bounds conditionScan/citizenBatch/pairs="+CivitasConfig.REPRODUCTION_CONDITION_SCAN_LIMIT.get()+"/"+CivitasConfig.REPRODUCTION_CITIZEN_SCAN_BATCH.get()+"/"+CivitasConfig.REPRODUCTION_PAIRS_PER_RUN.get()+"; latest reason="+manager.lastReason());}

    public static int security(ServerPlayer player){City city=city(player);if(city==null)return fail(player,"没有可诊断的城池");ThreatSavedData data=ThreatSavedData.get(player.level().getServer());long chunk=ChunkPos.pack(player.blockPosition());var cell=data.securityCell(city.id(),chunk).orElse(null);var summary=SecurityCellManager.summary(player.level().getServer(),city.id());var metrics=SecurityCellManager.metrics();String local=cell==null?"当前区块尚未评估":"当前区块风险="+one(cell.diagnosticRisk())+"，主因="+cell.primaryFactor()+"，原始文礼/活动/安全/巡逻/可见守卫/渗透通道/地下黑暗="+one(cell.evidence().civility())+"/"+one(cell.evidence().activity())+"/"+one(cell.evidence().builtSafety())+"/"+one(cell.evidence().patrolCoverage())+"/"+one(cell.evidence().guardResponse())+"/"+one(cell.evidence().infiltrationAccess())+"/"+one(cell.evidence().undergroundDarkness())+"，距边境="+cell.evidence().borderDistance()+"，荒废="+cell.evidence().neglectStage()+"，近期防御="+cell.evidence().lastDefenseResult()+"，规模="+cell.evidence().territorySize()+"，有效建筑/连通入口="+cell.evidence().validBuildings()+"/"+cell.evidence().validatedEntrances()+"，建筑/巡逻/守卫/渗透/地下证据="+cell.evidence().buildingEvidenceLinked()+"/"+cell.evidence().patrolEvidenceLinked()+"/"+cell.evidence().guardEvidenceLinked()+"/"+cell.evidence().infiltrationEvidenceLinked()+"/"+cell.evidence().undergroundEvidenceLinked()+"，分项="+cell.contributions()+"，缺失="+cell.missingEvidence()+"，修订="+cell.revision();return ok(player,"治安诊断："+local+"；全城单元="+summary.assessedCells()+"，平均/最高="+one(summary.averageRisk())+"/"+one(summary.maxRisk())+"，最弱区块="+ChunkCoordinate.unpack(summary.weakestChunk())+"，主因="+summary.primaryFactor()+"，缺证据单元="+summary.cellsWithMissingEvidence()+"，摘要检查/截断="+summary.examined()+"/"+summary.truncated()+"，近期巡逻/可见守卫单元="+summary.recentlyPatrolledCells()+"/"+summary.visibleGuardCells()+"；队列="+metrics.queued()+"，评估/延后/失效="+metrics.assessed()+"/"+metrics.deferred()+"/"+metrics.missing()+"，发现轮次/截断="+metrics.discoveryCycles()+"/"+metrics.discoveryTruncated()+"("+metrics.truncatedDiscoveries()+")，清理检查/移除="+metrics.cleanupExamined()+"/"+metrics.cleanupRemoved()+"，建筑证据截断="+metrics.buildingEvidenceTruncations()+"，渗透世界样本/截断/未加载="+metrics.infiltrationSamples()+"/"+metrics.infiltrationTruncations()+"/"+metrics.infiltrationUnloaded()+"，平均/最大="+metrics.averageMicros()+"/"+metrics.maxMicros()+"μs；schema="+ThreatSavedData.SCHEMA_VERSION+"，迁移="+data.migrationResult()+"，隔离边缘/单元/路线/任命/覆盖/渗透计划="+data.quarantinedEdges()+"/"+data.quarantinedSecurityCells()+"/"+data.quarantinedPatrolRoutes()+"/"+data.quarantinedPatrolAssignments()+"/"+data.quarantinedPatrolCoverage()+"/"+data.quarantinedInfiltrationPlans()+"，只读="+data.readOnly()+"；治安风险已参与合法渗透地点、拦截、预警时间和波次规模");}

    public static int createCitizen(ServerPlayer player,String raceText,String genderText){City city=city(player);if(city==null)return fail(player,"当前没有可用于测试居民的城池");CitizenRace race;Gender gender;try{race=CitizenRace.valueOf(raceText.toUpperCase(java.util.Locale.ROOT));gender=Gender.valueOf(genderText.toUpperCase(java.util.Locale.ROOT));}catch(IllegalArgumentException exception){return fail(player,"种族使用 human/pigfolk/cowfolk/sheepfolk，性别使用 male/female");}ServerLevel level=(ServerLevel)player.level();var page=BuildingSavedData.get(level.getServer()).cityPage(city.id(),CivitasConfig.BUILDING_DASHBOARD_SCAN_LIMIT.get());if(page.truncated())return fail(player,"建筑记录超过诊断扫描上限，已安全停止创建测试居民");var residence=page.records().stream().filter(b->b.purpose()==BuildingPurpose.RESIDENCE&&b.status()==BuildingStatus.VALID&&b.dimension().equals(level.dimension().identifier().toString())).min(java.util.Comparator.comparingDouble(b->player.distanceToSqr(BlockPos.of(b.interior()).getCenter()))).orElse(null);if(residence==null)return fail(player,"先在当前维度登记一座有效住宅，再创建显现测试居民");PopulationSavedData data=PopulationSavedData.get(level.getServer());if(data.readOnly())return fail(player,"人口存档处于未来 schema 只读保护");UUID id=UUID.randomUUID();var name=CitizenNameGenerator.generate(id.getMostSignificantBits()^id.getLeastSignificantBits(),gender);var rules=PopulationAgingManager.rules();int appearance=race==CitizenRace.HUMAN?Math.floorMod(id.hashCode(),4):0;int lifespan=PopulationAgingManager.lifespanRules().years(id);var citizen=new CreateCitizenService().create(data,new CreateCitizenService.Request(id,name.given(),name.family(),race,race.name().toLowerCase(java.util.Locale.ROOT)+"_"+appearance,gender,rules.youngAdultAt()*rules.ticksPerYear(),level.getGameTime(),city.id(),lifespan)).withResidence(residence.id());data.putCitizen(citizen);return ok(player,"测试居民记录已创建："+citizen.displayName()+"；住宅="+residence.id()+"；接近住宅后将自动预热并显现");}

    public static int migration(ServerPlayer player){var data=PopulationSavedData.get(player.level().getServer());var m=MigrationManager.metrics();return ok(player,"迁入诊断：出现判定/生成/池复用/随机未命中/无来源="+m.rolls()+"/"+m.appeared()+"/"+m.reusedFromPool()+"/"+m.rollFailures()+"/"+m.noLoadedOrigin()+"；吸引力证据截断="+m.attractionEvidenceTruncations()+"，累计检查居民/家庭/建筑/治安="+m.attractionCitizensExamined()+"/"+m.attractionHouseholdsExamined()+"/"+m.attractionBuildingsExamined()+"/"+m.attractionSecurityExamined()+"，最近原值与证据="+m.lastAttraction()+"；来源检查区块/截断/地表列="+m.originChunksExamined()+"/"+m.originScanTruncations()+"/"+m.originColumnsExamined()+"；活动家庭/区域池/处理/实体/抵达/过期/入池/位置/旧修订="+data.activeMigrationCount()+"/"+data.regionalPoolCount()+"/"+m.groupsProcessed()+"/"+m.spawned()+"/"+m.arrived()+"/"+m.expired()+"/"+m.departed()+"/"+m.positionUpdates()+"/"+m.stale()+"；上限 快照/来源区块/活动家庭/单tick家庭/落点列/实体="+CivitasConfig.POPULATION_DASHBOARD_SCAN_LIMIT.get()+"/"+CivitasConfig.MIGRATION_EDGE_CHUNKS_SCAN_LIMIT.get()+"/"+CivitasConfig.MIGRATION_ACTIVE_GROUP_CAP.get()+"/"+CivitasConfig.MIGRATION_GROUPS_PER_TICK.get()+"/"+CivitasConfig.MIGRATION_SAFE_COLUMNS.get()+"/"+CivitasConfig.NPC_ENTITY_CAP.get()+"；平均/最大="+m.averageMicros()+"/"+m.maxMicros()+"μs，最近="+m.lastReason());}
    public static int outMigration(ServerPlayer player){var data=PopulationSavedData.get(player.level().getServer());var m=OutMigrationManager.metrics();return ok(player,"迁出诊断：0.2a 未启用，定居居民不会自主迁出；休眠轮次/家庭/独居/合格/抽签/启动="+m.runs()+"/"+m.householdsExamined()+"/"+m.singlesExamined()+"/"+m.eligible()+"/"+m.rolls()+"/"+m.started()+"；游标仅作 schema 兼容="+data.outMigrationHouseholdCursor()+"/"+data.outMigrationCitizenCursor()+"；最近原因="+m.lastReason()+"；最近计算="+m.lastEvaluation());}

    public static int patrol(ServerPlayer player){City city=city(player);if(city==null)return fail(player,"没有可诊断的城池");var server=player.level().getServer();ThreatSavedData data=ThreatSavedData.get(server);var routeMetrics=PatrolRouteManager.metrics();var appointment=GuardAssignmentManager.metrics();var execution=PatrolExecutionManager.metrics();var equipment=CitizenEquipmentSavedData.get(server);var equipmentMetrics=equipment.metrics();var drops=CitizenEquipmentDeathDropManager.metrics();var routes=data.patrolRoutes(city.id(),CivitasConfig.PATROL_ROUTE_CITY_CAP.get());var assignments=data.patrolAssignments(city.id(),CivitasConfig.PATROL_ASSIGNMENT_CITY_CAP.get());return ok(player,"巡逻诊断：路线="+routes.size()+"/"+CivitasConfig.PATROL_ROUTE_CITY_CAP.get()+"，节点="+routes.stream().map(r->r.id()+":"+r.nodes().size()+":"+r.status()+":rev"+r.revision()).toList()+"；任命="+assignments.size()+"/"+CivitasConfig.PATROL_ASSIGNMENT_CITY_CAP.get()+"，明细="+assignments.stream().map(a->a.citizenId()+":"+a.shift()+":"+a.status()+":强制"+a.forced()+":意愿"+one(a.willingness().score())+a.willingness().components()+":节点"+a.nodeIndex()+":rev"+a.revision()+":"+a.inactiveReason()).toList()+"；任命 查看/尝试/成功/拒绝/强制/实体检查="+appointment.views()+"/"+appointment.attempts()+"/"+appointment.assigned()+"/"+appointment.rejected()+"/"+appointment.forced()+"/"+appointment.entityChecks()+"，最近="+appointment.lastReason()+"；执行 tick/检查/活动/离班/无武器/虚拟推进/可见寻路/到点/失效/缺人="+execution.ticks()+"/"+execution.examined()+"/"+execution.active()+"/"+execution.offShift()+"/"+execution.unequipped()+"/"+execution.virtualAdvances()+"/"+execution.visibleDrives()+"/"+execution.nodesReached()+"/"+execution.staleRoutes()+"/"+execution.missingGuards()+"，平均/最大="+execution.averageMicros()+"/"+execution.maxMicros()+"μs；画线 会话/节点/拒绝/确认/过期="+routeMetrics.sessions()+"/"+routeMetrics.nodesAccepted()+"/"+routeMetrics.rejected()+"/"+routeMetrics.confirmed()+"/"+routeMetrics.expired()+"，样本/岗哨截断="+routeMetrics.pathSamples()+"/"+routeMetrics.postLookupTruncations()+"；装备schema="+CitizenEquipmentSavedData.SCHEMA_VERSION+"，迁移="+equipment.migrationResult()+"，隔离快照/物品="+equipment.quarantinedSnapshots()+"/"+equipment.quarantinedStacks()+"，快照/捕获/恢复/解码失败="+equipmentMetrics.snapshots()+"/"+equipmentMetrics.captures()+"/"+equipmentMetrics.restores()+"/"+equipmentMetrics.decodeFailures()+"，死亡掉落 检查/快照/物品/区块延后/缺位置/空解码="+drops.examined()+"/"+drops.droppedSnapshots()+"/"+drops.droppedStacks()+"/"+drops.deferredChunks()+"/"+drops.missingLocations()+"/"+drops.decodeEmpty()+"；威胁schema="+ThreatSavedData.SCHEMA_VERSION+"，迁移="+data.migrationResult()+"，隔离路线/任命/覆盖="+data.quarantinedPatrolRoutes()+"/"+data.quarantinedPatrolAssignments()+"/"+data.quarantinedPatrolCoverage()+"，覆盖单元="+data.patrolCoverageCount()+"，只读="+data.readOnly());}

    private static City city(ServerPlayer player) {
        CitySavedData data = CitySavedData.get(player.level().getServer());
        long chunk = ChunkPos.pack(player.blockPosition());
        return data.cityAt(player.level().dimension().identifier().toString(), chunk)
                .or(() -> data.cityForMember(player.getUUID())).orElse(null);
    }
    private static int ok(ServerPlayer player, String message) { player.sendSystemMessage(Component.literal(message), true); return 1; }
    private static int fail(ServerPlayer player, String message) { player.sendSystemMessage(Component.literal(message), true); return 0; }
    private static String one(double value) { return String.format(Locale.ROOT, "%.1f", value); }
    private AdminDebugTools() {}
}
