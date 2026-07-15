package com.seaquake6324.civitas.infrastructure.event;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;

import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.infrastructure.network.CityMapNetworkSync;
import com.seaquake6324.civitas.infrastructure.activity.ActivityManager;
import com.seaquake6324.civitas.domain.civilization.ActivityCategory;
import com.seaquake6324.civitas.infrastructure.network.OpenFoundingPayload;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.registry.CivitasBlocks;
import com.seaquake6324.civitas.infrastructure.world.CityCoreBlockEntity;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.seaquake6324.civitas.infrastructure.debug.RegionDebugManager;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.seaquake6324.civitas.infrastructure.civilization.CivilizationHudSync;
import com.seaquake6324.civitas.infrastructure.world.CoreMoveManager;
import com.seaquake6324.civitas.infrastructure.world.TerritoryExpansionManager;
import com.seaquake6324.civitas.infrastructure.network.OpenCityManagementPayload;
import com.seaquake6324.civitas.infrastructure.spawn.NaturalMobDespawn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import com.seaquake6324.civitas.infrastructure.civilization.CivilityScanScheduler;
import com.seaquake6324.civitas.infrastructure.border.BorderThreatManager;
import com.seaquake6324.civitas.infrastructure.border.InvasionMobIndex;
import com.seaquake6324.civitas.infrastructure.border.InvasionMobMarker;
import com.seaquake6324.civitas.infrastructure.territory.TerritoryLifecycleManager;
import com.seaquake6324.civitas.infrastructure.persistence.ThreatSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.BuildingSavedData;
import com.seaquake6324.civitas.infrastructure.world.BuildingRegistrationManager;
import com.seaquake6324.civitas.infrastructure.world.StorageAuthorizationManager;
import com.seaquake6324.civitas.infrastructure.world.PatrolRouteManager;
import com.seaquake6324.civitas.infrastructure.building.BuildingRevalidationManager;
import com.seaquake6324.civitas.infrastructure.population.PopulationAgingManager;
import com.seaquake6324.civitas.infrastructure.population.MarriageProposalMaintenanceManager;
import com.seaquake6324.civitas.infrastructure.population.MarriageInteractionManager;
import com.seaquake6324.civitas.infrastructure.population.NpcMarriageManager;
import com.seaquake6324.civitas.infrastructure.population.ReproductionManager;
import com.seaquake6324.civitas.infrastructure.population.ReproductionInteractionManager;
import com.seaquake6324.civitas.infrastructure.population.AdoptionManager;
import com.seaquake6324.civitas.infrastructure.population.PlayerAdoptionManager;
import com.seaquake6324.civitas.infrastructure.population.CitizenCombatDeathManager;
import com.seaquake6324.civitas.infrastructure.population.CitizenMaterializationManager;
import com.seaquake6324.civitas.infrastructure.population.MigrationManager;
import com.seaquake6324.civitas.infrastructure.entity.CitizenEntity;
import com.seaquake6324.civitas.infrastructure.security.SecurityCellManager;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import com.seaquake6324.civitas.infrastructure.network.OpenGenderSelectionPayload;
import com.seaquake6324.civitas.domain.territory.NeglectStage;
import com.seaquake6324.civitas.domain.building.BuildingCapacitySummary;
import com.seaquake6324.civitas.infrastructure.network.OpenPlayerMarriagePayload;
import com.seaquake6324.civitas.application.BuildPopulationDashboardService;
import com.seaquake6324.civitas.domain.population.*;

public final class CivitasGameEvents {
    private static final Set<Item> RITUAL_TOOLS = Set.of(Items.WOODEN_AXE, Items.WOODEN_PICKAXE, Items.WOODEN_SHOVEL, Items.WOODEN_HOE);
    private static final Map<UUID, Long> LAST_NOTICE = new HashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CivitasGameEvents::onActivityFacilityUse);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onEntityInteractSpecific);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onBreakBlock);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onPlaceBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CivitasGameEvents::onActivityBreakBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CivitasGameEvents::onActivityPlaceBlock);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onItemSmelted);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CivitasGameEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onEntityLeaveLevel);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onFluidPlace);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onFarmlandTrample);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onExplosion);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CivitasGameEvents::onMobDespawn);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(CivitasGameEvents::onServerStopping);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        CivilityScanScheduler.tick(event.getServer());
        ActivityManager.tick(event.getServer());
        BorderThreatManager.tick(event.getServer());
        com.seaquake6324.civitas.infrastructure.security.BackgroundBattleManager.tick(event.getServer());
        com.seaquake6324.civitas.infrastructure.security.VisibleInvasionCombatManager.tick(event.getServer());
        TerritoryLifecycleManager.tick(event.getServer());
        BuildingRevalidationManager.tick(event.getServer());
        PopulationAgingManager.tick(event.getServer());
        MarriageProposalMaintenanceManager.tick(event.getServer());
        NpcMarriageManager.tick(event.getServer());
        ReproductionManager.tick(event.getServer());
        AdoptionManager.tick(event.getServer());
        PlayerAdoptionManager.maintain(event.getServer());
        MigrationManager.tick(event.getServer());
        CitizenMaterializationManager.tick(event.getServer());
        com.seaquake6324.civitas.infrastructure.population.CitizenEquipmentDeathDropManager.tick(event.getServer());
        com.seaquake6324.civitas.infrastructure.security.PatrolExecutionManager.tick(event.getServer());
        SecurityCellManager.tick(event.getServer());
    }

    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {CityMapNetworkSync.sendSnapshot(player);PopulationSavedData population=PopulationSavedData.get(player.level().getServer());if(population.profile(player.getUUID()).isEmpty())PacketDistributor.sendToPlayer(player,new OpenGenderSelectionPayload());else if(population.activeProposalFor(FamilyMemberRef.player(player.getUUID())).isPresent())player.sendSystemMessage(Component.translatable("civitas.marriage.notice.login"),true);}
    }

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RegionDebugManager.remove(player);
            CivilizationHudSync.remove(player);
            CoreMoveManager.remove(player);
            TerritoryExpansionManager.remove(player);
            BuildingRegistrationManager.remove(player);
            StorageAuthorizationManager.remove(player);
            PatrolRouteManager.remove(player);
            ActivityManager.remove(player);
            LAST_NOTICE.remove(player.getUUID());
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RegionDebugManager.tick(player);
            CivilizationHudSync.tick(player);
            CoreMoveManager.tick(player);
            StorageAuthorizationManager.tick(player);
            ActivityManager.sampleMovement(player);
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) return;
        BlockPos pos = event.getPos();
        if(event.getHand()==InteractionHand.MAIN_HAND&&PatrolRouteManager.active(player)){event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);PatrolRouteManager.select(level,player,pos,event.getFace());return;}
        if (event.getHand() == InteractionHand.MAIN_HAND && StorageAuthorizationManager.active(player)) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
            StorageAuthorizationManager.select(level, player, pos); return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND && BuildingRegistrationManager.active(player)) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
            if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof CityCoreBlockEntity core
                    && BuildingRegistrationManager.cancelAtCore(player, core)) return;
            BuildingRegistrationManager.select(level, player, pos, event.getFace());
            return;
        }
        if(event.getHand()==InteractionHand.MAIN_HAND&&TerritoryExpansionManager.active(player)){
            event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
            if(player.isShiftKeyDown()&&level.getBlockEntity(pos) instanceof CityCoreBlockEntity core
                    &&TerritoryExpansionManager.cancelAtCore(player,core))return;
            TerritoryExpansionManager.confirm(level,player,ChunkPos.pack(pos));return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND && CoreMoveManager.isMoving(player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof CityCoreBlockEntity core && core.isActivated()) {
                CoreMoveManager.cancel(player);
            } else {
                CoreMoveManager.submit(level, player, pos.relative(event.getFace()));
            }
            return;
        }
        if (level.getBlockEntity(pos) instanceof CityCoreBlockEntity core) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event.getHand() != InteractionHand.MAIN_HAND) return;
            if (core.isActivated()) {
                if (player.isShiftKeyDown()) CoreMoveManager.begin(level, player, pos, core);
                else openCityManagement(level, player, pos, core);
            } else if (!player.getUUID().equals(core.placerId())) {
                actionbar(player, Component.translatable("civitas.error.not_core_owner"));
            } else {
                PacketDistributor.sendToPlayer(player, new OpenFoundingPayload(pos));
            }
            return;
        }
        if (isProtectedFrom(level, pos, player)) {
            deny(event, player);
            return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND && tryRitual(level, player, pos)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        // Opening or using a container does not change structural evidence. Actual block
        // mutations are invalidated by the place, break, fluid, and trample listeners.
    }

    private static void onServerStopping(ServerStoppingEvent event) { PopulationAgingManager.shutdown(event.getServer()); StorageAuthorizationManager.clear(); }

    private static void onActivityFacilityUse(PlayerInteractEvent.RightClickBlock event) {
        if (!event.isCanceled() && event.getHand() == InteractionHand.MAIN_HAND
                && event.getEntity() instanceof ServerPlayer player) {
            ActivityManager.recordFacilityUse(player, event.getPos());
        }
    }

    private static void openCityManagement(ServerLevel level, ServerPlayer player, BlockPos pos, CityCoreBlockEntity core) {
        City city = core.cityId() == null ? null : CitySavedData.get(level.getServer()).byId(core.cityId()).orElse(null);
        if (city == null) {
            actionbar(player, Component.translatable("civitas.core_manage.stale_core"));
        } else {
            var threat=BorderThreatManager.snapshot(level.getServer(),city.id());
            float pressure=threat==null?0:(float)threat.state().pressure();String threatDirection=threat==null?"NONE":threat.direction().name();String threatPhase=threat==null?"CALM":threat.state().phase().name();long threatRemaining=threat==null?0:switch(threat.state().phase()){case WARNING->Math.max(0,CivitasConfig.INVASION_WARNING_TICKS.get()-(level.getGameTime()-threat.state().phaseStartedAt()));case COOLDOWN->Math.max(0,threat.state().cooldownUntil()-level.getGameTime());default->0;};
            int warning=(int)city.territoryStates().values().stream().filter(s->s.neglectStage()==NeglectStage.WARNING).count();int abandoned=(int)city.territoryStates().values().stream().filter(s->s.neglectStage()==NeglectStage.ABANDONED).count();int retractable=(int)city.territoryStates().values().stream().filter(s->s.neglectStage()==NeglectStage.RETRACTABLE).count();long now=level.getGameTime();long recovery=city.territoryStates().values().stream().filter(s->s.recoveryStartedAt()>0).mapToLong(s->Math.max(0,CivitasConfig.NEGLECT_RECOVERY_TICKS.get()-(now-s.recoveryStartedAt()))).min().orElse(0);
            int buildingScanLimit=CivitasConfig.BUILDING_DASHBOARD_SCAN_LIMIT.get();
            var buildingPage=BuildingSavedData.get(level.getServer()).cityPage(city.id(),buildingScanLimit);
            var buildingRecords=buildingPage.records();
            boolean buildingsTruncated=buildingPage.truncated();
            BuildingCapacitySummary buildings=BuildingCapacitySummary.from(buildingRecords);
            var security=SecurityCellManager.summary(level.getServer(),city.id());
            var threatData=ThreatSavedData.get(level.getServer());var assignments=threatData.patrolAssignments(city.id(),CivitasConfig.PATROL_ASSIGNMENT_CITY_CAP.get());int activeGuards=(int)assignments.stream().filter(a->a.status()==com.seaquake6324.civitas.domain.security.PatrolAssignment.Status.ACTIVE).count();
            var population=populationView(level,city);
            PacketDistributor.sendToPlayer(player, new OpenCityManagementPayload(pos, city.name(), city.color(),
                    city.isMember(player.getUUID()),city.mayManage(player.getUUID()),
                    city.founderId().equals(player.getUUID()),city.lordId().equals(player.getUUID()),
                    city.residents().stream().map(id->playerEntry(level,id)).toList(),
                    city.applications().keySet().stream().map(id->playerEntry(level,id)).toList(),
                    city.territory().size(),city.heartland().size(),city.lastExpansionAt(),Math.max(0,city.lastExpansionAt()+CivitasConfig.EXPANSION_COOLDOWN_TICKS.get()-now),pressure,threatDirection,threatPhase,threatRemaining,warning,abandoned,retractable,recovery,
                    buildings.validBuildings(),buildings.staleBuildings(),buildings.invalidBuildings(),buildings.housingCapacity(),buildings.guardCapacity(),
                    security.assessedCells(),(float)security.averageRisk(),(float)security.maxRisk(),security.weakestChunk(),security.primaryFactor(),security.cellsWithMissingEvidence(),security.truncated(),security.examined(),security.updatedAt(),threatData.patrolRoutes(city.id(),CivitasConfig.PATROL_ROUTE_CITY_CAP.get()).size(),assignments.size(),activeGuards,security.recentlyPatrolledCells(),security.visibleGuardCells(),
                    buildingRecords.stream().sorted(java.util.Comparator.comparingLong(com.seaquake6324.civitas.domain.building.BuildingRecord::validatedAt).reversed()).limit(CivitasConfig.BUILDING_DASHBOARD_RECORD_LIMIT.get()).map(CivitasGameEvents::buildingView).toList(),
                    buildingsTruncated||buildingRecords.size()>CivitasConfig.BUILDING_DASHBOARD_RECORD_LIMIT.get(),migrationViews(level,city,now),population,orphanViews(level,city),adoptionViews(level,city,player.getUUID(),now)));
        }
    }

    private static OpenCityManagementPayload.PopulationView populationView(ServerLevel level,City city){int limit=CivitasConfig.POPULATION_DASHBOARD_SCAN_LIMIT.get();PopulationSavedData population=PopulationSavedData.get(level.getServer());BuildingSavedData buildings=BuildingSavedData.get(level.getServer());var citizenPage=population.citizenPage(city.id(),limit);var householdPage=population.householdPage(city.id(),limit);var citizens=citizenPage.records();var households=householdPage.records();var page=buildings.cityPage(city.id(),limit);var records=page.records();boolean truncated=citizenPage.truncated()||householdPage.truncated()||page.truncated();var dashboard=new BuildPopulationDashboardService().build(city.id(),citizens,households,records,PopulationAgingManager.rules(),truncated);var c=dashboard.composition();return new OpenCityManagementPayload.PopulationView(c.total(),count(c.genders(),Gender.MALE),count(c.genders(),Gender.FEMALE),count(c.ages(),AgeStage.CHILD),count(c.ages(),AgeStage.ADOLESCENT),count(c.ages(),AgeStage.YOUNG_ADULT),count(c.ages(),AgeStage.MATURE_ADULT),count(c.ages(),AgeStage.ELDER),count(c.races(),CitizenRace.HUMAN),count(c.races(),CitizenRace.PIGFOLK),count(c.races(),CitizenRace.COWFOLK),count(c.races(),CitizenRace.SHEEPFOLK),count(c.runtimeStates(),CitizenRuntimeState.VIRTUAL),count(c.runtimeStates(),CitizenRuntimeState.PREWARMING),count(c.runtimeStates(),CitizenRuntimeState.MATERIALIZED),count(c.runtimeStates(),CitizenRuntimeState.LOCKED),dashboard.housingCapacity(),dashboard.housedCitizens(),dashboard.unassignedCitizens(),dashboard.invalidResidenceReferences(),dashboard.employedCitizens(),dashboard.householdCount(),dashboard.partneredHouseholds(),dashboard.householdsWithChildren(),dashboard.permanentDeaths(),dashboard.latestDeathAtTick(),(float)dashboard.averageFoodCoverage(),(float)dashboard.averageHousingCoverage(),(float)dashboard.averageSettlementWillingness(),(float)dashboard.averageMigrationWillingness(),dashboard.limitations().stream().map(Enum::name).sorted().toList(),dashboard.truncated());}
    private static<E>int count(Map<E,Integer>values,E key){return values.getOrDefault(key,0);}
    private static OpenCityManagementPayload.BuildingView buildingView(com.seaquake6324.civitas.domain.building.BuildingRecord record){var requirement=com.seaquake6324.civitas.domain.building.BuildingRequirement.forPurpose(record.purpose());var features=record.features();return new OpenCityManagementPayload.BuildingView(record.id(),record.purpose().name(),record.status().name(),record.capacity(),record.cells().size(),requirement.facility().name(),record.facilities().getOrDefault(requirement.facility(),0),features.boundaryPorts().size(),features.workstations().size(),features.storageEndpoints().size(),record.authorizedStorageEndpoints().size(),features.entranceConnected(),record.revision(),record.invalidReason());}
    private static java.util.List<OpenCityManagementPayload.MigrationView>migrationViews(ServerLevel level,City city,long now){PopulationSavedData data=PopulationSavedData.get(level.getServer());return data.migrationsForCity(city.id(),Math.min(64,CivitasConfig.MIGRATION_ACTIVE_GROUP_CAP.get())).stream().filter(g->g.state()==MigrationGroupRecord.State.APPLIED).map(g->{String names=g.members().stream().map(id->data.citizen(id).map(CitizenRecord::displayName).orElse("?")).sorted().collect(java.util.stream.Collectors.joining(", "));return new OpenCityManagementPayload.MigrationView(g.id(),names,g.members().size(),g.children().size(),(float)g.attractionScore(),Math.max(0,g.decisionDeadline()-now),g.revision());}).toList();}
    private static java.util.List<OpenCityManagementPayload.OrphanView>orphanViews(ServerLevel level,City city){PopulationSavedData data=PopulationSavedData.get(level.getServer());return data.orphanPage(city.id(),CivitasConfig.POPULATION_DASHBOARD_SCAN_LIMIT.get()).records().stream().filter(o->data.adoptionConsentForChild(o.childId()).isEmpty()).sorted(java.util.Comparator.comparing(OrphanRecord::admittedAt)).limit(16).map(o->new OpenCityManagementPayload.OrphanView(o.childId(),data.citizen(o.childId()).map(CitizenRecord::displayName).orElse("?"),o.admittedAt(),o.revision())).toList();}
    private static java.util.List<OpenCityManagementPayload.AdoptionView>adoptionViews(ServerLevel level,City city,UUID player,long now){PopulationSavedData data=PopulationSavedData.get(level.getServer());return data.adoptionConsentsForPlayer(player,16).stream().filter(c->c.cityId().equals(city.id())).map(c->new OpenCityManagementPayload.AdoptionView(c.id(),c.childId(),data.citizen(c.childId()).map(CitizenRecord::displayName).orElse("?"),c.requiredPlayers().size(),c.confirmedPlayers().size(),c.confirmedPlayers().contains(player),Math.max(0,c.expiresAt()-now),c.revision())).toList();}

    private static OpenCityManagementPayload.PlayerEntry playerEntry(ServerLevel level,UUID id){
        ServerPlayer online=level.getServer().getPlayerList().getPlayer(id);
        String name=online!=null?online.getGameProfile().name():level.getServer().services().nameToIdCache()
                .get(id).map(profile->profile.name()).orElse("未知玩家");
        return new OpenCityManagementPayload.PlayerEntry(id,name);
    }

    private static boolean tryRitual(ServerLevel level, ServerPlayer player, BlockPos pos) {
        ItemStack food = player.getMainHandItem();
        if (!level.getBlockState(pos).is(Blocks.COBBLESTONE) || !player.isShiftKeyDown() || !food.has(DataComponents.FOOD)) return false;
        if (!player.getOffhandItem().isEmpty()) {
            actionbar(player, Component.translatable("civitas.ritual.offhand_not_empty"));
            return true;
        }
        for (int i = 1; i <= 3; i++) {
            if (!level.isEmptyBlock(pos.above(i))) {
                actionbar(player, Component.translatable("civitas.ritual.space_blocked"));
                return true;
            }
        }
        Map<Item, ItemEntity> found = new HashMap<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = pos.relative(direction);
            List<ItemEntity> inCell = level.getEntitiesOfClass(ItemEntity.class, new AABB(adjacent),
                    entity -> RITUAL_TOOLS.contains(entity.getItem().getItem()));
            if (inCell.isEmpty()) {
                actionbar(player, Component.translatable("civitas.ritual.tools_missing"));
                return true;
            }
            ItemEntity entity = inCell.getFirst();
            if (found.put(entity.getItem().getItem(), entity) != null) {
                actionbar(player, Component.translatable("civitas.ritual.tools_missing"));
                return true;
            }
        }
        if (!found.keySet().equals(RITUAL_TOOLS)) {
            actionbar(player, Component.translatable("civitas.ritual.tools_missing"));
            return true;
        }

        found.values().forEach(Entity::discard);
        food.shrink(1); // Ritual offerings are consumed in creative mode as well.
        level.setBlock(pos, CivitasBlocks.CITY_CORE.get().defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof CityCoreBlockEntity core) core.setPlacer(player.getUUID());
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setVisualOnly(true);
            bolt.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
            level.addFreshEntity(bolt);
        }
        actionbar(player, Component.translatable("civitas.ritual.core_formed"));
        return true;
    }

    private static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockPos pos = event.getPos();
        if (level.getBlockEntity(pos) instanceof CityCoreBlockEntity core) {
            boolean ownerMayRecover = !core.isActivated() && player.getUUID().equals(core.placerId());
            if (ownerMayRecover) {
                Block.popResource(level, pos, new ItemStack(CivitasBlocks.CITY_CORE_ITEM.get()));
            } else {
                event.setCanceled(true);
                event.setNotifyClient(true);
                actionbar(player, Component.translatable(core.isActivated() ? "civitas.error.activated_core_locked" : "civitas.error.not_core_owner"));
            }
            return;
        }
        if (isProtectedFrom(level, pos, player)) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            protectedNotice(player);
        }
    }

    private static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (isProtectedFrom(level, event.getPos(), player)) {
            event.setCanceled(true);
            protectedNotice(player);
        } else {
            BuildingRevalidationManager.invalidate(level, event.getPos());
        }
    }

    private static void onActivityBreakBlock(BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel)
                || !(event.getPlayer() instanceof ServerPlayer player)) return;
        ActivityCategory category = event.getState().getBlock() instanceof CropBlock crop && crop.isMaxAge(event.getState())
                ? ActivityCategory.LIVELIHOOD : ActivityCategory.CONSTRUCTION;
        ActivityManager.record(player, event.getPos(), category);
    }

    private static void onActivityPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            ActivityManager.rejectNoPlayerSource();
            return;
        }
        ActivityCategory category = event.getPlacedBlock().getBlock() instanceof CropBlock
                ? ActivityCategory.LIVELIHOOD : ActivityCategory.CONSTRUCTION;
        ActivityManager.record(player, event.getPos(), category);
    }

    private static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getCrafting().isEmpty())
            ActivityManager.record(player, player.blockPosition(), ActivityCategory.PRODUCTION);
    }

    private static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmountRemoved() > 0)
            ActivityManager.record(player, player.blockPosition(), ActivityCategory.PRODUCTION);
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if(event.getEntity() instanceof CitizenEntity citizen&&citizen.level() instanceof ServerLevel citizenLevel){
            if(event.getSource().getEntity() instanceof net.minecraft.world.entity.Mob attacker&&InvasionMobMarker.active(attacker)&&InvasionMobMarker.invasionId(attacker).filter(id->ThreatSavedData.get(citizenLevel.getServer()).activeInvasion(id)&&CitizenCombatDeathManager.kill(citizen,citizenLevel,id)).isPresent())return;
            event.setCanceled(true);citizen.setHealth(Math.max(.1F,citizen.getMaxHealth()*.01F));citizen.persistHealth(1);citizen.lockFor(citizenLevel.getGameTime(),200);return;
        }
        if(!event.isCanceled()&&event.getEntity() instanceof net.minecraft.world.entity.Mob mob&&InvasionMobMarker.active(mob)){InvasionMobMarker.invasionId(mob).ifPresent(id->ThreatSavedData.get(mob.level().getServer()).recordInvasionMobDeath(id,mob.getUUID(),mob.level().getGameTime()));InvasionMobIndex.unregister(mob);}
        if (event.isCanceled() || !(event.getEntity() instanceof Enemy)) return;
        if (event.getSource().getEntity() instanceof ServerPlayer player)
            ActivityManager.record(player, event.getEntity().blockPosition(), ActivityCategory.DEFENSE);
    }

    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (isProtectedFrom(level, event.getPos(), player)) {
            event.setCanceled(true);
            protectedNotice(player);
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) return;
        Item item = event.getItemStack().getItem();
        if (!(item instanceof BoatItem || item instanceof BucketItem || item instanceof SpawnEggItem)) return;
        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.BLOCK
                && (isProtectedFrom(level, hit.getBlockPos(), player)
                || isProtectedFrom(level, hit.getBlockPos().relative(hit.getDirection()), player))) {
            deny(event, player);
        }
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) return;
        if(event.getHand()==InteractionHand.MAIN_HAND&&player.isShiftKeyDown()&&event.getTarget() instanceof ServerPlayer target){var view=MarriageInteractionManager.playerView(player,target);var reproduction=ReproductionInteractionManager.playerView(player,target);PacketDistributor.sendToPlayer(player,new OpenPlayerMarriagePayload(target.getUUID(),target.getGameProfile().name(),view.action(),view.proposalId(),view.proposalRevision(),reproduction.action(),reproduction.consentId(),reproduction.consentRevision()));event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(event.getTarget() instanceof CitizenEntity citizen)citizen.lockFor(level.getGameTime(),100);
        if(isProtectedFrom(level,event.getTarget().blockPosition(),player)){deny(event,player);return;}
        if(event.getHand()==InteractionHand.MAIN_HAND&&event.getTarget() instanceof CitizenEntity citizen&&com.seaquake6324.civitas.infrastructure.security.GuardEquipmentManager.tryEquip(player,citizen,event.getHand())){event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(event.getHand()==InteractionHand.MAIN_HAND&&event.getTarget() instanceof CitizenEntity entity&&com.seaquake6324.civitas.infrastructure.population.CitizenInfoSync.send(player,entity)){event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);}
    }

    private static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) return;
        if(event.getTarget() instanceof CitizenEntity citizen)citizen.lockFor(level.getGameTime(),100);
        if (isProtectedFrom(level, event.getTarget().blockPosition(), player)) deny(event, player);
    }

    private static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) return;
        NaturalMobDespawn.recordCombat(event.getTarget());
        if(event.getTarget() instanceof CitizenEntity citizen)citizen.lockFor(level.getGameTime(),200);
        if (!(event.getTarget() instanceof Enemy) && isProtectedFrom(level, event.getTarget().blockPosition(), player)) {
            event.setCanceled(true);
            protectedNotice(player);
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if(event.getEntity() instanceof CitizenEntity citizen&&event.getEntity().level() instanceof ServerLevel citizenLevel)citizen.lockFor(citizenLevel.getGameTime(),200);
        if (sourceEntity instanceof Player) NaturalMobDespawn.recordCombat(event.getEntity());
        if (event.getEntity() instanceof Player && sourceEntity != null) NaturalMobDespawn.recordCombat(sourceEntity);
        if (!(event.getEntity().level() instanceof ServerLevel level) || event.getEntity() instanceof Enemy) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && isProtectedFrom(level, event.getEntity().blockPosition(), attacker)) {
            event.setCanceled(true);
            protectedNotice(attacker);
        }
    }

    private static void onMobDespawn(MobDespawnEvent event) {
        NaturalMobDespawn.onDespawnCheck(event);
    }

    private static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ActivityManager.rejectNoPlayerSource();
        if (cityAt(level, event.getPos()).isPresent()) event.setCanceled(true);
    }

    private static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Player player) || isProtectedFrom(level, event.getPos(), player)) event.setCanceled(true);
        else BuildingRevalidationManager.invalidate(level, event.getPos());
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Player owner = event.getExplosion().getIndirectSourceEntity() instanceof Player player ? player : null;
        event.getAffectedBlocks().removeIf(pos -> level.getBlockState(pos).is(CivitasBlocks.CITY_CORE.get()) ||
                cityAt(level, pos).filter(city -> owner == null || !city.isMember(owner.getUUID())).isPresent());
        event.getAffectedBlocks().forEach(pos -> BuildingRevalidationManager.invalidate(level, pos));
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event){if(!event.getLevel().isClientSide()&&event.getEntity() instanceof net.minecraft.world.entity.Mob mob&&InvasionMobMarker.active(mob)&&event.getLevel() instanceof ServerLevel level)InvasionMobMarker.invasionId(mob).ifPresent(id->{var admission=ThreatSavedData.get(level.getServer()).mobAdmission(id,mob.getUUID());switch(admission){case LEGACY_ACTIVE,SURVIVOR->InvasionMobIndex.register(mob);case DEFEATED->{InvasionMobIndex.unregister(mob);mob.discard();}case INACTIVE->InvasionMobMarker.release(mob);}});}
    private static void onEntityLeaveLevel(EntityLeaveLevelEvent event){if(!event.getLevel().isClientSide()&&event.getEntity() instanceof net.minecraft.world.entity.Mob mob)InvasionMobIndex.unregister(mob);}

    private static boolean isProtectedFrom(ServerLevel level, BlockPos pos, Player player) {
        return cityAt(level, pos).filter(city -> !city.isMember(player.getUUID())).isPresent();
    }

    private static Optional<City> cityAt(ServerLevel level, BlockPos pos) {
        return CitySavedData.get(level.getServer()).cityAt(level.dimension().identifier().toString(), ChunkPos.pack(pos));
    }

    private static void deny(PlayerInteractEvent event, ServerPlayer player) {
        if (event instanceof net.neoforged.bus.api.ICancellableEvent cancellable) cancellable.setCanceled(true);
        if (event instanceof PlayerInteractEvent.RightClickBlock click) click.setCancellationResult(InteractionResult.FAIL);
        if (event instanceof PlayerInteractEvent.RightClickItem click) click.setCancellationResult(InteractionResult.FAIL);
        if (event instanceof PlayerInteractEvent.EntityInteract click) click.setCancellationResult(InteractionResult.FAIL);
        if (event instanceof PlayerInteractEvent.EntityInteractSpecific click) click.setCancellationResult(InteractionResult.FAIL);
        protectedNotice(player);
    }

    private static void protectedNotice(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now - LAST_NOTICE.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2) >= 10) {
            LAST_NOTICE.put(player.getUUID(), now);
            actionbar(player, Component.translatable("civitas.protection.outsider_denied"));
        }
    }

    private static void actionbar(ServerPlayer player, Component message) { player.sendSystemMessage(message, true); }
    private CivitasGameEvents() {}
}
