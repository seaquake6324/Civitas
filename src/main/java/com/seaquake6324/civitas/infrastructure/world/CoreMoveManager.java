package com.seaquake6324.civitas.infrastructure.world;

import com.seaquake6324.civitas.application.MoveCityCoreService;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.infrastructure.network.CityMapNetworkSync;
import com.seaquake6324.civitas.infrastructure.network.CoreMoveModePayload;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.registry.CivitasBlocks;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-owned move sessions and atomic Minecraft block mutation adapter. */
public final class CoreMoveManager {
    private static final MoveCityCoreService SERVICE = new MoveCityCoreService();
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static boolean isMoving(ServerPlayer player) { return SESSIONS.containsKey(player.getUUID()); }

    public static void begin(ServerLevel level, ServerPlayer player, BlockPos pos, CityCoreBlockEntity core) {
        UUID cityId = core.cityId();
        Optional<City> found = cityId == null ? Optional.empty() : CitySavedData.get(level.getServer()).byId(cityId);
        if (found.isEmpty() || found.get().corePosition() != pos.asLong()) {
            notice(player, "civitas.core_move.stale_core");
            return;
        }
        City city = found.get();
        if (!city.mayManage(player.getUUID())) {
            notice(player, "civitas.core_move.not_manager");
            return;
        }
        SESSIONS.put(player.getUUID(), new Session(city.id(), level.dimension().identifier().toString(), pos.immutable()));
        PacketDistributor.sendToPlayer(player, new CoreMoveModePayload(true, city.id(), pos));
        notice(player, "civitas.core_move.started");
    }

    public static void submit(ServerLevel level, ServerPlayer player, BlockPos target) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        CitySavedData repository = CitySavedData.get(level.getServer());
        City city = repository.byId(session.cityId).orElse(null);
        CityCoreBlockEntity original = level.getBlockEntity(session.originalPosition) instanceof CityCoreBlockEntity core ? core : null;
        UUID coreCityId = original == null || original.cityId() == null ? new UUID(0, 0) : original.cityId();
        boolean replaceable = level.isInWorldBounds(target) && level.getWorldBorder().isWithinBounds(target)
                && level.getBlockState(target).canBeReplaced()
                && level.isUnobstructed(CivitasBlocks.CITY_CORE.get().defaultBlockState(), target, CollisionContext.empty());
        boolean supported = level.isInWorldBounds(target.below())
                && level.getBlockState(target.below()).isFaceSturdy(level, target.below(), Direction.UP);
        MoveCityCoreService.Result result = city == null ? null : SERVICE.move(new MoveCityCoreService.Request(city,
                player.getUUID(), coreCityId, level.dimension().identifier().toString(), session.originalPosition.asLong(),
                target.asLong(), ChunkPos.pack(target), replaceable, supported));
        if (result == null || !result.success()) {
            notice(player, result == null ? "civitas.core_move.stale_core" : result.errorKey());
            return;
        }
        if (!level.setBlock(target, CivitasBlocks.CITY_CORE.get().defaultBlockState(), 3)
                || !(level.getBlockEntity(target) instanceof CityCoreBlockEntity moved)) {
            if (level.getBlockState(target).is(CivitasBlocks.CITY_CORE.get())) level.removeBlock(target, false);
            notice(player, "civitas.core_move.blocked");
            return;
        }
        moved.setPlacer(original.placerId() == null ? city.founderId() : original.placerId());
        moved.activate(city.id(), city.name(), city.color(), city.activatedAt());
        level.sendBlockUpdated(target, moved.getBlockState(), moved.getBlockState(), 3);
        level.removeBlock(session.originalPosition, false);
        repository.add(result.city());
        CityMapNetworkSync.broadcastUpsert(level.getServer(), result.city());
        stop(player, "civitas.core_move.success");
    }

    public static void cancel(ServerPlayer player) { stop(player, "civitas.core_move.cancelled"); }

    public static void tick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        if (!(player.level() instanceof ServerLevel level)
                || !session.dimension.equals(level.dimension().identifier().toString())
                || !(level.getBlockEntity(session.originalPosition) instanceof CityCoreBlockEntity core)
                || !session.cityId.equals(core.cityId())) stop(player, "civitas.core_move.cancelled");
    }

    public static void remove(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    private static void stop(ServerPlayer player, String key) {
        if (SESSIONS.remove(player.getUUID()) == null) return;
        PacketDistributor.sendToPlayer(player, CoreMoveModePayload.stopped());
        notice(player, key);
    }
    private static void notice(ServerPlayer player, String key) { player.sendSystemMessage(Component.translatable(key), true); }
    private record Session(UUID cityId, String dimension, BlockPos originalPosition) {}
    private CoreMoveManager() {}
}
