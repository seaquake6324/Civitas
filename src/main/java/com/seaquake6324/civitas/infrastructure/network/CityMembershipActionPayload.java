package com.seaquake6324.civitas.infrastructure.network;
import com.seaquake6324.civitas.CivitasMod;import java.util.UUID;import net.minecraft.core.BlockPos;import net.minecraft.network.RegistryFriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;import net.minecraft.network.protocol.common.custom.CustomPacketPayload;import net.minecraft.resources.Identifier;
public record CityMembershipActionPayload(BlockPos corePos,Action action,UUID target)implements CustomPacketPayload{
 public enum Action{APPLY,APPROVE,REJECT,LEAVE,REMOVE,EXPAND}
 public static final Type<CityMembershipActionPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID,"city_membership_action"));
 public static final StreamCodec<RegistryFriendlyByteBuf,CityMembershipActionPayload> STREAM_CODEC=CustomPacketPayload.codec((p,b)->{b.writeBlockPos(p.corePos);b.writeEnum(p.action);b.writeUUID(p.target);},b->new CityMembershipActionPayload(b.readBlockPos(),b.readEnum(Action.class),b.readUUID()));
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
