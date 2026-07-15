package com.seaquake6324.civitas.presentation.client;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;

/** Subtle cell-local selection boxes disappear as soon as the pointer leaves a block. */
public final class BuildingRegistrationPreviewRenderer implements DebugRenderer.SimpleDebugRenderer {
    @Override public void emitGizmos(double cameraX, double cameraY, double cameraZ,
            DebugValueAccess values, Frustum frustum, float partialTick) {
        BlockPos entrance = BuildingRegistrationPreview.entrance();
        if (entrance != null) Gizmos.cuboid(new AABB(entrance).inflate(.015),
                GizmoStyle.strokeAndFill(0xDDE9A95B,2F,0x28E9A95B));
        BlockPos target = BuildingRegistrationPreview.target();
        if (target == null) return;
        boolean valid = BuildingRegistrationPreview.targetValid();
        int stroke = valid ? 0xDD66D995 : 0xDDE06666, fill = valid ? 0x2866D995 : 0x28E06666;
        Gizmos.cuboid(new AABB(target).inflate(.01), GizmoStyle.strokeAndFill(stroke,2F,fill));
    }
}
