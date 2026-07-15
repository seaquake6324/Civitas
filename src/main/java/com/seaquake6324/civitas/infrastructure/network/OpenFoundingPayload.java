package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenFoundingPayload(BlockPos corePos) implements CustomPacketPayload {
    public static final Type<OpenFoundingPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "open_founding"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenFoundingPayload> STREAM_CODEC = CustomPacketPayload.codec(
            OpenFoundingPayload::write, buffer -> new OpenFoundingPayload(buffer.readBlockPos()));
    private void write(RegistryFriendlyByteBuf buffer) { buffer.writeBlockPos(corePos); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
