package com.seaquake6324.civitas.presentation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seaquake6324.civitas.infrastructure.network.CityMapRecord;
import com.seaquake6324.civitas.infrastructure.world.CityCoreBlockEntity;
import com.seaquake6324.civitas.presentation.client.map.ClientCityMapStore;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Renders the targeted activated core like a two-line entity name tag in world space. */
public final class CityCoreNameplateRenderer
        implements BlockEntityRenderer<CityCoreBlockEntity, CityCoreNameplateRenderer.State> {
    private static final Vec3 CORE_LABEL_ATTACHMENT = new Vec3(0.5, 1.0, 0.5);
    private static final Vec3 CITY_NAME_ATTACHMENT = new Vec3(0.5, 1.25875, 0.5);

    public CityCoreNameplateRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CityCoreBlockEntity core, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(core, state, partialTick, cameraPos, breakProgress);
        state.visible = false;
        state.cityName = null;
        state.coreLabel = null;

        Minecraft minecraft = Minecraft.getInstance();
        if (!core.isActivated() || minecraft.level == null || minecraft.player == null || minecraft.screen != null
                || minecraft.options.hideGui || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(core.getBlockPos())) return;

        String name = core.cityName();
        int color = core.cityColor();
        Optional<CityMapRecord> city = core.cityId() == null ? Optional.empty() : ClientCityMapStore.byId(core.cityId());
        if (city.isPresent()) {
            name = city.get().name();
            color = city.get().color();
        }
        if (name == null || name.isBlank()) return;

        int nameColor = color & 0xFFFFFF;
        state.cityName = Component.literal(name).withStyle(style -> style.withColor(nameColor));
        state.coreLabel = Component.translatable("civitas.hud.city_core");
        state.distanceToCameraSquared = cameraPos.distanceToSqr(Vec3.atCenterOf(core.getBlockPos()));
        state.visible = true;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!state.visible || state.cityName == null || state.coreLabel == null) return;
        collector.submitNameTag(poseStack, CITY_NAME_ATTACHMENT, 0, state.cityName, true,
                state.lightCoords, state.distanceToCameraSquared, cameraState);
        collector.submitNameTag(poseStack, CORE_LABEL_ATTACHMENT, 0, state.coreLabel, true,
                state.lightCoords, state.distanceToCameraSquared, cameraState);
    }

    public static final class State extends BlockEntityRenderState {
        private boolean visible;
        private Component cityName;
        private Component coreLabel;
        private double distanceToCameraSquared;
    }
}
