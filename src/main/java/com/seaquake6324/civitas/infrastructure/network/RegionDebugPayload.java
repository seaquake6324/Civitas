package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** One server-authoritative snapshot shared by all three client-side debug pages. */
public record RegionDebugPayload(boolean enabled, long serverGameTime, SpawnData spawn,
        RegionData region, CivilizationData civilization, EvidenceData evidence,
        ScheduleData schedule, ActivityData activity) implements CustomPacketPayload {
    public static final Type<RegionDebugPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "region_debug"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RegionDebugPayload> STREAM_CODEC = CustomPacketPayload.codec(
            RegionDebugPayload::write, RegionDebugPayload::read);

    public static RegionDebugPayload disabled() {
        return new RegionDebugPayload(false, 0, null, null, null, null, null, null);
    }

    public RegionDebugPayload(boolean enabled, long serverGameTime, SpawnData spawn,
            RegionData region, CivilizationData civilization) {
        this(enabled, serverGameTime, spawn, region, civilization, EvidenceData.empty(), ScheduleData.empty(), ActivityData.empty());
    }

    public RegionDebugPayload(boolean enabled, long serverGameTime, SpawnData spawn, RegionData region,
            CivilizationData civilization, EvidenceData evidence, ScheduleData schedule) {
        this(enabled, serverGameTime, spawn, region, civilization, evidence, schedule, ActivityData.empty());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(enabled);
        if (!enabled) return;
        buffer.writeVarLong(serverGameTime);
        spawn.write(buffer);
        region.write(buffer);
        civilization.write(buffer);
        evidence.write(buffer);
        schedule.write(buffer);
        activity.write(buffer);
    }

    private static RegionDebugPayload read(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) return disabled();
        return new RegionDebugPayload(true, buffer.readVarLong(), SpawnData.read(buffer),
                RegionData.read(buffer), CivilizationData.read(buffer), EvidenceData.read(buffer), ScheduleData.read(buffer),
                ActivityData.read(buffer));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record SpawnData(int surfaceCount, int undergroundCount, int poolLimit, int totalCount,
            int totalLimit, int localSurfaceCount, int localUndergroundCount, int localRadius,
            int spawnableChunks, float multiplier, float poolShare, long windowTicks, long attempts,
            long totalCapRejected, long poolCapRejected, long civilityRejected, long successful,
            long vanillaDespawns, long acceleratedDespawns) {
        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(surfaceCount); buffer.writeVarInt(undergroundCount); buffer.writeVarInt(poolLimit);
            buffer.writeVarInt(totalCount); buffer.writeVarInt(totalLimit);
            buffer.writeVarInt(localSurfaceCount); buffer.writeVarInt(localUndergroundCount); buffer.writeVarInt(localRadius);
            buffer.writeVarInt(spawnableChunks); buffer.writeFloat(multiplier); buffer.writeFloat(poolShare);
            buffer.writeVarLong(windowTicks); buffer.writeVarLong(attempts); buffer.writeVarLong(totalCapRejected);
            buffer.writeVarLong(poolCapRejected); buffer.writeVarLong(civilityRejected); buffer.writeVarLong(successful);
            buffer.writeVarLong(vanillaDespawns); buffer.writeVarLong(acceleratedDespawns);
        }

        private static SpawnData read(RegistryFriendlyByteBuf buffer) {
            return new SpawnData(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readVarLong(), buffer.readVarLong(), buffer.readVarLong(),
                    buffer.readVarLong(), buffer.readVarLong(), buffer.readVarLong(), buffer.readVarLong(), buffer.readVarLong());
        }
    }

    public record RegionData(int regionType, int reason, int x, int y, int z, int cellX, int cellY,
            int cellZ, int sampleX, int sampleY, int sampleZ, float confidence, int surfaceMedian,
            int burialDepth, int skySamples, int outdoorPath, int coverageMedian, int coverageMaximum,
            float enclosure, int visitedNodes, boolean searchExhausted, int nodeLimit, int cachedCells,
            boolean cacheHit, long cacheHits, long cacheMisses, long computeNanos, long rollingAverageNanos,
            long rollingMaximumNanos, int invalidationReason, int surfacePathLimit, int interiorPathLimit,
            int shallowDepthLimit, int thinCoverageLimit, int undergroundDepthLimit,
            int undergroundCoverageLimit, float enclosureThreshold, float scoreThreshold, int color) {
        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(regionType); buffer.writeVarInt(reason);
            buffer.writeInt(x); buffer.writeInt(y); buffer.writeInt(z);
            buffer.writeInt(cellX); buffer.writeInt(cellY); buffer.writeInt(cellZ);
            buffer.writeInt(sampleX); buffer.writeInt(sampleY); buffer.writeInt(sampleZ);
            buffer.writeFloat(confidence); buffer.writeVarInt(surfaceMedian); buffer.writeVarInt(burialDepth);
            buffer.writeVarInt(skySamples); buffer.writeVarInt(outdoorPath); buffer.writeVarInt(coverageMedian);
            buffer.writeVarInt(coverageMaximum); buffer.writeFloat(enclosure); buffer.writeVarInt(visitedNodes);
            buffer.writeBoolean(searchExhausted); buffer.writeVarInt(nodeLimit); buffer.writeVarInt(cachedCells);
            buffer.writeBoolean(cacheHit); buffer.writeVarLong(cacheHits); buffer.writeVarLong(cacheMisses);
            buffer.writeVarLong(computeNanos); buffer.writeVarLong(rollingAverageNanos); buffer.writeVarLong(rollingMaximumNanos);
            buffer.writeVarInt(invalidationReason); buffer.writeVarInt(surfacePathLimit); buffer.writeVarInt(interiorPathLimit);
            buffer.writeVarInt(shallowDepthLimit); buffer.writeVarInt(thinCoverageLimit); buffer.writeVarInt(undergroundDepthLimit);
            buffer.writeVarInt(undergroundCoverageLimit); buffer.writeFloat(enclosureThreshold);
            buffer.writeFloat(scoreThreshold); buffer.writeInt(color);
        }

        private static RegionData read(RegistryFriendlyByteBuf buffer) {
            return new RegionData(buffer.readVarInt(), buffer.readVarInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                    buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                    buffer.readFloat(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(), buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readVarLong(), buffer.readVarLong(),
                    buffer.readVarLong(), buffer.readVarLong(), buffer.readVarLong(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readInt());
        }
    }

    public record CivilizationData(int layer, float building, float facilities, float safety,
            float connectivity,float buildingWeight,float facilitiesWeight,float safetyWeight,float connectivityWeight, float targetCivility, float currentCivility, float activity,
            float baseSuppression, float activityModifier, float finalSuppression, long stableSince,
            long lastEvaluated) {
        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(layer); buffer.writeFloat(building); buffer.writeFloat(facilities);
            buffer.writeFloat(safety); buffer.writeFloat(connectivity);buffer.writeFloat(buildingWeight);buffer.writeFloat(facilitiesWeight);buffer.writeFloat(safetyWeight);buffer.writeFloat(connectivityWeight); buffer.writeFloat(targetCivility);
            buffer.writeFloat(currentCivility); buffer.writeFloat(activity); buffer.writeFloat(baseSuppression);
            buffer.writeFloat(activityModifier); buffer.writeFloat(finalSuppression);
            buffer.writeVarLong(stableSince); buffer.writeVarLong(lastEvaluated);
        }

        private static CivilizationData read(RegistryFriendlyByteBuf buffer) {
            return new CivilizationData(buffer.readVarInt(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(),buffer.readFloat(),buffer.readFloat(),buffer.readFloat(),buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readVarLong(), buffer.readVarLong());
        }
    }

    public record EvidenceData(int visited, int standable, int passable, int largestConnected,
            float enclosure, int safePassable, int hazards, int protectedHazards,
            int[] facilityPoints, int connectedFacilities, int territoryEdges, int connectedEdges,
            int northPorts, int eastPorts, int southPorts, int westPorts, String limits,
            String candidateFingerprint, float candidateTarget, long candidateFirstSeen) {
        public EvidenceData { facilityPoints = facilityPoints == null ? new int[0] : facilityPoints.clone(); }
        @Override public int[] facilityPoints() { return facilityPoints.clone(); }
        private static EvidenceData empty() { return new EvidenceData(0,0,0,0,0,0,0,0,new int[0],0,0,0,0,0,0,0,"","",0,0); }
        private void write(RegistryFriendlyByteBuf b) {
            b.writeVarInt(visited); b.writeVarInt(standable); b.writeVarInt(passable); b.writeVarInt(largestConnected);
            b.writeFloat(enclosure); b.writeVarInt(safePassable); b.writeVarInt(hazards); b.writeVarInt(protectedHazards);
            b.writeVarInt(facilityPoints.length); for (int point : facilityPoints) b.writeVarInt(point);
            b.writeVarInt(connectedFacilities); b.writeVarInt(territoryEdges); b.writeVarInt(connectedEdges);
            b.writeVarInt(northPorts); b.writeVarInt(eastPorts); b.writeVarInt(southPorts); b.writeVarInt(westPorts);
            b.writeUtf(limits); b.writeUtf(candidateFingerprint); b.writeFloat(candidateTarget); b.writeVarLong(candidateFirstSeen);
        }
        private static EvidenceData read(RegistryFriendlyByteBuf b) {
            int visited=b.readVarInt(), standable=b.readVarInt(), passable=b.readVarInt(), largest=b.readVarInt();
            float enclosure=b.readFloat(); int safe=b.readVarInt(), hazards=b.readVarInt(), protectedHazards=b.readVarInt();
            int[] points = new int[b.readVarInt()]; for (int i=0;i<points.length;i++) points[i]=b.readVarInt();
            return new EvidenceData(visited,standable,passable,largest,enclosure,safe,hazards,protectedHazards,points,
                    b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),
                    b.readUtf(),b.readUtf(),b.readFloat(),b.readVarLong());
        }
        @Override public boolean equals(Object other) {
            return other instanceof EvidenceData e && visited==e.visited && standable==e.standable && passable==e.passable
                    && largestConnected==e.largestConnected && Float.compare(enclosure,e.enclosure)==0
                    && safePassable==e.safePassable && hazards==e.hazards && protectedHazards==e.protectedHazards
                    && java.util.Arrays.equals(facilityPoints,e.facilityPoints) && connectedFacilities==e.connectedFacilities
                    && territoryEdges==e.territoryEdges && connectedEdges==e.connectedEdges && northPorts==e.northPorts
                    && eastPorts==e.eastPorts && southPorts==e.southPorts && westPorts==e.westPorts
                    && limits.equals(e.limits) && candidateFingerprint.equals(e.candidateFingerprint)
                    && Float.compare(candidateTarget,e.candidateTarget)==0 && candidateFirstSeen==e.candidateFirstSeen;
        }
        @Override public int hashCode() { return 31 * java.util.Objects.hash(visited,standable,passable,largestConnected,enclosure,
                safePassable,hazards,protectedHazards,connectedFacilities,territoryEdges,connectedEdges,northPorts,eastPorts,
                southPorts,westPorts,limits,candidateFingerprint,candidateTarget,candidateFirstSeen) + java.util.Arrays.hashCode(facilityPoints); }
    }

    public record ScheduleData(int queueLength, long oldestAge, long lastNanos, long averageNanos,
            long maxNanos, int visitedCells, long unloadedDeferrals, boolean progressiveRescan,
            int lastReasons, int lastDeferrals, int schemaVersion, String migrationResult, boolean readOnly) {
        private static ScheduleData empty() { return new ScheduleData(0,0,0,0,0,0,0,false,0,0,2,"none",false); }
        private void write(RegistryFriendlyByteBuf b) {
            b.writeVarInt(queueLength); b.writeVarLong(oldestAge); b.writeVarLong(lastNanos); b.writeVarLong(averageNanos);
            b.writeVarLong(maxNanos); b.writeVarInt(visitedCells); b.writeVarLong(unloadedDeferrals); b.writeBoolean(progressiveRescan);
            b.writeVarInt(lastReasons); b.writeVarInt(lastDeferrals); b.writeVarInt(schemaVersion); b.writeUtf(migrationResult); b.writeBoolean(readOnly);
        }
        private static ScheduleData read(RegistryFriendlyByteBuf b) {
            return new ScheduleData(b.readVarInt(),b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readVarInt(),
                    b.readVarLong(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readUtf(),b.readBoolean());
        }
    }

    public record ActivityData(int layer, float activity, int activityTier, long windowRemaining,
            int categoryMask, int contributors, float lastDirectGain, float lastPropagatedGain,
            long lastActivityTime, long graceRemaining, float decayPerWindow, int lastDecayPeriods,
            int propagationMask, int cachedWindows, long rejectedNonMember, long rejectedWilderness,
            long rejectedAutomation, long rejectedDuplicate, long rejectedUnloaded, long rejectedLayer) {
        private static ActivityData empty() { return new ActivityData(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0); }
        private void write(RegistryFriendlyByteBuf b) {
            b.writeVarInt(layer); b.writeFloat(activity); b.writeVarInt(activityTier); b.writeVarLong(windowRemaining);
            b.writeVarInt(categoryMask); b.writeVarInt(contributors); b.writeFloat(lastDirectGain); b.writeFloat(lastPropagatedGain);
            b.writeVarLong(lastActivityTime); b.writeVarLong(graceRemaining); b.writeFloat(decayPerWindow);
            b.writeVarInt(lastDecayPeriods); b.writeVarInt(propagationMask); b.writeVarInt(cachedWindows);
            b.writeVarLong(rejectedNonMember); b.writeVarLong(rejectedWilderness); b.writeVarLong(rejectedAutomation);
            b.writeVarLong(rejectedDuplicate); b.writeVarLong(rejectedUnloaded); b.writeVarLong(rejectedLayer);
        }
        private static ActivityData read(RegistryFriendlyByteBuf b) {
            return new ActivityData(b.readVarInt(),b.readFloat(),b.readVarInt(),b.readVarLong(),b.readVarInt(),b.readVarInt(),
                    b.readFloat(),b.readFloat(),b.readVarLong(),b.readVarLong(),b.readFloat(),b.readVarInt(),b.readVarInt(),
                    b.readVarInt(),b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readVarLong());
        }
    }
}
