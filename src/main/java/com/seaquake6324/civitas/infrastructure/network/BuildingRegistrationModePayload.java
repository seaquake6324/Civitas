package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import com.seaquake6324.civitas.domain.building.BuildingPurpose;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BuildingRegistrationModePayload(boolean active, BuildingPurpose purpose,
        boolean entranceSelected, BlockPos entrance) implements CustomPacketPayload {
    public static final Type<BuildingRegistrationModePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "building_registration_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuildingRegistrationModePayload> STREAM_CODEC =
            CustomPacketPayload.codec((payload, buffer) -> {
                buffer.writeBoolean(payload.active()); buffer.writeEnum(payload.purpose());
                buffer.writeBoolean(payload.entranceSelected()); buffer.writeBlockPos(payload.entrance());
            }, buffer -> new BuildingRegistrationModePayload(buffer.readBoolean(),
                    buffer.readEnum(BuildingPurpose.class), buffer.readBoolean(), buffer.readBlockPos()));
    public static BuildingRegistrationModePayload started(BuildingPurpose purpose) {
        return new BuildingRegistrationModePayload(true, purpose, false, BlockPos.ZERO);
    }
    public static BuildingRegistrationModePayload entrance(BuildingPurpose purpose, BlockPos entrance) {
        return new BuildingRegistrationModePayload(true, purpose, true, entrance);
    }
    public static BuildingRegistrationModePayload stopped() {
        return new BuildingRegistrationModePayload(false, BuildingPurpose.RESIDENCE, false, BlockPos.ZERO);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
