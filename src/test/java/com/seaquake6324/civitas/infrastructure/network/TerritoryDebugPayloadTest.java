package com.seaquake6324.civitas.infrastructure.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class TerritoryDebugPayloadTest {
    @Test void roundTripsEveryExplainabilityField() {
        var payload = new TerritoryDebugPayload(true,"Harbor",14,9,true,2,4,"v1_to_v4",false,
                "WARNING",10,12,20,400,31,42,18,57,12,6,11,28,
                64,30,2,4,1,10,76,.02F,.01F,0,.03F,1200,"WARNING","EAST",0,2,120,70,-45);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            TerritoryDebugPayload.STREAM_CODEC.encode(buffer,payload);
            assertEquals(payload,TerritoryDebugPayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}
