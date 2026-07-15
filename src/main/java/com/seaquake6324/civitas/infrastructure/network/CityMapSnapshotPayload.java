package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CityMapSnapshotPayload(List<CityMapRecord> cities) implements CustomPacketPayload {
    public static final Type<CityMapSnapshotPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "city_map_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CityMapSnapshotPayload> STREAM_CODEC = CustomPacketPayload.codec(
            CityMapSnapshotPayload::write, CityMapSnapshotPayload::read);

    public CityMapSnapshotPayload { cities = List.copyOf(cities); }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(cities.size());
        for (CityMapRecord city : cities) writeRecord(buffer, city);
    }

    private static CityMapSnapshotPayload read(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<CityMapRecord> cities = new ArrayList<>(size);
        for (int i = 0; i < size; i++) cities.add(readRecord(buffer));
        return new CityMapSnapshotPayload(cities);
    }

    static void writeRecord(RegistryFriendlyByteBuf buffer, CityMapRecord city) {
        buffer.writeUUID(city.id());
        buffer.writeUtf(city.name(), 80);
        buffer.writeInt(city.color());
        buffer.writeUtf(city.dimension(), 128);
        buffer.writeUtf(city.lordName(), 64);
        buffer.writeVarInt(city.territory().size());
        for (long chunk : city.territory()) buffer.writeLong(chunk);
    }

    static CityMapRecord readRecord(RegistryFriendlyByteBuf buffer) {
        UUID id = buffer.readUUID();
        String name = buffer.readUtf(80);
        int color = buffer.readInt();
        String dimension = buffer.readUtf(128);
        String lordName = buffer.readUtf(64);
        int count = buffer.readVarInt();
        Set<Long> territory = new HashSet<>(count);
        for (int i = 0; i < count; i++) territory.add(buffer.readLong());
        return new CityMapRecord(id, name, color, dimension, lordName, territory);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
