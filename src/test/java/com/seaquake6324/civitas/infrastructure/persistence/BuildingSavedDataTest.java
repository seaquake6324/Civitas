package com.seaquake6324.civitas.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;
import com.seaquake6324.civitas.domain.building.*;
import com.seaquake6324.civitas.domain.civilization.FacilityCategory;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class BuildingSavedDataTest {
    @Test void roundTripsExplicitSchemaAndMigratesVersionZeroRoot() {
        BuildingSavedData data = new BuildingSavedData();
        BuildingRecord record = record(new BlockPos(4,64,4));
        data.put(record);
        CompoundTag root = data.saveTag();
        assertEquals(3, root.getIntOr("SchemaVersion", -1));
        root.remove("SchemaVersion");
        BuildingSavedData loaded = BuildingSavedData.load(root);
        assertTrue(loaded.byId(record.id()).isPresent());
        assertEquals("v0_to_v3", loaded.migrationResult());
    }

    @Test void migratesV1EndpointsWithoutInventingUnknownPositions() {
        BuildingSavedData data = new BuildingSavedData();
        BuildingRecord record = record(new BlockPos(4,64,4)); data.put(record);
        CompoundTag root = data.saveTag(); root.putInt("SchemaVersion", 1);
        CompoundTag tag = (CompoundTag)root.getListOrEmpty("Buildings").getFirst();
        tag.remove("BoundaryPorts"); tag.remove("Workstations"); tag.remove("StorageEndpoints"); tag.remove("EntranceConnected");
        BuildingSavedData loaded = BuildingSavedData.load(root);
        BuildingRecord migrated = loaded.byId(record.id()).orElseThrow();
        assertEquals("v1_to_v3", loaded.migrationResult());
        assertEquals(BuildingStatus.STALE, migrated.status());
        assertEquals("migration_requires_revalidation", migrated.invalidReason());
        assertTrue(migrated.features().boundaryPorts().isEmpty());
        assertTrue(migrated.features().workstations().isEmpty());
        assertTrue(migrated.features().storageEndpoints().isEmpty());
    }

    @Test void roundTripsFeatureEndpointsAndQuarantinesDuplicateId() {
        BuildingSavedData data = new BuildingSavedData();
        BlockPos cell = new BlockPos(7,64,7);
        BuildingRecord record = new BuildingRecord(UUID.randomUUID(), UUID.randomUUID(), "minecraft:overworld",
                BuildingPurpose.WORKSHOP, cell.west().asLong(), cell.asLong(), Set.of(cell.asLong()),
                Map.of(FacilityCategory.PRODUCTION, 1,FacilityCategory.STORAGE,1),
                new BuildingFeatures(Set.of(cell.west().asLong()), Set.of(cell.east().asLong()),
                        Set.of(cell.north().asLong()), true), 1, BuildingStatus.VALID, 1, 10, "");
        data.put(record); CompoundTag root = data.saveTag();
        root.getListOrEmpty("Buildings").add(((CompoundTag)root.getListOrEmpty("Buildings").getFirst()).copy());
        BuildingSavedData loaded = BuildingSavedData.load(root);
        assertEquals(record.features(), loaded.byId(record.id()).orElseThrow().features());
        assertEquals(1, loaded.records().size());
        assertEquals(1, loaded.quarantinedRecords());
    }

    @Test void migratesV2WithNoInventedAuthorizationAndRoundTripsAuthorizedEndpoint() {
        BlockPos cell = new BlockPos(9,64,9);
        long storage = cell.north().asLong();
        BuildingRecord base = new BuildingRecord(UUID.randomUUID(), UUID.randomUUID(), "minecraft:overworld",
                BuildingPurpose.WORKSHOP, cell.west().asLong(), cell.asLong(), Set.of(cell.asLong()),
                Map.of(FacilityCategory.PRODUCTION,1, FacilityCategory.STORAGE,1),
                new BuildingFeatures(Set.of(cell.west().asLong()),Set.of(cell.east().asLong()),Set.of(storage),true),
                1,BuildingStatus.VALID,1,10,"",Set.of(storage));
        BuildingSavedData data=new BuildingSavedData();data.put(base);CompoundTag v2=data.saveTag();v2.putInt("SchemaVersion",2);
        ((CompoundTag)v2.getListOrEmpty("Buildings").getFirst()).remove("AuthorizedStorageEndpoints");
        BuildingSavedData migrated=BuildingSavedData.load(v2);
        assertEquals("v2_to_v3",migrated.migrationResult());assertTrue(migrated.byId(base.id()).orElseThrow().authorizedStorageEndpoints().isEmpty());
        BuildingSavedData roundTrip=BuildingSavedData.load(data.saveTag());
        assertEquals(Set.of(storage),roundTrip.byId(base.id()).orElseThrow().authorizedStorageEndpoints());
    }

    @Test void quarantinesMalformedRecordWithoutDroppingValidNeighbor() {
        BuildingSavedData data = new BuildingSavedData();
        BuildingRecord valid = record(new BlockPos(1,64,1)); data.put(valid);
        CompoundTag root = data.saveTag();
        CompoundTag malformed = new CompoundTag(); malformed.putString("Id", "bad");
        root.getListOrEmpty("Buildings").add(malformed);
        BuildingSavedData loaded = BuildingSavedData.load(root);
        assertEquals(1, loaded.records().size());
        assertEquals(1, loaded.quarantinedRecords());
    }

    @Test void protectsFutureSchemaAndPreservesOriginalRoot() {
        BuildingSavedData seed=new BuildingSavedData();seed.put(record(new BlockPos(3,64,3)));CompoundTag root=seed.saveTag();root.putInt("SchemaVersion",99);root.putString("FutureField","preserve-me");
        BuildingSavedData loaded = BuildingSavedData.load(root);
        loaded.put(record(new BlockPos(2,64,2)));
        assertTrue(loaded.readOnly());
        assertEquals(99, loaded.saveTag().getIntOr("SchemaVersion", 0));
        assertEquals("preserve-me",loaded.saveTag().getStringOr("FutureField",""));
        assertTrue(loaded.records().isEmpty());
    }

    @Test void localBlockChangeMarksOnlyIntersectingRecordStale() {
        BuildingSavedData data = new BuildingSavedData();
        BuildingRecord touched = record(new BlockPos(8,64,8));
        BuildingRecord distant = record(new BlockPos(40,64,40));
        data.put(touched); data.put(distant);
        assertEquals(1, data.markStaleAt("minecraft:overworld", new BlockPos(8,64,8)));
        assertEquals(BuildingStatus.STALE, data.byId(touched.id()).orElseThrow().status());
        assertEquals(java.util.List.of(touched.id()),data.recordsByStatus(BuildingStatus.STALE,10).stream().map(BuildingRecord::id).toList());
        assertEquals(BuildingStatus.VALID, data.byId(distant.id()).orElseThrow().status());
    }

    @Test void replacementRequiresStrictlyIncreasingRevision(){BuildingSavedData data=new BuildingSavedData();BuildingRecord record=record(new BlockPos(5,64,5));data.put(record);assertThrows(IllegalArgumentException.class,()->data.put(record));BuildingRecord stale=record.stale("test");data.put(stale);assertEquals(record.revision()+1,data.byId(record.id()).orElseThrow().revision());assertThrows(IllegalArgumentException.class,()->data.put(record));}

    @Test void cityPageHasExactBoundAndCellIndexFindsOverlap() {
        BuildingSavedData data = new BuildingSavedData(); UUID city = UUID.randomUUID();
        BuildingRecord first = record(city,new BlockPos(1,64,1));
        BuildingRecord second = record(city,new BlockPos(2,64,2));
        data.put(first);data.put(second);
        var exact=data.cityPage(city,2);assertEquals(2,exact.records().size());assertFalse(exact.truncated());assertEquals(2,exact.examined());
        var limited=data.cityPage(city,1);assertEquals(1,limited.records().size());assertTrue(limited.truncated());assertEquals(2,limited.examined());
        assertTrue(data.overlaps(city,"minecraft:overworld",Set.of(first.cells().iterator().next()),null));
        assertFalse(data.overlaps(city,"minecraft:overworld",Set.of(first.cells().iterator().next()),first.id()));
        var chunkPage=data.recordsInChunk(city,"minecraft:overworld",ChunkPos.pack(0,0),1);assertEquals(1,chunkPage.records().size());assertTrue(chunkPage.truncated());assertEquals(2,chunkPage.examined());
    }

    @Test void quarantinesInconsistentCapacityAndOverlappingLoadedRecord() {
        BuildingSavedData data=new BuildingSavedData();BuildingRecord record=record(new BlockPos(12,64,12));data.put(record);CompoundTag badCapacity=data.saveTag();((CompoundTag)badCapacity.getListOrEmpty("Buildings").getFirst()).putInt("Capacity",99);BuildingSavedData capacityLoaded=BuildingSavedData.load(badCapacity);assertTrue(capacityLoaded.records().isEmpty());assertEquals(1,capacityLoaded.quarantinedRecords());
        CompoundTag overlap=data.saveTag();CompoundTag duplicate=((CompoundTag)overlap.getListOrEmpty("Buildings").getFirst()).copy();duplicate.putString("Id",UUID.randomUUID().toString());overlap.getListOrEmpty("Buildings").add(duplicate);BuildingSavedData overlapLoaded=BuildingSavedData.load(overlap);assertEquals(1,overlapLoaded.records().size());assertEquals(1,overlapLoaded.quarantinedRecords());
    }

    private static BuildingRecord record(BlockPos cell) {
        return record(UUID.randomUUID(),cell);
    }
    private static BuildingRecord record(UUID city,BlockPos cell) {
        return new BuildingRecord(UUID.randomUUID(), city, "minecraft:overworld",
                BuildingPurpose.WORKSHOP, cell.west().asLong(), cell.asLong(), Set.of(cell.asLong()),
                Map.of(FacilityCategory.PRODUCTION, 1),new BuildingFeatures(Set.of(cell.west().asLong()),Set.of(cell.east().asLong()),Set.of(),true), 1, BuildingStatus.VALID, 1, 10, "");
    }
}
