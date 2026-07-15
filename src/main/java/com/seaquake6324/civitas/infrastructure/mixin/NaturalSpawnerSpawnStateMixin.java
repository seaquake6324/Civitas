package com.seaquake6324.civitas.infrastructure.mixin;

import com.seaquake6324.civitas.infrastructure.spawn.RegionSpawnQuota;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.SpawnState.class)
abstract class NaturalSpawnerSpawnStateMixin {
    @Inject(method = "canSpawnForCategoryGlobal", at = @At("HEAD"), cancellable = true)
    private void civitas$useSharedHostileGlobalCap(MobCategory category, CallbackInfoReturnable<Boolean> callback) {
        NaturalSpawner.SpawnState state = (NaturalSpawner.SpawnState)(Object)this;
        if (category == MobCategory.MONSTER && RegionSpawnQuota.isTracking(state)) {
            callback.setReturnValue(RegionSpawnQuota.sharedCapacityAvailable(state));
        }
    }

    @Inject(method = "canSpawn", at = @At("RETURN"), cancellable = true)
    private void civitas$checkExactRegionPool(EntityType<?> type, BlockPos pos, ChunkAccess chunk,
            CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() && !RegionSpawnQuota.canSpawn((NaturalSpawner.SpawnState)(Object)this, type, pos)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "afterSpawn", at = @At("TAIL"))
    private void civitas$countRegionSpawn(Mob mob, ChunkAccess chunk, CallbackInfo callback) {
        RegionSpawnQuota.afterSpawn((NaturalSpawner.SpawnState)(Object)this, mob);
    }
}
