package com.seaquake6324.civitas.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.City;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MoveCityCoreServiceTest {
    private static final UUID CITY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID LORD = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final long OLD_POS = 12L;
    private final MoveCityCoreService service = new MoveCityCoreService();

    @Test void relocatesWithinOwnedTerritory() {
        long targetChunk = ChunkCoordinate.pack(2, 3);
        City city = city(Set.of(targetChunk));
        MoveCityCoreService.Result result = service.move(request(city, LORD, 99L, targetChunk, true, true));
        assertTrue(result.success());
        assertEquals(99L, result.city().corePosition());
        assertEquals(targetChunk, result.city().coreChunk());
    }

    @Test void rejectsOutsidersOutsideTerritoryAndInvalidPlacement() {
        long owned = ChunkCoordinate.pack(2, 3);
        City city = city(Set.of(owned));
        assertEquals("civitas.core_move.not_manager", service.move(request(city, UUID.randomUUID(), 99, owned, true, true)).errorKey());
        assertEquals("civitas.core_move.outside_city", service.move(request(city, LORD, 99, ChunkCoordinate.pack(8, 8), true, true)).errorKey());
        assertEquals("civitas.core_move.blocked", service.move(request(city, LORD, 99, owned, false, true)).errorKey());
        assertEquals("civitas.core_move.no_support", service.move(request(city, LORD, 99, owned, true, false)).errorKey());
        assertFalse(service.move(request(city, LORD, OLD_POS, owned, true, true)).success());
    }

    private static MoveCityCoreService.Request request(City city, UUID player, long target, long chunk, boolean replaceable, boolean support) {
        return new MoveCityCoreService.Request(city, player, CITY_ID, city.dimension(), OLD_POS, target, chunk, replaceable, support);
    }
    private static City city(Set<Long> territory) {
        return new City(CITY_ID, "Test", 0x123456, "minecraft:overworld", OLD_POS, ChunkCoordinate.pack(0, 0),
                10, LORD, LORD, Set.of(LORD), territory);
    }
}
