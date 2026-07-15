package com.seaquake6324.civitas.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.civilization.CivilizationLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import com.seaquake6324.civitas.domain.civilization.*;
import java.util.List;

class CivilizationSavedDataTest {
    @Test
    void writesSchemaV3AndClearsVersionZeroRootsForRescan() {
        assertEquals(3, new CivilizationSavedData().saveTag().getIntOr("SchemaVersion", -1));

        CompoundTag legacyRoot = new CompoundTag();
        ListTag chunks = new ListTag();
        chunks.add(record("SURFACE", 31, 22));
        legacyRoot.put("Chunks", chunks);

        CivilizationSavedData migrated = CivilizationSavedData.load(legacyRoot);
        assertEquals(0, migrated
                .get("minecraft:overworld", ChunkCoordinate.pack(-3, 7), CivilizationLayer.SURFACE).civility());
        assertEquals("cleared-v0-records-for-v2-rescan", migrated.migrationResult());
    }

    @Test
    void clearsVersionOneCivilizationWithoutTouchingTheRootContract() {
        CompoundTag legacy = new CompoundTag(); legacy.putInt("SchemaVersion", 1);
        ListTag chunks = new ListTag(); chunks.add(record("SURFACE", 88, 44)); legacy.put("Chunks", chunks);
        CivilizationSavedData migrated = CivilizationSavedData.load(legacy);
        assertEquals(0, migrated.size());
        assertEquals("cleared-v1-records-for-v2-rescan", migrated.migrationResult());
        assertEquals(3, migrated.saveTag().getIntOr("SchemaVersion", -1));
    }

    @Test
    void loadsIndependentSurfaceAndUndergroundValuesForTheSameChunk() {
        CompoundTag root = new CompoundTag();
        root.putInt("SchemaVersion", 2);
        ListTag chunks = new ListTag();
        chunks.add(record("SURFACE", 72, 65));
        chunks.add(record("UNDERGROUND", 24, 18));
        root.put("Chunks", chunks);

        CivilizationSavedData data = CivilizationSavedData.load(root);
        long chunk = ChunkCoordinate.pack(-3, 7);

        assertEquals(72, data.get("minecraft:overworld", chunk, CivilizationLayer.SURFACE).civility());
        assertEquals(65, data.get("minecraft:overworld", chunk, CivilizationLayer.SURFACE).activity());
        assertEquals(24, data.get("minecraft:overworld", chunk, CivilizationLayer.UNDERGROUND).civility());
        assertEquals(18, data.get("minecraft:overworld", chunk, CivilizationLayer.UNDERGROUND).activity());
        assertEquals("migrated-v2-to-v3-preserved", data.migrationResult());
    }

    @Test
    void preservesUnknownFutureSchemaReadOnly() {
        CompoundTag root = new CompoundTag();
        root.putInt("SchemaVersion", 99);
        root.putString("FutureData", "keep-me");
        CivilizationSavedData data = CivilizationSavedData.load(root);
        assertEquals(true, data.readOnly());
        data.put("minecraft:overworld", 1, CivilizationLayer.SURFACE,
                new com.seaquake6324.civitas.domain.civilization.ChunkCivilization(
                        com.seaquake6324.civitas.domain.civilization.CivilizationFactors.empty(),0,1,0,0,0));
        assertEquals("keep-me", data.saveTag().getStringOr("FutureData", ""));
    }

    @Test
    void roundTripsCandidateEvidenceAndStableStart() {
        CivilityEvidence evidence = new CivilityEvidence(100,50,50,40,.8,45,3,2,
                List.of(1,2,3,0,0,1),7,2,1,true,true,new BoundaryPorts(1,2,4,8));
        CivilityScore score = CivilityScoringRules.score(evidence);
        CivilityCandidate candidate = new CivilityCandidate("fingerprint", evidence, score, 500, 700);
        ChunkCivilization state = new ChunkCivilization(score.factors(), score.target(), 12, 0, 500, 600,
                evidence, score, candidate, evidence.ports(), 700,
                new ActivitySummary(800, 900, new long[]{1,2,3,4,5}, 7, 2, .5, 3, 1));
        CivilizationSavedData original = new CivilizationSavedData();
        original.put("minecraft:overworld", 42, CivilizationLayer.UNDERGROUND, state);
        ChunkCivilization loaded = CivilizationSavedData.load(original.saveTag())
                .get("minecraft:overworld", 42, CivilizationLayer.UNDERGROUND);
        assertEquals("fingerprint", loaded.candidate().fingerprint());
        assertEquals(500, loaded.candidate().firstSeen());
        assertEquals(evidence, loaded.evidence());
        assertEquals(new BoundaryPorts(1,2,4,8), loaded.boundaryPorts());
        assertEquals(800, loaded.activitySummary().lastActivityTime());
        assertEquals(7, loaded.activitySummary().lastCategoryMask());
        assertEquals(5, loaded.activitySummary().categoryLastSeen()[4]);
    }

    @Test
    void isolatesMalformedRecordsAndLoadsOtherV2Records() {
        CompoundTag root = new CompoundTag(); root.putInt("SchemaVersion", 2);
        ListTag chunks = new ListTag();
        CompoundTag malformed = record("NOT_A_LAYER", 99, 99);
        chunks.add(malformed); chunks.add(record("SURFACE", 31, 22)); root.put("Chunks", chunks);
        CivilizationSavedData data = CivilizationSavedData.load(root);
        assertEquals(31, data.get("minecraft:overworld", ChunkCoordinate.pack(-3, 7), CivilizationLayer.SURFACE).civility());
        assertEquals(1, data.size());
    }

    @Test
    void malformedActivitySummaryFallsBackWithoutDroppingCivilizationRecord() {
        CompoundTag root = new CompoundTag(); root.putInt("SchemaVersion", 3);
        CompoundTag valid = record("SURFACE", 31, 22);
        CompoundTag summary = new CompoundTag(); summary.putLongArray("CategoryLastSeen", new long[]{1,2});
        valid.put("ActivitySummary", summary);
        ListTag chunks = new ListTag(); chunks.add(valid); root.put("Chunks", chunks);
        ChunkCivilization loaded = CivilizationSavedData.load(root)
                .get("minecraft:overworld", ChunkCoordinate.pack(-3, 7), CivilizationLayer.SURFACE);
        assertEquals(31, loaded.civility());
        assertEquals(ActivitySummary.empty(), loaded.activitySummary());
    }

    private static CompoundTag record(String layer, double civility, double activity) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", "minecraft:overworld");
        tag.putLong("Chunk", ChunkCoordinate.pack(-3, 7));
        tag.putString("Layer", layer);
        tag.putDouble("Civility", civility);
        tag.putDouble("Activity", activity);
        return tag;
    }
}
