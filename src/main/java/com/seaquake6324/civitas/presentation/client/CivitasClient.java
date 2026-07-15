package com.seaquake6324.civitas.presentation.client;

import com.seaquake6324.civitas.CivitasMod;
import com.seaquake6324.civitas.infrastructure.network.FoundingAnimationPayload;
import com.seaquake6324.civitas.infrastructure.network.FoundingResultPayload;
import com.seaquake6324.civitas.infrastructure.network.OpenFoundingPayload;
import com.seaquake6324.civitas.infrastructure.network.CityAnnouncementPayload;
import com.seaquake6324.civitas.infrastructure.network.CityMapSnapshotPayload;
import com.seaquake6324.civitas.infrastructure.network.CityMapUpsertPayload;
import com.seaquake6324.civitas.infrastructure.network.CityMapRemovePayload;
import com.seaquake6324.civitas.presentation.client.map.ClientCityMapStore;
import com.seaquake6324.civitas.presentation.animation.FoundingAnimationManager;
import com.seaquake6324.civitas.presentation.screen.FoundCityScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RegisterDebugRenderersEvent;
import com.seaquake6324.civitas.infrastructure.network.RegionDebugPayload;
import com.seaquake6324.civitas.presentation.client.map.CityMapOverlayController;
import com.seaquake6324.civitas.presentation.client.map.CivitasMapKeys;
import com.seaquake6324.civitas.infrastructure.network.CivilizationHudPayload;
import com.seaquake6324.civitas.infrastructure.network.CoreMoveModePayload;
import com.seaquake6324.civitas.infrastructure.network.OpenCityManagementPayload;
import com.seaquake6324.civitas.infrastructure.network.CityManagementResultPayload;
import com.seaquake6324.civitas.presentation.screen.CityCoreScreen;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.seaquake6324.civitas.infrastructure.registry.CivitasBlocks;
import com.seaquake6324.civitas.infrastructure.network.ExpansionModePayload;
import com.seaquake6324.civitas.infrastructure.network.ExpansionResultPayload;
import com.seaquake6324.civitas.infrastructure.network.TerritoryDebugPayload;
import com.seaquake6324.civitas.infrastructure.network.SystemDebugPayload;
import com.seaquake6324.civitas.infrastructure.network.BuildingRegistrationModePayload;
import com.seaquake6324.civitas.infrastructure.network.PatrolRouteModePayload;
import com.seaquake6324.civitas.infrastructure.network.OpenGenderSelectionPayload;
import com.seaquake6324.civitas.infrastructure.network.GenderSelectionResultPayload;
import com.seaquake6324.civitas.presentation.screen.GenderSelectionScreen;
import com.seaquake6324.civitas.infrastructure.network.OpenCitizenInfoPayload;
import com.seaquake6324.civitas.infrastructure.network.MarriageActionResultPayload;
import com.seaquake6324.civitas.infrastructure.network.OpenPlayerMarriagePayload;
import com.seaquake6324.civitas.infrastructure.network.ReproductionActionResultPayload;
import com.seaquake6324.civitas.infrastructure.network.GuardAssignmentResultPayload;
import com.seaquake6324.civitas.presentation.screen.PlayerMarriageScreen;
import com.seaquake6324.civitas.presentation.screen.CitizenInfoScreen;
import com.seaquake6324.civitas.infrastructure.registry.CivitasEntities;
import com.seaquake6324.civitas.presentation.entity.CitizenRenderer;

@EventBusSubscriber(modid = CivitasMod.MOD_ID, value = Dist.CLIENT)
public final class CivitasClient {
    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenFoundingPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().setScreen(new FoundCityScreen(payload.corePos())));
        event.register(FoundingResultPayload.TYPE, (payload, context) -> {
            if (Minecraft.getInstance().screen instanceof FoundCityScreen screen) screen.handleResult(payload.success(), payload.messageKey());
        });
        event.register(FoundingAnimationPayload.TYPE, (payload, context) ->
                FoundingAnimationManager.start(payload.corePos(), payload.cityName(), payload.color()));
        event.register(CityAnnouncementPayload.TYPE, (payload, context) -> {
            Component message = Component.translatable("civitas.announcement.city_founded", payload.cityName(), payload.dimension())
                    .copy().withStyle(style -> style.withColor(payload.color()));
            Minecraft.getInstance().gui.setOverlayMessage(message, false);
        });
        event.register(CityMapSnapshotPayload.TYPE, (payload, context) -> {
            ClientCityMapStore.replace(payload.cities());
            CityMapOverlayController.dataChanged();
        });
        event.register(CityMapUpsertPayload.TYPE, (payload, context) -> {
            ClientCityMapStore.upsert(payload.city());
            CityMapOverlayController.dataChanged();
        });
        event.register(CityMapRemovePayload.TYPE, (payload, context) -> {
            ClientCityMapStore.remove(payload.cityId());
            CityMapOverlayController.dataChanged();
        });
        event.register(RegionDebugPayload.TYPE, (payload, context) -> RegionDebugOverlay.accept(payload));
        event.register(TerritoryDebugPayload.TYPE,(payload,context)->RegionDebugOverlay.acceptTerritory(payload));
        event.register(SystemDebugPayload.TYPE,(payload,context)->RegionDebugOverlay.acceptSystem(payload));
        event.register(CivilizationHudPayload.TYPE, (payload, context) -> CivilizationHud.accept(payload));
        event.register(CoreMoveModePayload.TYPE, (payload, context) -> CoreMovePreview.accept(payload));
        event.register(ExpansionModePayload.TYPE, (payload, context) -> ExpansionPreview.accept(payload));
        event.register(ExpansionResultPayload.TYPE, (payload, context) -> Minecraft.getInstance().gui.setOverlayMessage(Component.translatable(payload.messageKey()), false));
        event.register(OpenCityManagementPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().setScreen(new CityCoreScreen(payload)));
        event.register(CityManagementResultPayload.TYPE, (payload, context) -> {
            if (Minecraft.getInstance().screen instanceof CityCoreScreen screen) screen.handleResult(payload.success(), payload.messageKey());
        });
        event.register(BuildingRegistrationModePayload.TYPE, (payload, context) -> BuildingRegistrationPreview.accept(payload));
        event.register(PatrolRouteModePayload.TYPE,(payload,context)->PatrolRoutePreview.accept(payload));
        event.register(OpenGenderSelectionPayload.TYPE,(payload,context)->Minecraft.getInstance().setScreen(new GenderSelectionScreen()));
        event.register(GenderSelectionResultPayload.TYPE,(payload,context)->{if(Minecraft.getInstance().screen instanceof GenderSelectionScreen screen)screen.handleResult(payload.success(),payload.messageKey());});
        event.register(OpenCitizenInfoPayload.TYPE,(payload,context)->{if(Minecraft.getInstance().screen instanceof CitizenInfoScreen screen&&screen.citizenId().equals(payload.citizenId()))screen.updateData(payload);else Minecraft.getInstance().setScreen(new CitizenInfoScreen(payload));});
        event.register(MarriageActionResultPayload.TYPE,(payload,context)->{if(Minecraft.getInstance().screen instanceof CitizenInfoScreen screen)screen.handleMarriageResult(payload.success(),payload.messageKey());else if(Minecraft.getInstance().screen instanceof PlayerMarriageScreen screen)screen.handleResult(payload.success(),payload.messageKey());});
        event.register(ReproductionActionResultPayload.TYPE,(payload,context)->{if(Minecraft.getInstance().screen instanceof CitizenInfoScreen screen)screen.handleReproductionResult(payload.success(),payload.messageKey());else if(Minecraft.getInstance().screen instanceof PlayerMarriageScreen screen)screen.handleResult(payload.success(),payload.messageKey());});
        event.register(GuardAssignmentResultPayload.TYPE,(payload,context)->{if(Minecraft.getInstance().screen instanceof CitizenInfoScreen screen)screen.handleGuardResult(payload.success(),payload.messageKey());});
        event.register(OpenPlayerMarriagePayload.TYPE,(payload,context)->Minecraft.getInstance().setScreen(new PlayerMarriageScreen(payload)));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        FoundingAnimationManager.tick();
        CivitasMapKeys.tick();
        CivilizationHud.tick();
        CoreMovePreview.tick();
        ExpansionPreview.tick();
        BuildingRegistrationPreview.tick();
        PatrolRoutePreview.tick();
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!RegionDebugOverlay.isVisible() || event.getScrollDeltaY() == 0.0) return;
        RegionDebugOverlay.cyclePage(event.getScrollDeltaY() < 0.0 ? 1 : -1);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) { CivitasMapKeys.register(event); }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CivitasBlocks.CITY_CORE_ENTITY.get(), CityCoreNameplateRenderer::new);
        event.registerEntityRenderer(CivitasEntities.CITIZEN.get(), CitizenRenderer::new);
    }

    @SubscribeEvent
    public static void registerDebugRenderers(RegisterDebugRenderersEvent event) {
        event.register(new CoreMoveGhostRenderer());
        event.register(new ExpansionPreviewRenderer());
        event.register(new BuildingRegistrationPreviewRenderer());
        event.register(new PatrolRoutePreviewRenderer());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCityMapStore.clear();
        RegionDebugOverlay.hide();
        CivilizationHud.clear();
        CoreMovePreview.clear();
        ExpansionPreview.clear();
        BuildingRegistrationPreview.clear();
        PatrolRoutePreview.clear();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        CivilizationHud.render(event.getGuiGraphics());
        RegionDebugOverlay.render(event.getGuiGraphics());
    }

    private CivitasClient() {}
}
