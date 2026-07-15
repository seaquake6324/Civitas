package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CityMapRemovePayload(UUID cityId) implements CustomPacketPayload {
    public static final Type<CityMapRemovePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "city_map_remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CityMapRemovePayload> STREAM_CODEC = CustomPacketPayload.codec(
            CityMapRemovePayload::write, buffer -> new CityMapRemovePayload(buffer.readUUID()));
    private void write(RegistryFriendlyByteBuf buffer) { buffer.writeUUID(cityId); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
