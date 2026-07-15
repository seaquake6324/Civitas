package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CoreMoveModePayload(boolean active, UUID cityId, BlockPos originalPosition) implements CustomPacketPayload {
    private static final UUID EMPTY_ID = new UUID(0, 0);
    public static final Type<CoreMoveModePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "core_move_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CoreMoveModePayload> STREAM_CODEC = CustomPacketPayload.codec(
            CoreMoveModePayload::write, CoreMoveModePayload::read);

    public static CoreMoveModePayload stopped() { return new CoreMoveModePayload(false, EMPTY_ID, BlockPos.ZERO); }
    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeUUID(cityId);
        buffer.writeBlockPos(originalPosition);
    }
    private static CoreMoveModePayload read(RegistryFriendlyByteBuf buffer) {
        return new CoreMoveModePayload(buffer.readBoolean(), buffer.readUUID(), buffer.readBlockPos());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
