package com.seaquake6324.civitas.infrastructure.network;

import java.util.UUID;

import com.seaquake6324.civitas.application.FoundCityService;
import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.world.CityCoreBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.seaquake6324.civitas.infrastructure.debug.RegionDebugManager;
import net.minecraft.server.permissions.Permissions;
import com.seaquake6324.civitas.application.EditCityIdentityService;
import com.seaquake6324.civitas.application.MembershipService;
import com.seaquake6324.civitas.infrastructure.world.TerritoryExpansionManager;
import com.seaquake6324.civitas.infrastructure.world.BuildingRegistrationManager;
import com.seaquake6324.civitas.infrastructure.world.StorageAuthorizationManager;
import com.seaquake6324.civitas.application.SelectPlayerGenderService;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import com.seaquake6324.civitas.infrastructure.population.MarriageInteractionManager;
import com.seaquake6324.civitas.infrastructure.population.ReproductionInteractionManager;
import com.seaquake6324.civitas.infrastructure.population.PlayerAdoptionManager;
import com.seaquake6324.civitas.infrastructure.population.CitizenInfoSync;
import com.seaquake6324.civitas.application.DecideMigrationService;
import com.seaquake6324.civitas.infrastructure.persistence.BuildingSavedData;
import com.seaquake6324.civitas.domain.building.BuildingPurpose;
import com.seaquake6324.civitas.domain.building.BuildingStatus;
import com.seaquake6324.civitas.infrastructure.world.PatrolRouteManager;

public final class CivitasNetwork {
    private static final FoundCityService FOUND_CITY = new FoundCityService();
    private static final EditCityIdentityService EDIT_CITY = new EditCityIdentityService();
    private static final MembershipService MEMBERSHIP = new MembershipService();
    private static final SelectPlayerGenderService SELECT_GENDER = new SelectPlayerGenderService();
    private static final DecideMigrationService DECIDE_MIGRATION=new DecideMigrationService();

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("19");
        registrar.playToClient(OpenFoundingPayload.TYPE, OpenFoundingPayload.STREAM_CODEC);
        registrar.playToClient(FoundingResultPayload.TYPE, FoundingResultPayload.STREAM_CODEC);
        registrar.playToClient(FoundingAnimationPayload.TYPE, FoundingAnimationPayload.STREAM_CODEC);
        registrar.playToClient(CityAnnouncementPayload.TYPE, CityAnnouncementPayload.STREAM_CODEC);
        registrar.playToClient(CityMapSnapshotPayload.TYPE, CityMapSnapshotPayload.STREAM_CODEC);
        registrar.playToClient(CityMapUpsertPayload.TYPE, CityMapUpsertPayload.STREAM_CODEC);
        registrar.playToClient(CityMapRemovePayload.TYPE, CityMapRemovePayload.STREAM_CODEC);
        registrar.playToClient(RegionDebugPayload.TYPE, RegionDebugPayload.STREAM_CODEC);
        registrar.playToClient(TerritoryDebugPayload.TYPE,TerritoryDebugPayload.STREAM_CODEC);
        registrar.playToClient(SystemDebugPayload.TYPE,SystemDebugPayload.STREAM_CODEC);
        registrar.playToClient(CivilizationHudPayload.TYPE, CivilizationHudPayload.STREAM_CODEC);
        registrar.playToClient(CoreMoveModePayload.TYPE, CoreMoveModePayload.STREAM_CODEC);
        registrar.playToClient(ExpansionModePayload.TYPE, ExpansionModePayload.STREAM_CODEC);
        registrar.playToClient(ExpansionResultPayload.TYPE, ExpansionResultPayload.STREAM_CODEC);
        registrar.playToClient(OpenCityManagementPayload.TYPE, OpenCityManagementPayload.STREAM_CODEC);
        registrar.playToClient(CityManagementResultPayload.TYPE, CityManagementResultPayload.STREAM_CODEC);
        registrar.playToClient(BuildingRegistrationModePayload.TYPE, BuildingRegistrationModePayload.STREAM_CODEC);
        registrar.playToClient(PatrolRouteModePayload.TYPE,PatrolRouteModePayload.STREAM_CODEC);
        registrar.playToClient(OpenGenderSelectionPayload.TYPE, OpenGenderSelectionPayload.STREAM_CODEC);
        registrar.playToClient(GenderSelectionResultPayload.TYPE, GenderSelectionResultPayload.STREAM_CODEC);
        registrar.playToClient(OpenCitizenInfoPayload.TYPE, OpenCitizenInfoPayload.STREAM_CODEC);
        registrar.playToClient(MarriageActionResultPayload.TYPE,MarriageActionResultPayload.STREAM_CODEC);
        registrar.playToClient(OpenPlayerMarriagePayload.TYPE,OpenPlayerMarriagePayload.STREAM_CODEC);
        registrar.playToClient(ReproductionActionResultPayload.TYPE,ReproductionActionResultPayload.STREAM_CODEC);
        registrar.playToClient(GuardAssignmentResultPayload.TYPE,GuardAssignmentResultPayload.STREAM_CODEC);
        registrar.playToServer(SubmitFoundingPayload.TYPE, SubmitFoundingPayload.STREAM_CODEC, CivitasNetwork::handleSubmit);
        registrar.playToServer(ToggleRegionDebugPayload.TYPE, ToggleRegionDebugPayload.STREAM_CODEC, CivitasNetwork::handleDebugToggle);
        registrar.playToServer(SubmitCityManagementPayload.TYPE, SubmitCityManagementPayload.STREAM_CODEC, CivitasNetwork::handleCityManagement);
        registrar.playToServer(CityMembershipActionPayload.TYPE, CityMembershipActionPayload.STREAM_CODEC, CivitasNetwork::handleMembership);
        registrar.playToServer(BeginBuildingRegistrationPayload.TYPE, BeginBuildingRegistrationPayload.STREAM_CODEC, CivitasNetwork::handleBuildingRegistration);
        registrar.playToServer(BeginPatrolRoutePayload.TYPE,BeginPatrolRoutePayload.STREAM_CODEC,CivitasNetwork::handlePatrolRoute);
        registrar.playToServer(BeginStorageAuthorizationPayload.TYPE, BeginStorageAuthorizationPayload.STREAM_CODEC, CivitasNetwork::handleStorageAuthorization);
        registrar.playToServer(SubmitGenderSelectionPayload.TYPE, SubmitGenderSelectionPayload.STREAM_CODEC, CivitasNetwork::handleGenderSelection);
        registrar.playToServer(SubmitNpcMarriageProposalPayload.TYPE,SubmitNpcMarriageProposalPayload.STREAM_CODEC,CivitasNetwork::handleNpcMarriageProposal);
        registrar.playToServer(SubmitNpcReproductionPayload.TYPE,SubmitNpcReproductionPayload.STREAM_CODEC,CivitasNetwork::handleNpcReproduction);
        registrar.playToServer(SubmitPlayerReproductionPayload.TYPE,SubmitPlayerReproductionPayload.STREAM_CODEC,CivitasNetwork::handlePlayerReproduction);
        registrar.playToServer(SubmitPlayerMarriageActionPayload.TYPE,SubmitPlayerMarriageActionPayload.STREAM_CODEC,CivitasNetwork::handlePlayerMarriageAction);
        registrar.playToServer(MigrationDecisionPayload.TYPE,MigrationDecisionPayload.STREAM_CODEC,CivitasNetwork::handleMigrationDecision);
        registrar.playToServer(AdoptionActionPayload.TYPE,AdoptionActionPayload.STREAM_CODEC,CivitasNetwork::handleAdoptionAction);
        registrar.playToServer(SubmitGuardAssignmentPayload.TYPE,SubmitGuardAssignmentPayload.STREAM_CODEC,CivitasNetwork::handleGuardAssignment);
        registrar.playToServer(RequestCitizenInfoPayload.TYPE,RequestCitizenInfoPayload.STREAM_CODEC,CivitasNetwork::handleCitizenInfoRefresh);
    }

    private static void handleCitizenInfoRefresh(RequestCitizenInfoPayload payload,IPayloadContext context){if(context.player() instanceof ServerPlayer player)CitizenInfoSync.refresh(player,payload.citizenId());}

    private static void handleGuardAssignment(SubmitGuardAssignmentPayload payload,IPayloadContext context){if(!(context.player() instanceof ServerPlayer player))return;var result=com.seaquake6324.civitas.infrastructure.security.GuardAssignmentManager.assign(player,payload.citizenId(),payload.expectedCitizenRevision(),payload.routeId(),payload.expectedRouteRevision(),payload.shift(),payload.force());PacketDistributor.sendToPlayer(player,new GuardAssignmentResultPayload(result.success(),"civitas.guard."+result.reason()));}

    private static void handleAdoptionAction(AdoptionActionPayload payload,IPayloadContext context){if(!(context.player() instanceof ServerPlayer player)||!(player.level() instanceof ServerLevel level))return;CityCoreBlockEntity core=level.getBlockEntity(payload.corePos())instanceof CityCoreBlockEntity found?found:null;City city=core==null||core.cityId()==null?null:CitySavedData.get(level.getServer()).byId(core.cityId()).orElse(null);if(core==null||!core.isActivated()||city==null||player.distanceToSqr(payload.corePos().getCenter())>64){PacketDistributor.sendToPlayer(player,new CityManagementResultPayload(false,"civitas.adoption.stale_core"));return;}PlayerAdoptionManager.Result result=payload.action()==AdoptionActionPayload.Action.REQUEST?PlayerAdoptionManager.request(player,city,payload.targetId(),payload.expectedRevision()):PlayerAdoptionManager.act(player,city,payload.targetId(),payload.expectedRevision(),payload.action()==AdoptionActionPayload.Action.CONFIRM);PacketDistributor.sendToPlayer(player,new CityManagementResultPayload(result.success(),"civitas.adoption."+result.reason()));}

    private static void handleMigrationDecision(MigrationDecisionPayload payload,IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player)||!(player.level() instanceof ServerLevel level))return;CityCoreBlockEntity core=level.getBlockEntity(payload.corePos())instanceof CityCoreBlockEntity found?found:null;City city=core==null||core.cityId()==null?null:CitySavedData.get(level.getServer()).byId(core.cityId()).orElse(null);
        if(core==null||!core.isActivated()||city==null||player.distanceToSqr(payload.corePos().getCenter())>64){PacketDistributor.sendToPlayer(player,new CityManagementResultPayload(false,"civitas.migration.stale_core"));return;}
        UUID residence=payload.approve()?availableResidence(level,city):null;var result=DECIDE_MIGRATION.decide(PopulationSavedData.get(level.getServer()),city,player.getUUID(),payload.groupId(),payload.expectedRevision(),payload.approve(),residence,level.getGameTime());String key=result.success()?(payload.approve()?"civitas.migration.approved":"civitas.migration.rejected"):"civitas.migration."+result.failure().name().toLowerCase(java.util.Locale.ROOT);PacketDistributor.sendToPlayer(player,new CityManagementResultPayload(result.success(),key));
    }
    private static UUID availableResidence(ServerLevel level,City city){int limit=CivitasConfig.POPULATION_DASHBOARD_SCAN_LIMIT.get();PopulationSavedData population=PopulationSavedData.get(level.getServer());var page=population.citizenPage(city.id(),limit);var buildingPage=BuildingSavedData.get(level.getServer()).cityPage(city.id(),limit);if(page.truncated()||buildingPage.truncated())return null;java.util.Map<UUID,Long>used=page.records().stream().filter(c->c.residenceId()!=null).collect(java.util.stream.Collectors.groupingBy(c->c.residenceId(),java.util.stream.Collectors.counting()));return buildingPage.records().stream().filter(b->b.status()==BuildingStatus.VALID&&b.purpose()==BuildingPurpose.RESIDENCE&&used.getOrDefault(b.id(),0L)<b.capacity()).sorted(java.util.Comparator.comparing(com.seaquake6324.civitas.domain.building.BuildingRecord::id)).map(com.seaquake6324.civitas.domain.building.BuildingRecord::id).findFirst().orElse(null);}

    private static void handleNpcMarriageProposal(SubmitNpcMarriageProposalPayload payload,IPayloadContext context){if(!(context.player() instanceof ServerPlayer player))return;var result=MarriageInteractionManager.propose(player,payload.citizenId(),payload.expectedRevision());PacketDistributor.sendToPlayer(player,new MarriageActionResultPayload(result.success(),"civitas.marriage."+result.reason()));}
    private static void handlePlayerMarriageAction(SubmitPlayerMarriageActionPayload payload,IPayloadContext context){if(!(context.player() instanceof ServerPlayer player))return;var result=MarriageInteractionManager.playerAction(player,payload.targetId(),payload.action(),payload.proposalId(),payload.expectedRevision());PacketDistributor.sendToPlayer(player,new MarriageActionResultPayload(result.success(),"civitas.marriage."+result.reason()));if(result.success()&&("pending".equals(result.reason())||"accepted".equals(result.reason()))){ServerPlayer target=player.level().getServer().getPlayerList().getPlayer(payload.targetId());if(target!=null)target.sendSystemMessage(net.minecraft.network.chat.Component.translatable("civitas.marriage.notice."+result.reason(),player.getGameProfile().name()),true);}}
    private static void handleNpcReproduction(SubmitNpcReproductionPayload payload,IPayloadContext context){if(!(context.player() instanceof ServerPlayer player))return;var result=ReproductionInteractionManager.npcConceive(player,payload.citizenId(),payload.expectedRevision());PacketDistributor.sendToPlayer(player,new ReproductionActionResultPayload(result.success(),"civitas.reproduction."+result.reason()));}
    private static void handlePlayerReproduction(SubmitPlayerReproductionPayload payload,IPayloadContext context){if(!(context.player() instanceof ServerPlayer player))return;var result=ReproductionInteractionManager.playerAction(player,payload.targetId(),payload.action(),payload.consentId(),payload.expectedRevision());PacketDistributor.sendToPlayer(player,new ReproductionActionResultPayload(result.success(),"civitas.reproduction."+result.reason()));if(result.success()&&("pending".equals(result.reason())||"conceived".equals(result.reason()))){ServerPlayer target=player.level().getServer().getPlayerList().getPlayer(payload.targetId());if(target!=null)target.sendSystemMessage(net.minecraft.network.chat.Component.translatable("civitas.reproduction.notice."+result.reason(),player.getGameProfile().name()),true);}}

    private static void handleGenderSelection(SubmitGenderSelectionPayload payload,IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player))return;PopulationSavedData data=PopulationSavedData.get(player.level().getServer());
        if(data.readOnly()){PacketDistributor.sendToPlayer(player,new GenderSelectionResultPayload(false,"civitas.gender.read_only"));return;}
        var result=SELECT_GENDER.select(data,player.getUUID(),payload.gender());
        PacketDistributor.sendToPlayer(player,new GenderSelectionResultPayload(result.success(),result.success()?"civitas.gender.saved":"civitas.gender.already_selected"));
    }

    private static void handleBuildingRegistration(BeginBuildingRegistrationPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        CityCoreBlockEntity core = level.getBlockEntity(payload.corePos()) instanceof CityCoreBlockEntity found ? found : null;
        City city = core == null || core.cityId() == null ? null : CitySavedData.get(level.getServer()).byId(core.cityId()).orElse(null);
        if (city == null || !core.isActivated() || player.distanceToSqr(payload.corePos().getCenter()) > 64) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("civitas.building.failure.stale_city"), true);
            return;
        }
        BuildingRegistrationManager.begin(player, city, payload.purpose());
    }

    private static void handlePatrolRoute(BeginPatrolRoutePayload payload,IPayloadContext context){if(!(context.player() instanceof ServerPlayer player)||!(player.level() instanceof ServerLevel level))return;CityCoreBlockEntity core=level.getBlockEntity(payload.corePos())instanceof CityCoreBlockEntity found?found:null;City city=core==null||core.cityId()==null?null:CitySavedData.get(level.getServer()).byId(core.cityId()).orElse(null);if(city==null||!core.isActivated()||player.distanceToSqr(payload.corePos().getCenter())>64){player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("civitas.patrol.failure.stale_city"),true);return;}PatrolRouteManager.begin(player,city);}

    private static void handleStorageAuthorization(BeginStorageAuthorizationPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        CityCoreBlockEntity core = level.getBlockEntity(payload.corePos()) instanceof CityCoreBlockEntity found ? found : null;
        City city = core == null || core.cityId() == null ? null : CitySavedData.get(level.getServer()).byId(core.cityId()).orElse(null);
        if (city == null || !core.isActivated() || player.distanceToSqr(payload.corePos().getCenter()) > 64) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("civitas.storage.failure.stale_core"), true); return;
        }
        StorageAuthorizationManager.begin(player, city, payload.buildingId(), payload.expectedRevision());
    }

    private static void handleMembership(CityMembershipActionPayload payload,IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player)||!(player.level() instanceof ServerLevel level))return;
        CityCoreBlockEntity core=level.getBlockEntity(payload.corePos())instanceof CityCoreBlockEntity found?found:null;
        CitySavedData repository=CitySavedData.get(level.getServer()); City city=core==null||core.cityId()==null?null:repository.byId(core.cityId()).orElse(null);
        if(city==null||core==null||!core.isActivated()){PacketDistributor.sendToPlayer(player,new CityManagementResultPayload(false,"civitas.core_manage.stale_core"));return;}
        boolean atCore=player.distanceToSqr(payload.corePos().getCenter())<=64;
        if(!atCore){PacketDistributor.sendToPlayer(player,new CityManagementResultPayload(false,"civitas.membership.error.not_at_core"));return;}
        if(payload.action()==CityMembershipActionPayload.Action.EXPAND){TerritoryExpansionManager.begin(player,city);return;}
        MembershipService.Result result=switch(payload.action()){
            case APPLY->MEMBERSHIP.apply(city,player.getUUID(),level.getGameTime(),true);
            case APPROVE->MEMBERSHIP.decide(city,player.getUUID(),payload.target(),true);
            case REJECT->MEMBERSHIP.decide(city,player.getUUID(),payload.target(),false);
            case LEAVE->MEMBERSHIP.leaveOrRemove(city,player.getUUID(),player.getUUID());
            case REMOVE->MEMBERSHIP.leaveOrRemove(city,player.getUUID(),payload.target());
            case EXPAND->throw new IllegalStateException();};
        if(!result.success()){PacketDistributor.sendToPlayer(player,new CityManagementResultPayload(false,"civitas.membership.error."+result.failure().name().toLowerCase(java.util.Locale.ROOT)));return;}
        repository.add(result.city()); PacketDistributor.sendToPlayer(player,new CityManagementResultPayload(true,"civitas.membership.updated"));
    }

    private static void handleCityManagement(SubmitCityManagementPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        CityCoreBlockEntity core = level.getBlockEntity(payload.corePos()) instanceof CityCoreBlockEntity found ? found : null;
        CitySavedData repository = CitySavedData.get(level.getServer());
        City city = core == null || core.cityId() == null ? null : repository.byId(core.cityId()).orElse(null);
        if (core == null || !core.isActivated() || city == null) {
            PacketDistributor.sendToPlayer(player, new CityManagementResultPayload(false, "civitas.core_manage.stale_core"));
            return;
        }
        EditCityIdentityService.Result result = EDIT_CITY.edit(repository, new EditCityIdentityService.Request(city,
                player.getUUID(), core.cityId(), payload.corePos().asLong(), player.distanceToSqr(payload.corePos().getCenter()),
                payload.cityName(), payload.cityColor()));
        if (!result.success()) {
            PacketDistributor.sendToPlayer(player, new CityManagementResultPayload(false, result.errorKey()));
            return;
        }
        City updated = result.city();
        core.activate(updated.id(), updated.name(), updated.color(), updated.activatedAt());
        level.sendBlockUpdated(payload.corePos(), core.getBlockState(), core.getBlockState(), 3);
        CityMapNetworkSync.broadcastUpsert(level.getServer(), updated);
        PacketDistributor.sendToPlayer(player, new CityManagementResultPayload(true, "civitas.gui.city_saved"));
    }

    private static void handleDebugToggle(ToggleRegionDebugPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) RegionDebugManager.toggle(player);
    }

    private static void handleSubmit(SubmitFoundingPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        CityCoreBlockEntity core = level.getBlockEntity(payload.corePos()) instanceof CityCoreBlockEntity found ? found : null;
        ChunkPos coreChunk = ChunkPos.containing(payload.corePos());
        String dimension = level.dimension().identifier().toString();
        FoundCityService.Request request = new FoundCityService.Request(player.getUUID(), dimension,
                payload.corePos().asLong(), new ChunkCoordinate(coreChunk.x(), coreChunk.z()), core != null,
                core != null && core.isActivated(), core == null ? null : core.placerId(),
                player.distanceToSqr(payload.corePos().getCenter()), payload.name(), payload.color(), level.getGameTime(),
                CivitasConfig.ALLOWED_DIMENSIONS.get().contains(dimension), CivitasConfig.MINIMUM_CORE_DISTANCE_CHUNKS.get(),
                CivitasConfig.BORDER_BUFFER_CHUNKS.get());
        FoundCityService.Result result = FOUND_CITY.found(CitySavedData.get(level.getServer()), request);
        if (!result.success()) {
            PacketDistributor.sendToPlayer(player, new FoundingResultPayload(false, result.errorKey()));
            return;
        }
        City city = result.city();
        core.activate(city.id(), city.name(), city.color(), city.activatedAt());
        level.sendBlockUpdated(payload.corePos(), core.getBlockState(), core.getBlockState(), 3);
        CityMapNetworkSync.broadcastUpsert(player.level().getServer(), city);
        PacketDistributor.sendToPlayer(player, new FoundingResultPayload(true, "civitas.gui.city_created"));
        FoundingAnimationPayload animation = new FoundingAnimationPayload(payload.corePos(), city.name(), city.color());
        PacketDistributor.sendToPlayersNear(level, null, payload.corePos().getX() + 0.5, payload.corePos().getY() + 0.5,
                payload.corePos().getZ() + 0.5, 96.0, animation);
        if (CivitasConfig.SERVER_WIDE_ANNOUNCEMENTS.get()) {
            // The optional global announcement is deliberately a HUD payload, never a chat component.
            PacketDistributor.sendToAllPlayers(new CityAnnouncementPayload(city.name(), city.dimension(), city.color()));
        }
    }

    private CivitasNetwork() {}
}
