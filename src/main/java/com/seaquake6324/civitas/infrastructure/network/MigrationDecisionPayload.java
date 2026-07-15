package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MigrationDecisionPayload(BlockPos corePos,UUID groupId,long expectedRevision,boolean approve)implements CustomPacketPayload{
    public static final Type<MigrationDecisionPayload>TYPE=new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID,"migration_decision"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MigrationDecisionPayload>STREAM_CODEC=CustomPacketPayload.codec((p,b)->{b.writeBlockPos(p.corePos);b.writeUUID(p.groupId);b.writeVarLong(p.expectedRevision);b.writeBoolean(p.approve);},b->new MigrationDecisionPayload(b.readBlockPos(),b.readUUID(),b.readVarLong(),b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload>type(){return TYPE;}
}
