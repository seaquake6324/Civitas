package com.seaquake6324.civitas.infrastructure.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class RegionDebugPayloadTest {
    @Test
    void roundTripsTheCompleteThreePageSnapshot() {
        RegionDebugPayload payload = new RegionDebugPayload(true, 12_345L,
                new RegionDebugPayload.SpawnData(40, 27, 42, 67, 70, 15, 12, 128,
                        289, 1.0F, 0.60F, 900L, 120L, 3L, 4L, 5L, 108L, 6L, 7L),
                new RegionDebugPayload.RegionData(2, 3, -17, 42, 31, -5, 10, 7,
                        -20, 40, 28, 0.82F, 75, 33, 0, -1, 12, 19, 0.72F,
                        321, true, 4096, 8192, false, 101L, 22L, 987_654L,
                        345_678L, 1_234_567L, 2, 8, 20, 8, 5, 12, 8, 0.34F,
                        0.62F, 0xB66CFF),
                new RegionDebugPayload.CivilizationData(1, 10, 20, 30, 40,.35F,.25F,.25F,.15F, 25,
                        18, 60, 0.07F, 0.76F, 0.0532F, 1_000L, 1_200L),
                new RegionDebugPayload.EvidenceData(512, 30, 40, 25, .75F, 35, 4, 3,
                        new int[]{1,2,3,0,1,2}, 8, 3, 2, 1, 2, 4, 8,
                        "[LOW_SAFETY]", "abcdef", 44.5F, 11_000L),
                new RegionDebugPayload.ScheduleData(17, 200, 900_000, 700_000, 2_000_000,
                        512, 3, true, 5, 1, 3, "none", false),
                new RegionDebugPayload.ActivityData(1, 42.5F, 2, 400, 0x15, 3, 2.4F, .6F,
                        12_000, 3_000, .25F, 4, 0xB, 22, 1, 2, 3, 4, 5, 6));

        assertEquals(payload, roundTrip(payload));
    }

    @Test
    void roundTripsDisabledPayloadWithoutPageBodies() {
        assertEquals(RegionDebugPayload.disabled(), roundTrip(RegionDebugPayload.disabled()));
    }

    private static RegionDebugPayload roundTrip(RegionDebugPayload payload) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            RegionDebugPayload.STREAM_CODEC.encode(buffer, payload);
            return RegionDebugPayload.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }
}
