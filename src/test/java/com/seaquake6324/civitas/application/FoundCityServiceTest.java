package com.seaquake6324.civitas.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seaquake6324.civitas.application.port.CityRepository;
import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.CityName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FoundCityServiceTest {
    private static final UUID PLAYER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final FoundCityService service = new FoundCityService();

    @Test
    void foundsCityWithNormalizedNameAndNineChunkTerritory() {
        Repository repository = new Repository();

        FoundCityService.Result result = service.found(repository, request("  New City  "));

        assertTrue(result.success());
        assertEquals("New City", result.city().name());
        assertEquals(9, result.city().territory().size());
        assertEquals(new ChunkCoordinate(10, -4), ChunkCoordinate.unpack(result.city().coreChunk()));
        assertEquals(List.of(result.city()), repository.cities);
    }

    @Test
    void rejectsInvalidCoreAndOwnershipState() {
        Repository repository = new Repository();
        FoundCityService.Request base = request("New City");

        assertEquals("civitas.error.core_missing", service.found(repository, copy(base, false, false, PLAYER, 0)).errorKey());
        assertEquals("civitas.error.core_activated", service.found(repository, copy(base, true, true, PLAYER, 0)).errorKey());
        assertEquals("civitas.error.not_core_owner", service.found(repository,
                copy(base, true, false, UUID.fromString("22222222-2222-2222-2222-222222222222"), 0)).errorKey());
        assertEquals("civitas.error.too_far", service.found(repository, copy(base, true, false, PLAYER, 65)).errorKey());
        assertTrue(repository.cities.isEmpty());
    }

    @Test
    void rejectsDuplicateLeadershipNameAndDisabledDimension() {
        Repository repository = new Repository();
        repository.add(city(PLAYER, "Existing", new ChunkCoordinate(100, 100), Set.of(ChunkCoordinate.pack(100, 100))));
        assertEquals("civitas.error.already_leads_city", service.found(repository, request("New City")).errorKey());

        repository.cities.clear();
        repository.add(city(UUID.randomUUID(), "New City", new ChunkCoordinate(100, 100), Set.of(ChunkCoordinate.pack(100, 100))));
        assertEquals("civitas.error.name_taken", service.found(repository, request("new city")).errorKey());

        repository.cities.clear();
        FoundCityService.Request base = request("New City");
        FoundCityService.Request disabled = new FoundCityService.Request(base.playerId(), base.dimension(), base.corePosition(),
                base.coreChunk(), true, false, PLAYER, 0, base.requestedName(), base.requestedColor(), base.gameTime(),
                false, base.minimumCoreDistanceChunks(), base.borderBufferChunks());
        assertEquals("civitas.error.dimension_disabled", service.found(repository, disabled).errorKey());
    }

    @Test
    void rejectsNearbyCoreAndBufferedTerritoryConflict() {
        Repository repository = new Repository();
        repository.add(city(UUID.randomUUID(), "Nearby", new ChunkCoordinate(20, -4), Set.of(ChunkCoordinate.pack(20, -4))));
        assertEquals("civitas.error.core_too_close", service.found(repository, request("New City")).errorKey());

        repository.cities.clear();
        repository.add(city(UUID.randomUUID(), "Border", new ChunkCoordinate(100, 100), Set.of(ChunkCoordinate.pack(12, -4))));
        assertEquals("civitas.error.territory_conflict", service.found(repository, request("New City")).errorKey());
    }

    @Test
    void chunkPackingRoundTripsNegativeCoordinates() {
        ChunkCoordinate coordinate = new ChunkCoordinate(-123456, 7654321);
        assertEquals(coordinate, ChunkCoordinate.unpack(coordinate.packed()));
        assertEquals(7, new ChunkCoordinate(-2, 4).chebyshevDistance(new ChunkCoordinate(5, 0)));
    }

    private static FoundCityService.Request request(String name) {
        return new FoundCityService.Request(PLAYER, "minecraft:overworld", 123L, new ChunkCoordinate(10, -4),
                true, false, PLAYER, 0, name, 0x123456, 200L, true, 16, 4);
    }

    private static FoundCityService.Request copy(FoundCityService.Request base, boolean present, boolean activated,
                                                   UUID placer, double distance) {
        return new FoundCityService.Request(base.playerId(), base.dimension(), base.corePosition(), base.coreChunk(),
                present, activated, placer, distance, base.requestedName(), base.requestedColor(), base.gameTime(),
                base.dimensionAllowed(), base.minimumCoreDistanceChunks(), base.borderBufferChunks());
    }

    private static City city(UUID leader, String name, ChunkCoordinate core, Set<Long> territory) {
        return new City(UUID.randomUUID(), name, 0, "minecraft:overworld", 0L, core.packed(), 0L,
                leader, leader, Set.of(leader), territory);
    }

    private static final class Repository implements CityRepository {
        private final List<City> cities = new ArrayList<>();
        @Override public Collection<City> cities() { return cities; }
        @Override public Optional<City> byName(String name) {
            String key = CityName.uniquenessKey(name);
            return cities.stream().filter(city -> CityName.uniquenessKey(city.name()).equals(key)).findFirst();
        }
        @Override public Optional<City> cityLedBy(UUID playerId) {
            return cities.stream().filter(city -> city.founderId().equals(playerId) || city.lordId().equals(playerId)).findFirst();
        }
        @Override public void add(City city) { cities.add(city); }
    }
}
