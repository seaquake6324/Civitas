package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;import net.minecraft.network.RegistryFriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;import net.minecraft.network.protocol.common.custom.CustomPacketPayload;import net.minecraft.resources.Identifier;

public record GuardAssignmentResultPayload(boolean success,String messageKey)implements CustomPacketPayload{
 public static final Type<GuardAssignmentResultPayload>TYPE=new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID,"guard_assignment_result"));
 public static final StreamCodec<RegistryFriendlyByteBuf,GuardAssignmentResultPayload>STREAM_CODEC=CustomPacketPayload.codec((p,b)->{b.writeBoolean(p.success);b.writeUtf(p.messageKey);},b->new GuardAssignmentResultPayload(b.readBoolean(),b.readUtf()));
 @Override public Type<? extends CustomPacketPayload>type(){return TYPE;}
}
