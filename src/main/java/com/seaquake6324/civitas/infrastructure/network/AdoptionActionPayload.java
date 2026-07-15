package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AdoptionActionPayload(BlockPos corePos,Action action,UUID targetId,long expectedRevision)implements CustomPacketPayload{
    public enum Action{REQUEST,CONFIRM,DECLINE}
    public static final Type<AdoptionActionPayload>TYPE=new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID,"adoption_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AdoptionActionPayload>STREAM_CODEC=CustomPacketPayload.codec((p,b)->{b.writeBlockPos(p.corePos);b.writeEnum(p.action);b.writeUUID(p.targetId);b.writeVarLong(p.expectedRevision);},b->new AdoptionActionPayload(b.readBlockPos(),b.readEnum(Action.class),b.readUUID(),b.readVarLong()));
    @Override public Type<? extends CustomPacketPayload>type(){return TYPE;}
}
