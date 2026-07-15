package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.CityRepository;
import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.CityName;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FoundCityService {
    public Result found(CityRepository cities, Request request) {
        if (!request.corePresent()) return Result.failure("civitas.error.core_missing");
        if (request.coreActivated()) return Result.failure("civitas.error.core_activated");
        if (!request.playerId().equals(request.corePlacerId())) return Result.failure("civitas.error.not_core_owner");
        if (request.distanceSquared() > 64.0D) return Result.failure("civitas.error.too_far");

        CityName.Validation name = CityName.validate(request.requestedName());
        if (!name.valid()) return Result.failure(name.errorKey());
        if (cities.cityLedBy(request.playerId()).isPresent()) return Result.failure("civitas.error.already_leads_city");
        if (cities.byName(name.normalized()).isPresent()) return Result.failure("civitas.error.name_taken");
        if (!request.dimensionAllowed()) return Result.failure("civitas.error.dimension_disabled");

        Set<Long> initialTerritory = initialTerritory(request.coreChunk());
        for (City existing : cities.cities()) {
            if (!existing.dimension().equals(request.dimension())) continue;
            if (request.coreChunk().chebyshevDistance(ChunkCoordinate.unpack(existing.coreChunk()))
                    < request.minimumCoreDistanceChunks()) {
                return Result.failure("civitas.error.core_too_close");
            }
            for (long candidate : initialTerritory) {
                ChunkCoordinate candidateChunk = ChunkCoordinate.unpack(candidate);
                for (long claimed : existing.territory()) {
                    if (candidateChunk.chebyshevDistance(ChunkCoordinate.unpack(claimed)) <= request.borderBufferChunks()) {
                        return Result.failure("civitas.error.territory_conflict");
                    }
                }
            }
        }

        City city = new City(UUID.randomUUID(), name.normalized(), request.requestedColor(), request.dimension(),
                request.corePosition(), request.coreChunk().packed(), request.gameTime(), request.playerId(),
                request.playerId(), Set.of(request.playerId()), initialTerritory);
        cities.add(city);
        return Result.success(city);
    }

    private static Set<Long> initialTerritory(ChunkCoordinate center) {
        Set<Long> chunks = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) chunks.add(ChunkCoordinate.pack(center.x() + dx, center.z() + dz));
        }
        return chunks;
    }

    public record Request(UUID playerId, String dimension, long corePosition, ChunkCoordinate coreChunk,
                          boolean corePresent, boolean coreActivated, UUID corePlacerId, double distanceSquared,
                          String requestedName, int requestedColor, long gameTime, boolean dimensionAllowed,
                          int minimumCoreDistanceChunks, int borderBufferChunks) {}

    public record Result(boolean success, String errorKey, City city) {
        public static Result success(City city) { return new Result(true, "", city); }
        public static Result failure(String errorKey) { return new Result(false, errorKey, null); }
    }
}
