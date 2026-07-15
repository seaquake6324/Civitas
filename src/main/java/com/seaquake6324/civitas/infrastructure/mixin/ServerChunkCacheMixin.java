package com.seaquake6324.civitas.infrastructure.mixin;

import com.seaquake6324.civitas.infrastructure.spawn.RegionSpawnQuota;
import com.seaquake6324.civitas.infrastructure.spawn.ServerRegionClassifier;
import com.seaquake6324.civitas.infrastructure.civilization.CivilityScanScheduler;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkCache.class)
abstract class ServerChunkCacheMixin {
    @Shadow @Final private ServerLevel level;

    @Redirect(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;createState(ILjava/lang/Iterable;Lnet/minecraft/world/level/NaturalSpawner$ChunkGetter;Lnet/minecraft/world/level/LocalMobCapCalculator;)Lnet/minecraft/world/level/NaturalSpawner$SpawnState;"))
    private NaturalSpawner.SpawnState civitas$createRegionSpawnState(int chunkCount, Iterable<Entity> entities,
            NaturalSpawner.ChunkGetter chunkGetter, LocalMobCapCalculator localCaps) {
        NaturalSpawner.SpawnState state = NaturalSpawner.createState(chunkCount, entities, chunkGetter, localCaps);
        RegionSpawnQuota.beginTick(level, state, chunkCount, entities);
        return state;
    }

    @Inject(method = "blockChanged", at = @At("HEAD"))
    private void civitas$invalidateRegionCache(BlockPos pos, CallbackInfo callback) {
        ServerRegionClassifier.invalidateNear(level, pos);
        CivilityScanScheduler.markNear(level, pos);
    }
}
