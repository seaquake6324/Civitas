package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CityAnnouncementPayload(String cityName, String dimension, int color) implements CustomPacketPayload {
    public static final Type<CityAnnouncementPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "city_announcement"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CityAnnouncementPayload> STREAM_CODEC = CustomPacketPayload.codec(
            CityAnnouncementPayload::write,
            buffer -> new CityAnnouncementPayload(buffer.readUtf(80), buffer.readUtf(128), buffer.readInt()));
    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(cityName, 80);
        buffer.writeUtf(dimension, 128);
        buffer.writeInt(color);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
