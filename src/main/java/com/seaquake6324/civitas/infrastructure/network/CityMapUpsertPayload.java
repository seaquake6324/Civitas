package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CityMapUpsertPayload(CityMapRecord city) implements CustomPacketPayload {
    public static final Type<CityMapUpsertPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "city_map_upsert"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CityMapUpsertPayload> STREAM_CODEC = CustomPacketPayload.codec(
            CityMapUpsertPayload::write, CityMapUpsertPayload::read);

    private void write(RegistryFriendlyByteBuf buffer) { CityMapSnapshotPayload.writeRecord(buffer, city); }
    private static CityMapUpsertPayload read(RegistryFriendlyByteBuf buffer) { return new CityMapUpsertPayload(CityMapSnapshotPayload.readRecord(buffer)); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
