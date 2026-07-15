package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import com.seaquake6324.civitas.domain.building.BuildingPurpose;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BeginBuildingRegistrationPayload(BlockPos corePos, BuildingPurpose purpose)
        implements CustomPacketPayload {
    public static final Type<BeginBuildingRegistrationPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "begin_building_registration"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BeginBuildingRegistrationPayload> STREAM_CODEC =
            CustomPacketPayload.codec((payload, buffer) -> {
                buffer.writeBlockPos(payload.corePos()); buffer.writeEnum(payload.purpose());
            }, buffer -> new BeginBuildingRegistrationPayload(buffer.readBlockPos(), buffer.readEnum(BuildingPurpose.class)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
