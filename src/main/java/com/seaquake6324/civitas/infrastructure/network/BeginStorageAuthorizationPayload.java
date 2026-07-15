package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BeginStorageAuthorizationPayload(BlockPos corePos, UUID buildingId, long expectedRevision)
        implements CustomPacketPayload {
    public static final Type<BeginStorageAuthorizationPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "begin_storage_authorization"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BeginStorageAuthorizationPayload> STREAM_CODEC =
            CustomPacketPayload.codec((value, buffer) -> { buffer.writeBlockPos(value.corePos); buffer.writeUUID(value.buildingId); buffer.writeVarLong(value.expectedRevision); },
                    buffer -> new BeginStorageAuthorizationPayload(buffer.readBlockPos(), buffer.readUUID(), buffer.readVarLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
