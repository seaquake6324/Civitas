package com.seaquake6324.civitas.presentation.client.map;

import com.seaquake6324.civitas.infrastructure.network.CityMapRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class ClientCityMapStore {
    private static volatile Snapshot snapshot = Snapshot.empty();

    public static synchronized void replace(List<CityMapRecord> cities) { snapshot = Snapshot.build(cities, snapshot.version + 1); }

    public static synchronized void upsert(CityMapRecord city) {
        List<CityMapRecord> cities = new ArrayList<>(snapshot.cities.values());
        cities.removeIf(existing -> existing.id().equals(city.id()));
        cities.add(city);
        snapshot = Snapshot.build(cities, snapshot.version + 1);
    }

    public static synchronized void remove(UUID cityId) {
        List<CityMapRecord> cities = new ArrayList<>(snapshot.cities.values());
        if (cities.removeIf(city -> city.id().equals(cityId))) snapshot = Snapshot.build(cities, snapshot.version + 1);
    }

    public static synchronized void clear() { snapshot = Snapshot.empty(); }

    public static Optional<CityMapRecord> byId(UUID cityId) { return Optional.ofNullable(snapshot.cities.get(cityId)); }

    public static Optional<CityMapRecord> cityAt(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return Optional.ofNullable(snapshot.byDimensionChunk.getOrDefault(dimension.identifier().toString(), Map.of()).get(ChunkPos.pack(chunkX, chunkZ)));
    }

    public static boolean regionHasCities(ResourceKey<Level> dimension, int regionX, int regionZ) {
        Map<Long, CityMapRecord> chunks = snapshot.byDimensionChunk.get(dimension.identifier().toString());
        if (chunks == null) return false;
        int minX = regionX << 5;
        int minZ = regionZ << 5;
        for (long packed : chunks.keySet()) {
            int x = ChunkPos.getX(packed);
            int z = ChunkPos.getZ(packed);
            if (x >= minX && x < minX + 32 && z >= minZ && z < minZ + 32) return true;
        }
        return false;
    }

    public static int regionHash(ResourceKey<Level> dimension, int regionX, int regionZ) {
        int hash = 31 * snapshot.version + dimension.identifier().hashCode();
        int minX = (regionX << 5) - 1;
        int minZ = (regionZ << 5) - 1;
        Map<Long, CityMapRecord> chunks = snapshot.byDimensionChunk.getOrDefault(dimension.identifier().toString(), Map.of());
        for (int z = minZ; z <= minZ + 33; z++) for (int x = minX; x <= minX + 33; x++) {
            CityMapRecord city = chunks.get(ChunkPos.pack(x, z));
            if (city != null) hash = 31 * hash + city.id().hashCode() + city.color();
        }
        return hash;
    }

    private record Snapshot(Map<UUID, CityMapRecord> cities, Map<String, Map<Long, CityMapRecord>> byDimensionChunk, int version) {
        private static Snapshot empty() { return new Snapshot(Map.of(), Map.of(), 0); }
        private static Snapshot build(List<CityMapRecord> records, int version) {
            Map<UUID, CityMapRecord> cities = new HashMap<>();
            Map<String, Map<Long, CityMapRecord>> index = new HashMap<>();
            for (CityMapRecord city : records) {
                cities.put(city.id(), city);
                Map<Long, CityMapRecord> dimension = index.computeIfAbsent(city.dimension(), ignored -> new HashMap<>());
                for (long chunk : city.territory()) dimension.put(chunk, city);
            }
            Map<String, Map<Long, CityMapRecord>> frozen = new HashMap<>();
            index.forEach((dimension, chunks) -> frozen.put(dimension, Map.copyOf(chunks)));
            return new Snapshot(Map.copyOf(cities), Map.copyOf(frozen), version);
        }
    }

    private ClientCityMapStore() {}
}
