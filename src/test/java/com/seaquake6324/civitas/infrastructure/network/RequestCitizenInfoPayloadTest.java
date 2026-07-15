package com.seaquake6324.civitas.infrastructure.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class RequestCitizenInfoPayloadTest {
    @Test
    void roundTripsBoundedRefreshTarget() {
        var expected = new RequestCitizenInfoPayload(UUID.randomUUID());
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            RequestCitizenInfoPayload.STREAM_CODEC.encode(buffer, expected);
            assertEquals(expected, RequestCitizenInfoPayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
