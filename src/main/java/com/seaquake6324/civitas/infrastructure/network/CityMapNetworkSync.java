package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CityMapNetworkSync {
    public static void sendSnapshot(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        List<CityMapRecord> records = CitySavedData.get(server).cities().stream().map(city -> toRecord(server, city)).toList();
        PacketDistributor.sendToPlayer(player, new CityMapSnapshotPayload(records));
    }

    public static void broadcastUpsert(MinecraftServer server, City city) {
        PacketDistributor.sendToAllPlayers(new CityMapUpsertPayload(toRecord(server, city)));
    }

    public static void broadcastRemove(UUID cityId) {
        PacketDistributor.sendToAllPlayers(new CityMapRemovePayload(cityId));
    }

    private static CityMapRecord toRecord(MinecraftServer server, City city) {
        String lordName = server.services().nameToIdCache().get(city.lordId()).map(profile -> profile.name()).orElse("");
        return new CityMapRecord(city.id(), city.name(), city.color(), city.dimension(), lordName, city.territory());
    }

    private CityMapNetworkSync() {}
}
