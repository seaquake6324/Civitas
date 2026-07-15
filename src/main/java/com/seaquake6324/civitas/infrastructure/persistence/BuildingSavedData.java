package com.seaquake6324.civitas.infrastructure.persistence;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.seaquake6324.civitas.application.port.BuildingRepository;
import com.seaquake6324.civitas.domain.building.BuildingPurpose;
import com.seaquake6324.civitas.domain.building.BuildingFeatures;
import com.seaquake6324.civitas.domain.building.BuildingRecord;
import com.seaquake6324.civitas.domain.building.BuildingStatus;
import com.seaquake6324.civitas.domain.building.BuildingRequirement;
import com.seaquake6324.civitas.domain.civilization.FacilityCategory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

/** Independent schema root; buildings never become part of layered civilization data. */
public final class BuildingSavedData extends SavedData implements BuildingRepository {
    public static final int SCHEMA_VERSION = 3;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Codec<BuildingSavedData> CODEC = CompoundTag.CODEC.xmap(BuildingSavedData::load, BuildingSavedData::saveTag);
    private static final SavedDataType<BuildingSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("civitas", "buildings"), BuildingSavedData::new, CODEC);
    private final Map<UUID, BuildingRecord> records = new HashMap<>();
    private final Map<String, Map<Long, Set<UUID>>> cellIndex = new HashMap<>();
    private final Map<String,Map<Long,java.util.LinkedHashSet<UUID>>> chunkIndex=new HashMap<>();
    private final Map<UUID, java.util.LinkedHashSet<UUID>> cityIndex = new HashMap<>();
    private final java.util.EnumMap<BuildingStatus,java.util.LinkedHashSet<UUID>> statusIndex=new java.util.EnumMap<>(BuildingStatus.class);
    private boolean readOnly;
    private CompoundTag protectedRoot;
    private int quarantinedRecords;
    private String migrationResult = "created_v3";

    public static BuildingSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    @Override public Collection<BuildingRecord> forCity(UUID cityId) {
        return cityIndex.getOrDefault(cityId, new java.util.LinkedHashSet<>()).stream().map(records::get).filter(java.util.Objects::nonNull).toList();
    }
    public List<BuildingRecord> forCity(UUID cityId,int limit){return cityPage(cityId,limit).records();}
    public CityPage cityPage(UUID cityId,int limit){int cap=Math.max(0,limit),examined=0;ArrayList<BuildingRecord>out=new ArrayList<>(Math.min(cap,64));boolean truncated=false;for(UUID id:cityIndex.getOrDefault(cityId,new java.util.LinkedHashSet<>())){if(examined++>=cap){truncated=true;break;}BuildingRecord record=records.get(id);if(record!=null)out.add(record);}return new CityPage(List.copyOf(out),truncated,examined);}
    public record CityPage(List<BuildingRecord> records,boolean truncated,int examined){public CityPage{records=List.copyOf(records);}}
    public ChunkPage recordsInChunk(UUID cityId,String dimension,long chunk,int limit){int cap=Math.max(0,limit),examined=0;ArrayList<BuildingRecord>out=new ArrayList<>();boolean truncated=false;for(UUID id:chunkIndex.getOrDefault(dimension,Map.of()).getOrDefault(chunk,new java.util.LinkedHashSet<>())){BuildingRecord record=records.get(id);if(record==null||!record.cityId().equals(cityId))continue;if(examined++>=cap){truncated=true;break;}out.add(record);}return new ChunkPage(List.copyOf(out),truncated,examined);}
    public record ChunkPage(List<BuildingRecord>records,boolean truncated,int examined){public ChunkPage{records=List.copyOf(records);}}
    @Override public boolean overlaps(UUID cityId,String dimension,Set<Long>cells,UUID excludedId){Map<Long,Set<UUID>>index=cellIndex.getOrDefault(dimension,Map.of());for(long cell:cells)for(UUID id:index.getOrDefault(cell,Set.of())){BuildingRecord record=records.get(id);if(record!=null&&record.cityId().equals(cityId)&&(excludedId==null||!excludedId.equals(id)))return true;}return false;}
    @Override public Optional<BuildingRecord> byId(UUID id) { return Optional.ofNullable(records.get(id)); }
    @Override public void put(BuildingRecord building) {
        if (readOnly) return;
        BuildingRecord previous = records.get(building.id());
        if(previous!=null&&building.revision()<=previous.revision())throw new IllegalArgumentException("building revision must increase");
        records.put(building.id(), building);
        if (previous != null) unindex(previous);
        index(building); setDirty();
    }
    public Collection<BuildingRecord> records() { return List.copyOf(records.values()); }
    public List<BuildingRecord> recordsByStatus(BuildingStatus status,int limit){ArrayList<BuildingRecord>out=new ArrayList<>();for(UUID id:statusIndex.getOrDefault(status,new java.util.LinkedHashSet<>())){if(out.size()>=Math.max(0,limit))break;BuildingRecord record=records.get(id);if(record!=null)out.add(record);}return List.copyOf(out);}
    public boolean readOnly() { return readOnly; }
    public int quarantinedRecords() { return quarantinedRecords; }
    public String migrationResult() { return migrationResult; }

    public int markStaleAt(String dimension, BlockPos changed) {
        return markStaleRecordsAt(dimension, changed).size();
    }

    public Set<UUID> markStaleRecordsAt(String dimension, BlockPos changed) {
        if (readOnly) return Set.of();
        Set<Long> affected = java.util.stream.Stream.concat(java.util.stream.Stream.of(changed),
                java.util.stream.Stream.of(net.minecraft.core.Direction.values()).map(changed::relative))
                .map(BlockPos::asLong).collect(Collectors.toSet());
        Set<UUID> ids = new java.util.HashSet<>();
        Map<Long, Set<UUID>> dimensionIndex = cellIndex.getOrDefault(dimension, Map.of());
        affected.forEach(position -> ids.addAll(dimensionIndex.getOrDefault(position, Set.of())));
        Set<UUID> changedIds = new java.util.HashSet<>();
        for (UUID id : ids) {
            BuildingRecord record = records.get(id);
            if (record == null || record.status() == BuildingStatus.STALE) continue;
            put(record.stale("block_changed")); changedIds.add(id);
        }
        if (!changedIds.isEmpty()) setDirty();
        return Set.copyOf(changedIds);
    }

    public void removeCity(UUID cityId) {
        if (readOnly) return;
        List<BuildingRecord> removed = List.copyOf(forCity(cityId));
        removed.forEach(record -> { records.remove(record.id()); unindex(record); });
        if (!removed.isEmpty()) setDirty();
    }

    CompoundTag saveTag() {
        if (readOnly && protectedRoot != null) return protectedRoot.copy();
        CompoundTag root = new CompoundTag();
        root.putInt("SchemaVersion", SCHEMA_VERSION);
        root.putString("MigrationResult", migrationResult);
        root.putInt("QuarantinedRecords", quarantinedRecords);
        ListTag list = new ListTag();
        for (BuildingRecord record : records.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Id", record.id().toString());
            tag.putString("CityId", record.cityId().toString());
            tag.putString("Dimension", record.dimension());
            tag.putString("Purpose", record.purpose().name());
            tag.putLong("Entrance", record.entrance());
            tag.putLong("Interior", record.interior());
            tag.putLongArray("Cells", record.cells().stream().mapToLong(Long::longValue).toArray());
            ListTag facilities = new ListTag();
            record.facilities().forEach((category, count) -> {
                CompoundTag facility = new CompoundTag();
                facility.putString("Category", category.name());
                facility.putInt("Count", count);
                facilities.add(facility);
            });
            tag.put("Facilities", facilities);
            tag.putLongArray("BoundaryPorts", record.features().boundaryPorts().stream().mapToLong(Long::longValue).toArray());
            tag.putLongArray("Workstations", record.features().workstations().stream().mapToLong(Long::longValue).toArray());
            tag.putLongArray("StorageEndpoints", record.features().storageEndpoints().stream().mapToLong(Long::longValue).toArray());
            tag.putLongArray("AuthorizedStorageEndpoints", record.authorizedStorageEndpoints().stream().mapToLong(Long::longValue).toArray());
            tag.putBoolean("EntranceConnected", record.features().entranceConnected());
            tag.putInt("Capacity", record.capacity());
            tag.putString("Status", record.status().name());
            tag.putLong("Revision", record.revision());
            tag.putLong("ValidatedAt", record.validatedAt());
            tag.putString("InvalidReason", record.invalidReason());
            list.add(tag);
        }
        root.put("Buildings", list);
        return root;
    }

    static BuildingSavedData load(CompoundTag root) {
        BuildingSavedData data = new BuildingSavedData();
        int schema = root.getIntOr("SchemaVersion", 0);
        if (schema > SCHEMA_VERSION) {
            data.readOnly = true;
            data.protectedRoot = root.copy();
            data.migrationResult = "future_schema_read_only";
            LOGGER.warn("Civitas building schema {} is newer than {}; loading read-only", schema, SCHEMA_VERSION);
            return data;
        } else if (schema == 0) {
            data.migrationResult = "v0_to_v3";
        } else if (schema == 1) {
            data.migrationResult = "v1_to_v3";
        } else if (schema == 2) {
            data.migrationResult = "v2_to_v3";
        } else {
            data.migrationResult = root.getStringOr("MigrationResult", "v1");
        }
        data.quarantinedRecords = Math.max(0, root.getIntOr("QuarantinedRecords", 0));
        for (Tag raw : root.getListOrEmpty("Buildings")) {
            if (!(raw instanceof CompoundTag tag)) { data.quarantine("expected compound", null); continue; }
            try {
                UUID id = UUID.fromString(tag.getStringOr("Id", ""));
                UUID cityId = UUID.fromString(tag.getStringOr("CityId", ""));
                String dimension = tag.getStringOr("Dimension", "");
                if (!dimension.contains(":")) throw new IllegalArgumentException("invalid dimension");
                BuildingPurpose purpose = BuildingPurpose.valueOf(tag.getStringOr("Purpose", ""));
                Set<Long> cells = java.util.Arrays.stream(tag.getLongArray("Cells").orElseThrow()).boxed().collect(Collectors.toSet());
                EnumMap<FacilityCategory, Integer> facilities = new EnumMap<>(FacilityCategory.class);
                for (Tag facilityRaw : tag.getListOrEmpty("Facilities")) {
                    if (!(facilityRaw instanceof CompoundTag facility)) continue;
                    FacilityCategory category = FacilityCategory.valueOf(facility.getStringOr("Category", ""));
                    facilities.put(category, Math.max(0, facility.getIntOr("Count", 0)));
                }
                BuildingStatus status = schema < 2 ? BuildingStatus.STALE
                        : enumValue(BuildingStatus.class, tag.getStringOr("Status", "VALID"), BuildingStatus.STALE);
                long entrance = tag.getLongOr("Entrance", 0);
                BuildingFeatures features;
                if (schema >= 2) {
                    features = new BuildingFeatures(longSet(tag, "BoundaryPorts"), longSet(tag, "Workstations"),
                            longSet(tag, "StorageEndpoints"), tag.getBooleanOr("EntranceConnected", false));
                } else {
                    features = BuildingFeatures.EMPTY;
                }
                BuildingRecord record = new BuildingRecord(id, cityId, dimension, purpose,
                        entrance, tag.getLongOr("Interior", 0), cells, facilities, features,
                        Math.max(0, tag.getIntOr("Capacity", 0)), status,
                        Math.max(0, tag.getLongOr("Revision", 0)), tag.getLongOr("ValidatedAt", 0),
                        schema < 2 ? "migration_requires_revalidation" : tag.getStringOr("InvalidReason", ""),
                        schema >= 3 ? longSet(tag, "AuthorizedStorageEndpoints") : Set.of());
                if (data.records.containsKey(id)) { data.quarantine("duplicate building id", null); continue; }
                if (schema >= 2 && !validStoredEvidence(record)) { data.quarantine("inconsistent building evidence", null); continue; }
                if (data.hasIndexedCellConflict(record)) { data.quarantine("overlapping building cells", null); continue; }
                data.records.put(id, record);
                data.index(record);
            } catch (RuntimeException exception) {
                data.quarantine("malformed building", exception);
            }
        }
        return data;
    }

    private void quarantine(String reason, RuntimeException exception) {
        quarantinedRecords++;
        if (exception == null) LOGGER.warn("Skipping Civitas building record: {}", reason);
        else LOGGER.warn("Skipping Civitas building record: {}", reason, exception);
    }
    private void index(BuildingRecord record) {
        cityIndex.computeIfAbsent(record.cityId(), ignored -> new java.util.LinkedHashSet<>()).add(record.id());
        statusIndex.computeIfAbsent(record.status(),ignored->new java.util.LinkedHashSet<>()).add(record.id());
        Map<Long, Set<UUID>> index = cellIndex.computeIfAbsent(record.dimension(), ignored -> new HashMap<>());
        indexedPositions(record).forEach(position -> index.computeIfAbsent(position, ignored -> new java.util.HashSet<>()).add(record.id()));
        Map<Long,java.util.LinkedHashSet<UUID>>chunks=chunkIndex.computeIfAbsent(record.dimension(),ignored->new HashMap<>());indexedChunks(record).forEach(chunk->chunks.computeIfAbsent(chunk,ignored->new java.util.LinkedHashSet<>()).add(record.id()));
    }
    private void unindex(BuildingRecord record) {
        Set<UUID> cityIds=cityIndex.get(record.cityId());if(cityIds!=null&&cityIds.remove(record.id())&&cityIds.isEmpty())cityIndex.remove(record.cityId());
        Set<UUID> statusIds=statusIndex.get(record.status());if(statusIds!=null&&statusIds.remove(record.id())&&statusIds.isEmpty())statusIndex.remove(record.status());
        Map<Long, Set<UUID>> index = cellIndex.get(record.dimension());
        if (index == null) return;
        indexedPositions(record).forEach(position -> {
            Set<UUID> ids = index.get(position);
            if (ids != null && ids.remove(record.id()) && ids.isEmpty()) index.remove(position);
        });
        if (index.isEmpty()) cellIndex.remove(record.dimension());
        Map<Long,java.util.LinkedHashSet<UUID>>chunks=chunkIndex.get(record.dimension());if(chunks!=null){indexedChunks(record).forEach(chunk->{Set<UUID>ids=chunks.get(chunk);if(ids!=null&&ids.remove(record.id())&&ids.isEmpty())chunks.remove(chunk);});if(chunks.isEmpty())chunkIndex.remove(record.dimension());}
    }
    private static java.util.stream.Stream<Long> indexedPositions(BuildingRecord record) {
        return java.util.stream.Stream.of(java.util.stream.Stream.of(record.entrance()), record.cells().stream(),
                record.features().boundaryPorts().stream(), record.features().workstations().stream(),
                record.features().storageEndpoints().stream()).flatMap(java.util.function.Function.identity()).distinct();
    }
    private static java.util.stream.Stream<Long> indexedChunks(BuildingRecord record){return indexedPositions(record).map(BlockPos::of).map(pos->ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4)).distinct();}
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        try { return Enum.valueOf(type, value); } catch (IllegalArgumentException exception) { return fallback; }
    }
    private static Set<Long> longSet(CompoundTag tag, String key) {
        return java.util.Arrays.stream(tag.getLongArray(key).orElse(new long[0])).boxed().collect(Collectors.toUnmodifiableSet());
    }
    private boolean hasIndexedCellConflict(BuildingRecord record){Map<Long,Set<UUID>>index=cellIndex.getOrDefault(record.dimension(),Map.of());return record.cells().stream().anyMatch(cell->!index.getOrDefault(cell,Set.of()).isEmpty());}
    private static boolean validStoredEvidence(BuildingRecord record){
        if(record.status()==BuildingStatus.INVALID)return record.capacity()==0;
        if(record.status()!=BuildingStatus.VALID)return true;
        BuildingRequirement requirement=BuildingRequirement.forPurpose(record.purpose());int facilities=record.facilities().getOrDefault(requirement.facility(),0);int expected=requirement.explainCapacity(facilities,record.cells().size()).effectiveCapacity();
        return record.features().entranceConnected()&&record.features().boundaryPorts().contains(record.entrance())
                &&record.facilities().getOrDefault(FacilityCategory.PRODUCTION,0)==record.features().workstations().size()
                &&record.facilities().getOrDefault(FacilityCategory.STORAGE,0)==record.features().storageEndpoints().size()
                &&facilities>=requirement.minimumFacilities()&&record.capacity()==expected;
    }
}
