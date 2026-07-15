package com.seaquake6324.civitas.infrastructure.world;

import com.seaquake6324.civitas.application.RegisterBuildingService;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.building.BuildingPurpose;
import com.seaquake6324.civitas.domain.building.BuildingValidation;
import com.seaquake6324.civitas.domain.building.CityTopologyToken;
import com.seaquake6324.civitas.infrastructure.building.BoundedBuildingScanner;
import com.seaquake6324.civitas.infrastructure.persistence.BuildingSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.network.BuildingRegistrationModePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Two-click, server-authoritative registration session. All world access remains on the main thread. */
public final class BuildingRegistrationManager {
    private static final Map<UUID, Session> ACTIVE = new HashMap<>();
    private static final BoundedBuildingScanner SCANNER = new BoundedBuildingScanner();
    private static final RegisterBuildingService SERVICE = new RegisterBuildingService();
    private static long scans, accepted, rejected, elapsedMicros, staleResults;
    private static int maxVisited,maxQueued;

    public static void begin(ServerPlayer player, City city, BuildingPurpose purpose) {
        if (!city.mayManage(player.getUUID())) {
            notice(player, "civitas.building.failure.not_manager"); return;
        }
        TerritoryExpansionManager.remove(player);
        CoreMoveManager.remove(player);
        ACTIVE.put(player.getUUID(), new Session(city.id(), CityTopologyToken.of(city), purpose, null));
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, BuildingRegistrationModePayload.started(purpose));
        notice(player, "civitas.building.select_entrance");
    }

    public static boolean active(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }
    public static void remove(ServerPlayer player) {
        if (ACTIVE.remove(player.getUUID()) != null)
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, BuildingRegistrationModePayload.stopped());
    }

    public static boolean cancelAtCore(ServerPlayer player, CityCoreBlockEntity core) {
        Session session = ACTIVE.get(player.getUUID());
        if (session == null || core.cityId() == null || !core.cityId().equals(session.cityId())) return false;
        remove(player); notice(player, "civitas.building.cancelled"); return true;
    }

    public static void select(ServerLevel level, ServerPlayer player, BlockPos clicked, Direction face) {
        Session session = ACTIVE.get(player.getUUID());
        if (session == null) return;
        City city = CitySavedData.get(level.getServer()).byId(session.cityId()).orElse(null);
        if (city == null || !city.mayManage(player.getUUID()) || !city.dimension().equals(level.dimension().identifier().toString())) {
            remove(player); notice(player, "civitas.building.failure.stale_city"); return;
        }
        if (session.entrance() == null) {
            var block = level.getBlockState(clicked).getBlock();
            if (!(block instanceof DoorBlock || block instanceof FenceGateBlock)) {
                notice(player, "civitas.building.failure.entrance_invalid"); return;
            }
            ACTIVE.put(player.getUUID(), new Session(session.cityId(), session.cityRevision(), session.purpose(), clicked.immutable()));
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    BuildingRegistrationModePayload.entrance(session.purpose(), clicked));
            notice(player, "civitas.building.select_interior");
            return;
        }
        BlockPos interior = clicked.relative(face);
        BuildingValidation.Evidence evidence = SCANNER.scan(level, city, session.entrance(), interior);
        scans++; elapsedMicros += evidence.elapsedMicros(); maxVisited = Math.max(maxVisited, evidence.visited());maxQueued=Math.max(maxQueued,evidence.queuePeak());
        var result = SERVICE.register(BuildingSavedData.get(level.getServer()), new RegisterBuildingService.Request(
                city, player.getUUID(), level.dimension().identifier().toString(), session.purpose(),
                session.entrance().asLong(), interior.asLong(), session.cityRevision(), level.getGameTime(), evidence));
        if (result.success()) {
            accepted++; remove(player); notice(player, "civitas.building.registered");
        } else {
            rejected++;
            if (result.validation().failures().contains(BuildingValidation.Failure.STALE_CITY)) staleResults++;
            BuildingValidation.Failure failure = result.validation().failures().getFirst();
            notice(player, "civitas.building.failure." + failure.name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public static Metrics metrics() {
        return new Metrics(scans, accepted, rejected, staleResults, elapsedMicros,
                scans == 0 ? 0 : elapsedMicros / scans, maxVisited, maxQueued, ACTIVE.size());
    }
    private static void notice(ServerPlayer player, String key) { player.sendSystemMessage(Component.translatable(key), true); }
    private record Session(UUID cityId, long cityRevision, BuildingPurpose purpose, BlockPos entrance) {}
    public record Metrics(long scans, long accepted, long rejected, long staleResults, long totalMicros,
            long averageMicros, int maxVisited, int maxQueued, int activeSessions) {}
    private BuildingRegistrationManager() {}
}
