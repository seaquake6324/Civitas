package com.seaquake6324.civitas.infrastructure.civilization;

import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.civilization.ChunkCivilization;
import com.seaquake6324.civitas.domain.civilization.CivilizationLayer;
import com.seaquake6324.civitas.infrastructure.network.CivilizationHudPayload;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

/** Adds a short server-side stability window so classification boundaries cannot flash the HUD. */
public final class CivilizationHudSync {
    private static final int SAMPLE_INTERVAL = 5;
    private static final int STABLE_SAMPLES = 3;
    private static final Map<UUID, Tracking> TRACKING = new HashMap<>();

    public static void tick(ServerPlayer player) {
        if (player.tickCount % SAMPLE_INTERVAL != 0 || !(player.level() instanceof ServerLevel level)) return;
        CivilizationLayer layer = CivilizationAccess.layer(level, player.blockPosition());
        long chunk = ChunkPos.pack(player.blockPosition());
        ChunkCivilization state = CivilizationAccess.state(level, player.blockPosition());
        Optional<City> city = CitySavedData.get(level.getServer()).cityAt(level.dimension().identifier().toString(), chunk);
        Snapshot next = new Snapshot(city.map(value -> value.id().toString()).orElse("wild"),
                city.map(City::name).orElse(""), city.map(City::color).orElse(0xB58A52), layer,
                state.civilityTier().ordinal(), state.activityTier().ordinal());
        Tracking tracking = TRACKING.computeIfAbsent(player.getUUID(), ignored -> new Tracking());
        if (!next.equals(tracking.candidate)) {
            tracking.candidate = next;
            tracking.samples = 1;
            return;
        }
        tracking.samples++;
        if (tracking.samples < STABLE_SAMPLES || next.equals(tracking.sent)) return;
        tracking.sent = next;
        PacketDistributor.sendToPlayer(player, new CivilizationHudPayload(next.cityIdentity, next.cityName,
                next.cityColor, next.layer.ordinal(), next.civilityTier, next.activityTier));
    }

    public static void remove(ServerPlayer player) { TRACKING.remove(player.getUUID()); }

    private record Snapshot(String cityIdentity, String cityName, int cityColor, CivilizationLayer layer,
                            int civilityTier, int activityTier) {}
    private static final class Tracking { private Snapshot candidate; private Snapshot sent; private int samples; }
    private CivilizationHudSync() {}
}
