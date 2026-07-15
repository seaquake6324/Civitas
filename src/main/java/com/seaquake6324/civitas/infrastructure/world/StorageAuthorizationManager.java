package com.seaquake6324.civitas.infrastructure.world;

import com.seaquake6324.civitas.application.AuthorizeBuildingStorageService;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.BuildingSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;

/** Bounded main-thread selection sessions. No inventory is copied or abstracted. */
public final class StorageAuthorizationManager {
    private static final Map<UUID, Session> ACTIVE = new LinkedHashMap<>();
    private static final AuthorizeBuildingStorageService SERVICE = new AuthorizeBuildingStorageService();
    private static long started, accepted, revoked, rejected, expired;

    public static void begin(ServerPlayer player, City city, UUID buildingId, long revision) {
        if (!city.mayManage(player.getUUID())) { notice(player, "civitas.storage.failure.not_manager"); return; }
        BuildingSavedData data = BuildingSavedData.get(player.level().getServer());
        var building = data.byId(buildingId).orElse(null);
        if (building == null || !building.cityId().equals(city.id()) || building.revision() != revision) {
            notice(player, "civitas.storage.failure.stale_revision"); return;
        }
        if (!ACTIVE.containsKey(player.getUUID()) && ACTIVE.size() >= CivitasConfig.BUILDING_STORAGE_AUTHORIZATION_SESSIONS.get()) {
            rejected++; notice(player, "civitas.storage.failure.session_limit"); return;
        }
        BuildingRegistrationManager.remove(player);
        TerritoryExpansionManager.remove(player);
        CoreMoveManager.remove(player);
        long now = player.level().getGameTime();
        ACTIVE.put(player.getUUID(), new Session(city.id(), buildingId, revision,
                now + CivitasConfig.BUILDING_STORAGE_AUTHORIZATION_TICKS.get()));
        started++; notice(player, "civitas.storage.select_container");
    }

    public static boolean active(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }
    public static void remove(ServerPlayer player) { ACTIVE.remove(player.getUUID()); }
    public static void clear() { ACTIVE.clear(); }
    public static void tick(ServerPlayer player) {
        Session session = ACTIVE.get(player.getUUID());
        if (session != null && player.level().getGameTime() > session.expiresAt) {
            ACTIVE.remove(player.getUUID()); expired++; notice(player, "civitas.storage.failure.expired");
        }
    }

    public static void select(ServerLevel level, ServerPlayer player, BlockPos pos) {
        Session session = ACTIVE.get(player.getUUID());
        if (session == null) return;
        City city = CitySavedData.get(level.getServer()).byId(session.cityId).orElse(null);
        boolean actualContainer = level.getBlockEntity(pos) instanceof Container;
        var result = city == null ? new AuthorizeBuildingStorageService.Result(null, AuthorizeBuildingStorageService.Failure.WRONG_CITY)
                : SERVICE.toggle(BuildingSavedData.get(level.getServer()), city, player.getUUID(), session.buildingId,
                        session.revision, pos.asLong(), actualContainer);
        ACTIVE.remove(player.getUUID());
        if (!result.success()) { rejected++; notice(player, "civitas.storage.failure." + result.failure().name().toLowerCase(java.util.Locale.ROOT)); return; }
        boolean enabled = result.building().authorizedStorageEndpoints().contains(pos.asLong());
        if (enabled) accepted++; else revoked++;
        notice(player, enabled ? "civitas.storage.authorized" : "civitas.storage.revoked");
    }

    public static Metrics metrics() { return new Metrics(started, accepted, revoked, rejected, expired, ACTIVE.size()); }
    private static void notice(ServerPlayer player, String key) { player.sendSystemMessage(Component.translatable(key), true); }
    private record Session(UUID cityId, UUID buildingId, long revision, long expiresAt) {}
    public record Metrics(long started, long accepted, long revoked, long rejected, long expired, int activeSessions) {}
    private StorageAuthorizationManager() {}
}
