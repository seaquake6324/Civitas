package com.seaquake6324.civitas.domain.building;

import com.seaquake6324.civitas.domain.civilization.FacilityCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Complete explainable result; presentation may show only the first failure. */
public final class BuildingValidation {
    public enum Failure {
        NOT_MANAGER, WRONG_DIMENSION, ENTRANCE_INVALID, INTERIOR_INVALID, OUTSIDE_TERRITORY,
        OPEN_BOUNDARY, SCAN_LIMIT, EVIDENCE_INCOMPLETE, MISSING_FACILITY, OVERLAP, STALE_CITY
    }

    public record Evidence(int visited, int accepted, boolean entranceValid, boolean interiorValid,
            boolean territoryValid, boolean openBoundary, boolean limitReached,
            Map<FacilityCategory, Integer> facilities, Set<Long> cells, BuildingFeatures features, boolean evidenceComplete, int queuePeak,
            long elapsedMicros) {
        public Evidence { facilities = Map.copyOf(facilities); cells = Set.copyOf(cells); java.util.Objects.requireNonNull(features); }
        public Evidence(int visited, int accepted, boolean entranceValid, boolean interiorValid,
                boolean territoryValid, boolean openBoundary, boolean limitReached,
                Map<FacilityCategory, Integer> facilities, Set<Long> cells, long elapsedMicros) {
            this(visited, accepted, entranceValid, interiorValid, territoryValid, openBoundary,
                    limitReached, facilities, cells, BuildingFeatures.EMPTY, true, Math.max(0, accepted), elapsedMicros);
        }
    }

    public record Result(List<Failure> failures, int capacity, Evidence evidence,
            BuildingRequirement requirement, int facilityCount,
            int facilityCapacity, int spaceCapacity) {
        public Result { failures = List.copyOf(failures); }
        public boolean valid() { return failures.isEmpty(); }
    }

    public static Result evaluate(BuildingPurpose purpose, Evidence evidence, boolean manager,
            boolean dimensionMatches, boolean overlaps) {
        List<Failure> failures = new ArrayList<>();
        if (!manager) failures.add(Failure.NOT_MANAGER);
        if (!dimensionMatches) failures.add(Failure.WRONG_DIMENSION);
        if (!evidence.entranceValid()) failures.add(Failure.ENTRANCE_INVALID);
        if (!evidence.interiorValid()) failures.add(Failure.INTERIOR_INVALID);
        if (!evidence.territoryValid()) failures.add(Failure.OUTSIDE_TERRITORY);
        if (evidence.openBoundary()) failures.add(Failure.OPEN_BOUNDARY);
        if (evidence.limitReached()) failures.add(Failure.SCAN_LIMIT);
        if (!evidence.evidenceComplete()) failures.add(Failure.EVIDENCE_INCOMPLETE);
        BuildingRequirement requirement = BuildingRequirement.forPurpose(purpose);
        int facilityCount = evidence.facilities().getOrDefault(requirement.facility(), 0);
        if (facilityCount < requirement.minimumFacilities()) failures.add(Failure.MISSING_FACILITY);
        if (overlaps) failures.add(Failure.OVERLAP);
        BuildingRequirement.Capacity explained = requirement.explainCapacity(facilityCount, evidence.accepted());
        int capacity = failures.isEmpty() ? explained.effectiveCapacity() : 0;
        return new Result(failures, capacity, evidence, requirement, facilityCount,
                explained.facilityCapacity(), explained.spaceCapacity());
    }

    private BuildingValidation() {}
}
