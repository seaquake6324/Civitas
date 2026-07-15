package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleRegionDebugPayload() implements CustomPacketPayload {
    public static final ToggleRegionDebugPayload INSTANCE = new ToggleRegionDebugPayload();
    public static final Type<ToggleRegionDebugPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "toggle_region_debug"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleRegionDebugPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> {}, buffer -> INSTANCE);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
