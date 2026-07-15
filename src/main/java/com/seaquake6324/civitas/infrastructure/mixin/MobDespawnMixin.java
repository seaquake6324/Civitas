package com.seaquake6324.civitas.infrastructure.mixin;

import com.seaquake6324.civitas.infrastructure.spawn.NaturalMobDespawn;
import com.seaquake6324.civitas.infrastructure.spawn.RegionSpawnQuota;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Attributes removals only while inside Mob.checkDespawn, avoiding command/death false positives. */
@Mixin(Mob.class)
abstract class MobDespawnMixin {
    @Inject(method = "checkDespawn", at = @At("RETURN"))
    private void civitas$recordDespawnSource(CallbackInfo callback) {
        Mob mob = (Mob)(Object)this;
        if (!mob.isRemoved() || !NaturalMobDespawn.isMarked(mob) || !(mob.level() instanceof ServerLevel level)) return;
        if (NaturalMobDespawn.consumeAcceleratedRemoval(mob)) RegionSpawnQuota.recordAcceleratedDespawn(level);
        else if (NaturalMobDespawn.consumeExternalRemoval(mob)) return;
        else RegionSpawnQuota.recordVanillaDespawn(level);
    }
}
