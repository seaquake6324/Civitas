package com.seaquake6324.civitas.presentation.client;

import com.seaquake6324.civitas.domain.building.BuildingPurpose;
import com.seaquake6324.civitas.infrastructure.network.BuildingRegistrationModePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Local pointer hint only; the server performs every authoritative validation. */
public final class BuildingRegistrationPreview {
    private static BuildingPurpose purpose;
    private static BlockPos entrance, target;
    private static boolean entranceSelected, targetValid;

    public static void accept(BuildingRegistrationModePayload payload) {
        if (!payload.active()) { clear(); return; }
        purpose = payload.purpose(); entranceSelected = payload.entranceSelected();
        entrance = entranceSelected ? payload.entrance() : null;
    }
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (purpose == null || minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) { target = null; return; }
        if (!entranceSelected) {
            target = hit.getBlockPos().immutable();
            var block = minecraft.level.getBlockState(target).getBlock();
            targetValid = block instanceof DoorBlock || block instanceof FenceGateBlock;
            return;
        }
        target = hit.getBlockPos().relative(hit.getDirection()).immutable();
        targetValid = entrance.distManhattan(target) <= 16
                && minecraft.level.getBlockState(target).getCollisionShape(minecraft.level, target).isEmpty()
                && minecraft.level.getBlockState(target.above()).getCollisionShape(minecraft.level, target.above()).isEmpty()
                && minecraft.level.getBlockState(target.below()).blocksMotion();
    }
    public static BlockPos target() { return target; }
    public static BlockPos entrance() { return entrance; }
    public static boolean targetValid() { return targetValid; }
    public static boolean entranceSelected() { return entranceSelected; }
    public static void clear() { purpose=null;entrance=null;target=null;entranceSelected=false;targetValid=false; }
    private BuildingRegistrationPreview() {}
}
