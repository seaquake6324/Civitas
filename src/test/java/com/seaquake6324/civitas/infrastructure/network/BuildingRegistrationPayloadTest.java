package com.seaquake6324.civitas.infrastructure.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.seaquake6324.civitas.domain.building.BuildingPurpose;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class BuildingRegistrationPayloadTest {
    @Test void roundTripsServerboundSelectionAndClientPreviewStage() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            var begin = new BeginBuildingRegistrationPayload(new BlockPos(1,64,2), BuildingPurpose.GUARD_POST);
            BeginBuildingRegistrationPayload.STREAM_CODEC.encode(buffer, begin);
            assertEquals(begin, BeginBuildingRegistrationPayload.STREAM_CODEC.decode(buffer));
            buffer.clear();
            var mode = BuildingRegistrationModePayload.entrance(BuildingPurpose.GUARD_POST, new BlockPos(9,65,9));
            BuildingRegistrationModePayload.STREAM_CODEC.encode(buffer, mode);
            assertEquals(mode, BuildingRegistrationModePayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }

    @Test void roundTripsStorageAuthorizationSelection() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            var payload = new BeginStorageAuthorizationPayload(new BlockPos(3,70,4), java.util.UUID.randomUUID(), 12);
            BeginStorageAuthorizationPayload.STREAM_CODEC.encode(buffer, payload);
            assertEquals(payload, BeginStorageAuthorizationPayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}
