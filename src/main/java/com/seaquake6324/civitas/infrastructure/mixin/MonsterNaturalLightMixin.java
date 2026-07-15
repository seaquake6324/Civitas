package com.seaquake6324.civitas.infrastructure.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Natural hostile spawning observes sky/daylight but deliberately ignores emitted block light. */
@Mixin(Monster.class)
abstract class MonsterNaturalLightMixin {
    @Inject(method = "checkMonsterSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void civitas$naturalSpawnIgnoresBlockLight(EntityType<? extends Monster> type,
            ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random,
            CallbackInfoReturnable<Boolean> callback) {
        if (reason != EntitySpawnReason.NATURAL) return;
        int sky = level.getBrightness(LightLayer.SKY, pos);
        boolean skyAllowsSpawn = sky <= random.nextInt(32);
        int skyDarkening = level.getLevel().isThundering() ? 10 : level.getSkyDarken();
        int effectiveSky = Math.max(0, sky - skyDarkening);
        boolean lightTest = effectiveSky <= level.dimensionType().monsterSpawnLightTest().sample(random);
        callback.setReturnValue(level.getDifficulty() != Difficulty.PEACEFUL && skyAllowsSpawn && lightTest
                && Mob.checkMobSpawnRules(type, level, reason, pos, random));
    }
}
