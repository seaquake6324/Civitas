package com.seaquake6324.civitas.presentation.client;

import com.seaquake6324.civitas.infrastructure.network.CoreMoveModePayload;
import com.seaquake6324.civitas.presentation.client.map.ClientCityMapStore;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import com.seaquake6324.civitas.infrastructure.registry.CivitasBlocks;

/** Client-side targeting state consumed by the translucent gizmo renderer. */
public final class CoreMovePreview {
    private static UUID cityId;
    private static BlockPos originalPosition;
    private static BlockPos targetPosition;
    private static boolean targetValid;

    public static void accept(CoreMoveModePayload payload) {
        if (!payload.active()) {
            clear();
            return;
        }
        cityId = payload.cityId();
        originalPosition = payload.originalPosition();
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (cityId == null || minecraft.level == null || minecraft.player == null) return;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            targetPosition = null;
            return;
        }
        BlockPos target = hit.getBlockPos().relative(hit.getDirection());
        boolean replaceable = minecraft.level.isInWorldBounds(target) && minecraft.level.getWorldBorder().isWithinBounds(target)
                && minecraft.level.getBlockState(target).canBeReplaced()
                && minecraft.level.isUnobstructed(CivitasBlocks.CITY_CORE.get().defaultBlockState(), target, CollisionContext.empty());
        boolean supported = minecraft.level.isInWorldBounds(target.below())
                && minecraft.level.getBlockState(target.below()).isFaceSturdy(minecraft.level, target.below(), Direction.UP);
        boolean owned = ClientCityMapStore.cityAt(minecraft.level.dimension(),
                Math.floorDiv(target.getX(), 16), Math.floorDiv(target.getZ(), 16))
                .filter(city -> city.id().equals(cityId)).isPresent();
        targetPosition = target.immutable();
        targetValid = !target.equals(originalPosition) && replaceable && supported && owned;
    }

    public static BlockPos targetPosition() { return targetPosition; }
    public static boolean targetValid() { return targetValid; }

    public static void clear() {
        cityId = null;
        originalPosition = null;
        targetPosition = null;
        targetValid = false;
    }
    private CoreMovePreview() {}
}
