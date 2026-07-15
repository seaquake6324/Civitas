package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FoundingResultPayload(boolean success, String messageKey) implements CustomPacketPayload {
    public static final Type<FoundingResultPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "founding_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FoundingResultPayload> STREAM_CODEC = CustomPacketPayload.codec(
            FoundingResultPayload::write,
            buffer -> new FoundingResultPayload(buffer.readBoolean(), buffer.readUtf(128)));
    private void write(RegistryFriendlyByteBuf buffer) { buffer.writeBoolean(success); buffer.writeUtf(messageKey, 128); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
