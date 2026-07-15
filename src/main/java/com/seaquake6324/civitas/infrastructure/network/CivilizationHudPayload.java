package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CivilizationHudPayload(String cityIdentity, String cityName, int cityColor, int layer,
        int civilityTier, int activityTier) implements CustomPacketPayload {
    public static final Type<CivilizationHudPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "civilization_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CivilizationHudPayload> STREAM_CODEC = CustomPacketPayload.codec(
            CivilizationHudPayload::write, CivilizationHudPayload::read);

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(cityIdentity, 64);
        buffer.writeUtf(cityName, 80);
        buffer.writeInt(cityColor);
        buffer.writeVarInt(layer);
        buffer.writeVarInt(civilityTier);
        buffer.writeVarInt(activityTier);
    }

    private static CivilizationHudPayload read(RegistryFriendlyByteBuf buffer) {
        return new CivilizationHudPayload(buffer.readUtf(64), buffer.readUtf(80), buffer.readInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
