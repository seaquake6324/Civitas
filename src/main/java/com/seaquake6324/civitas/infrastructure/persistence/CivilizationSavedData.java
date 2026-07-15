package com.seaquake6324.civitas.infrastructure.persistence;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.seaquake6324.civitas.domain.civilization.ChunkCivilization;
import com.seaquake6324.civitas.domain.civilization.CivilizationFactors;
import com.seaquake6324.civitas.domain.civilization.CivilizationLayer;
import com.seaquake6324.civitas.domain.civilization.BoundaryPorts;
import com.seaquake6324.civitas.domain.civilization.ActivitySummary;
import com.seaquake6324.civitas.domain.civilization.CivilityCandidate;
import com.seaquake6324.civitas.domain.civilization.CivilityEvidence;
import com.seaquake6324.civitas.domain.civilization.CivilityLimitReason;
import com.seaquake6324.civitas.domain.civilization.CivilityScore;
import com.seaquake6324.civitas.domain.civilization.CivilityScoringRules;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;

/** Server-authoritative, dimension-aware storage with two records per X/Z chunk. */
public final class CivilizationSavedData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SCHEMA_VERSION = 3;
    private static final Codec<CivilizationSavedData> CODEC = CompoundTag.CODEC.xmap(CivilizationSavedData::load, CivilizationSavedData::saveTag);
    private static final SavedDataType<CivilizationSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("civitas", "civilization"), CivilizationSavedData::new, CODEC);
    private final Map<Key, ChunkCivilization> states = new HashMap<>();
    private boolean readOnly;
    private CompoundTag protectedRoot;
    private String migrationResult = "none";

    public static CivilizationSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public ChunkCivilization get(String dimension, long chunk, CivilizationLayer layer) {
        return states.getOrDefault(new Key(dimension, chunk, layer), ChunkCivilization.empty());
    }

    public void put(String dimension, long chunk, CivilizationLayer layer, ChunkCivilization state) {
        if (readOnly) return;
        Key key = new Key(dimension, chunk, layer);
        if (state.equals(ChunkCivilization.empty())) states.remove(key); else states.put(key, state);
        setDirty();
    }

    public void removeChunk(String dimension, long chunk) {
        if (readOnly) return;
        boolean changed = states.keySet().removeIf(key -> key.dimension.equals(dimension) && key.chunk == chunk);
        if (changed) setDirty();
    }

    public boolean readOnly() { return readOnly; }
    public int size() { return states.size(); }
    public String migrationResult() { return migrationResult; }

    CompoundTag saveTag() {
        if (readOnly && protectedRoot != null) return protectedRoot.copy();
        CompoundTag root = new CompoundTag();
        root.putInt("SchemaVersion", SCHEMA_VERSION);
        root.putString("MigrationResult", migrationResult);
        ListTag list = new ListTag();
        states.forEach((key, state) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", key.dimension);
            tag.putLong("Chunk", key.chunk);
            tag.putString("Layer", key.layer.name());
            CivilizationFactors factors = state.factors();
            tag.putDouble("Building", factors.building());
            tag.putDouble("Facilities", factors.facilities());
            tag.putDouble("Safety", factors.safety());
            tag.putDouble("Connectivity", factors.connectivity());
            tag.putDouble("Target", state.targetCivility());
            tag.putDouble("Civility", state.civility());
            tag.putDouble("Activity", state.activity());
            tag.putLong("StableSince", state.stableSince());
            tag.putLong("LastEvaluated", state.lastEvaluated());
            tag.putLong("LastScanned", state.lastScanned());
            tag.put("ActivitySummary", writeActivitySummary(state.activitySummary()));
            tag.put("Evidence", writeEvidence(state.evidence()));
            tag.put("Score", writeScore(state.score()));
            tag.put("Ports", writePorts(state.boundaryPorts()));
            if (state.candidate() != null) tag.put("Candidate", writeCandidate(state.candidate()));
            list.add(tag);
        });
        root.put("Chunks", list);
        return root;
    }

    static CivilizationSavedData load(CompoundTag root) {
        int schemaVersion = root.getIntOr("SchemaVersion", 0);
        if (schemaVersion > SCHEMA_VERSION) {
            LOGGER.error("Civitas civilization schema {} is newer than supported {}; preserving it read-only",
                    schemaVersion, SCHEMA_VERSION);
            CivilizationSavedData protectedData = new CivilizationSavedData();
            protectedData.readOnly = true;
            protectedData.protectedRoot = root.copy();
            protectedData.migrationResult = "future-schema-read-only:" + schemaVersion;
            return protectedData;
        }
        CivilizationSavedData data = new CivilizationSavedData();
        if (schemaVersion < 2) {
            data.migrationResult = "cleared-v" + schemaVersion + "-records-for-v2-rescan";
            LOGGER.info("Migrated Civitas civilization schema {} to v2 by clearing legacy chunk civilization records",
                    schemaVersion);
            return data;
        }
        data.migrationResult = schemaVersion == 2 ? "migrated-v2-to-v3-preserved" : root.getStringOr("MigrationResult", "none");
        for (Tag raw : root.getListOrEmpty("Chunks")) {
            if (!(raw instanceof CompoundTag tag)) continue;
            try {
                String dimension = tag.getStringOr("Dimension", "minecraft:overworld");
                long chunk = tag.getLongOr("Chunk", 0L);
                CivilizationLayer layer = CivilizationLayer.valueOf(tag.getStringOr("Layer", "SURFACE"));
                CivilizationFactors factors = new CivilizationFactors(tag.getDoubleOr("Building", 0),
                        tag.getDoubleOr("Facilities", 0), tag.getDoubleOr("Safety", 0), tag.getDoubleOr("Connectivity", 0));
                CivilityEvidence evidence = readEvidence(tag.getCompoundOrEmpty("Evidence"));
                CivilityScore score = readScore(tag.getCompoundOrEmpty("Score"), factors);
                BoundaryPorts ports = readPorts(tag.getCompoundOrEmpty("Ports"));
                CivilityCandidate candidate = tag.contains("Candidate")
                        ? readCandidate(tag.getCompoundOrEmpty("Candidate")) : null;
                ActivitySummary activitySummary = ActivitySummary.empty();
                if (tag.contains("ActivitySummary")) {
                    try {
                        activitySummary = readActivitySummary(tag.getCompoundOrEmpty("ActivitySummary"));
                    } catch (RuntimeException exception) {
                        LOGGER.warn("Skipping malformed Civitas activity summary for {} {} {}", dimension, chunk, layer);
                    }
                }
                ChunkCivilization state = new ChunkCivilization(factors, tag.getDoubleOr("Target", factors.target()),
                        tag.getDoubleOr("Civility", 0), tag.getDoubleOr("Activity", 0),
                        tag.getLongOr("StableSince", candidate == null ? 0 : candidate.firstSeen()),
                        tag.getLongOr("LastEvaluated", 0), evidence, score, candidate, ports,
                        tag.getLongOr("LastScanned", 0), activitySummary);
                data.states.put(new Key(dimension, chunk, layer), state);
            } catch (RuntimeException exception) {
                LOGGER.warn("Skipping malformed Civitas civilization record", exception);
            }
        }
        return data;
    }

    private static CompoundTag writeEvidence(CivilityEvidence e) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Visited", e.visitedCells()); tag.putInt("Standable", e.standableCells());
        tag.putInt("Passable", e.passableCells()); tag.putInt("Largest", e.largestConnectedCells());
        tag.putDouble("Enclosure", e.enclosureRatio()); tag.putInt("Safe", e.safePassableCells());
        tag.putInt("Hazards", e.hazardousBoundaries()); tag.putInt("ProtectedHazards", e.protectedHazardousBoundaries());
        tag.putIntArray("Facilities", e.facilityDistributionPoints().stream().mapToInt(Integer::intValue).toArray());
        tag.putInt("ConnectedFacilities", e.connectedFacilities());
        tag.putInt("TerritoryEdges", e.territoryEdges()); tag.putInt("ConnectedEdges", e.connectedTerritoryEdges());
        tag.putBoolean("CoreChunk", e.coreChunk()); tag.putBoolean("CoreConnected", e.coreConnected());
        tag.put("Ports", writePorts(e.ports()));
        return tag;
    }

    private static CivilityEvidence readEvidence(CompoundTag tag) {
        int[] raw = tag.getIntArray("Facilities").orElseGet(() -> new int[0]);
        java.util.List<Integer> facilities = Arrays.stream(raw).boxed().toList();
        return new CivilityEvidence(tag.getIntOr("Visited", 0), tag.getIntOr("Standable", 0),
                tag.getIntOr("Passable", 0), tag.getIntOr("Largest", 0), tag.getDoubleOr("Enclosure", 0),
                tag.getIntOr("Safe", 0), tag.getIntOr("Hazards", 0), tag.getIntOr("ProtectedHazards", 0),
                facilities, tag.getIntOr("ConnectedFacilities", 0), tag.getIntOr("TerritoryEdges", 0),
                tag.getIntOr("ConnectedEdges", 0), tag.getBooleanOr("CoreChunk", false),
                tag.getBooleanOr("CoreConnected", false), readPorts(tag.getCompoundOrEmpty("Ports")));
    }

    private static CompoundTag writeScore(CivilityScore score) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Uncapped", score.uncappedTarget()); tag.putDouble("Target", score.target());
        tag.putIntArray("Limits", score.limits().stream().mapToInt(Enum::ordinal).toArray());
        return tag;
    }

    private static CivilityScore readScore(CompoundTag tag, CivilizationFactors factors) {
        ArrayList<CivilityLimitReason> limits = new ArrayList<>();
        for (int ordinal : tag.getIntArray("Limits").orElseGet(() -> new int[0])) {
            if (ordinal >= 0 && ordinal < CivilityLimitReason.values().length) limits.add(CivilityLimitReason.values()[ordinal]);
        }
        return new CivilityScore(factors, tag.getDoubleOr("Uncapped", factors.target()),
                tag.getDoubleOr("Target", factors.target()), limits);
    }

    private static CompoundTag writeCandidate(CivilityCandidate candidate) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Fingerprint", candidate.fingerprint()); tag.putLong("FirstSeen", candidate.firstSeen());
        tag.putLong("ScannedAt", candidate.scannedAt()); tag.put("Evidence", writeEvidence(candidate.evidence()));
        tag.put("Score", writeScore(candidate.score()));
        return tag;
    }

    private static CivilityCandidate readCandidate(CompoundTag tag) {
        CivilityEvidence evidence = readEvidence(tag.getCompoundOrEmpty("Evidence"));
        CivilizationFactors factors = CivilityScoringRules.score(evidence).factors();
        return new CivilityCandidate(tag.getStringOr("Fingerprint", ""), evidence,
                readScore(tag.getCompoundOrEmpty("Score"), factors), tag.getLongOr("FirstSeen", 0),
                tag.getLongOr("ScannedAt", 0));
    }

    private static CompoundTag writePorts(BoundaryPorts ports) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("North", ports.north()); tag.putInt("East", ports.east());
        tag.putInt("South", ports.south()); tag.putInt("West", ports.west());
        return tag;
    }

    private static BoundaryPorts readPorts(CompoundTag tag) {
        return new BoundaryPorts(tag.getIntOr("North", 0), tag.getIntOr("East", 0),
                tag.getIntOr("South", 0), tag.getIntOr("West", 0));
    }

    private static CompoundTag writeActivitySummary(ActivitySummary summary) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("LastActivityTime", summary.lastActivityTime());
        tag.putLong("LastEvaluated", summary.lastEvaluated());
        tag.putLongArray("CategoryLastSeen", summary.categoryLastSeen());
        tag.putInt("LastCategoryMask", summary.lastCategoryMask());
        tag.putDouble("LastDirectGain", summary.lastDirectGain());
        tag.putDouble("LastPropagatedGain", summary.lastPropagatedGain());
        tag.putInt("LastContributorCount", summary.lastContributorCount());
        tag.putInt("LastDecayPeriods", summary.lastDecayPeriods());
        return tag;
    }

    private static ActivitySummary readActivitySummary(CompoundTag tag) {
        long[] seen = tag.getLongArray("CategoryLastSeen").orElseGet(() -> new long[5]);
        if (seen.length != 5) throw new IllegalArgumentException("activity category summary must contain five entries");
        return new ActivitySummary(tag.getLongOr("LastActivityTime", 0), tag.getLongOr("LastEvaluated", 0), seen,
                tag.getIntOr("LastCategoryMask", 0), tag.getDoubleOr("LastDirectGain", 0),
                tag.getDoubleOr("LastPropagatedGain", 0), tag.getIntOr("LastContributorCount", 0),
                tag.getIntOr("LastDecayPeriods", 0));
    }

    private record Key(String dimension, long chunk, CivilizationLayer layer) {}
}
