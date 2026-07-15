package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;import com.seaquake6324.civitas.domain.security.GuardShift;import java.util.UUID;import net.minecraft.network.RegistryFriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;import net.minecraft.network.protocol.common.custom.CustomPacketPayload;import net.minecraft.resources.Identifier;

public record SubmitGuardAssignmentPayload(UUID citizenId,long expectedCitizenRevision,UUID routeId,long expectedRouteRevision,GuardShift shift,boolean force)implements CustomPacketPayload{
 public static final Type<SubmitGuardAssignmentPayload>TYPE=new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID,"submit_guard_assignment"));
 public static final StreamCodec<RegistryFriendlyByteBuf,SubmitGuardAssignmentPayload>STREAM_CODEC=CustomPacketPayload.codec((p,b)->{b.writeUUID(p.citizenId);b.writeVarLong(p.expectedCitizenRevision);b.writeUUID(p.routeId);b.writeVarLong(p.expectedRouteRevision);b.writeEnum(p.shift);b.writeBoolean(p.force);},b->new SubmitGuardAssignmentPayload(b.readUUID(),b.readVarLong(),b.readUUID(),b.readVarLong(),b.readEnum(GuardShift.class),b.readBoolean()));
 @Override public Type<? extends CustomPacketPayload>type(){return TYPE;}
}
