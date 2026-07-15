package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FoundingAnimationPayload(BlockPos corePos, String cityName, int color) implements CustomPacketPayload {
    public static final Type<FoundingAnimationPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "founding_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FoundingAnimationPayload> STREAM_CODEC = CustomPacketPayload.codec(
            FoundingAnimationPayload::write,
            buffer -> new FoundingAnimationPayload(buffer.readBlockPos(), buffer.readUtf(80), buffer.readInt()));
    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(corePos);
        buffer.writeUtf(cityName, 80);
        buffer.writeInt(color);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
