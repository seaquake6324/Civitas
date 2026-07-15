package com.seaquake6324.civitas.presentation.client;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;

/** A translucent reconstruction of all eight city-core model sections. */
public final class CoreMoveGhostRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final double[][] PARTS = {
            {1, 0, 1, 15, 4, 15}, {2, 4, 2, 14, 7, 14}, {5, 7, 5, 11, 15, 11},
            {4, 14, 4, 12, 16, 12}, {2, 7, 2, 4, 13, 4}, {12, 7, 2, 14, 13, 4},
            {2, 7, 12, 4, 13, 14}, {12, 7, 12, 14, 13, 14}
    };

    @Override public void emitGizmos(double cameraX, double cameraY, double cameraZ,
            DebugValueAccess debugValues, Frustum frustum, float partialTick) {
        BlockPos target = CoreMovePreview.targetPosition();
        if (target == null) return;
        boolean valid = CoreMovePreview.targetValid();
        int stroke = valid ? 0xDD66FFAA : 0xDDFF5566;
        int fill = valid ? 0x5066FFAA : 0x50FF5566;
        GizmoStyle style = GizmoStyle.strokeAndFill(stroke, 2.0F, fill);
        for (double[] part : PARTS) {
            Gizmos.cuboid(new AABB(target.getX() + part[0] / 16.0, target.getY() + part[1] / 16.0,
                    target.getZ() + part[2] / 16.0, target.getX() + part[3] / 16.0,
                    target.getY() + part[4] / 16.0, target.getZ() + part[5] / 16.0), style);
        }
    }
}
