package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SubmitFoundingPayload(BlockPos corePos, String name, int color) implements CustomPacketPayload {
    public static final Type<SubmitFoundingPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "submit_founding"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitFoundingPayload> STREAM_CODEC = CustomPacketPayload.codec(
            SubmitFoundingPayload::write,
            buffer -> new SubmitFoundingPayload(buffer.readBlockPos(), buffer.readUtf(80), buffer.readInt()));
    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(corePos);
        buffer.writeUtf(name, 80);
        buffer.writeInt(color);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
