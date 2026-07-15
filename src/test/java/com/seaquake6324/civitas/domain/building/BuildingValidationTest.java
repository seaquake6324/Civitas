package com.seaquake6324.civitas.domain.building;

import static org.junit.jupiter.api.Assertions.*;
import com.seaquake6324.civitas.domain.civilization.FacilityCategory;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuildingValidationTest {
    @Test void validResidenceExplainsCapacityFromRealFacilitiesAndSpace() {
        var evidence = evidence(Map.of(FacilityCategory.RESIDENTIAL, 4), 18);
        var result = BuildingValidation.evaluate(BuildingPurpose.RESIDENCE, evidence, true, true, false);
        assertTrue(result.valid());
        assertEquals(2, result.capacity());
        assertEquals(18, result.evidence().accepted());
        assertEquals(FacilityCategory.RESIDENTIAL, result.requirement().facility());
        assertEquals(2, result.requirement().facilitiesPerCapacity());
        assertEquals(2, result.facilityCapacity());
        assertEquals(3, result.spaceCapacity());
    }

    @Test void everyPurposeHasAnExplicitPositiveRequirement() {
        for (BuildingPurpose purpose : BuildingPurpose.values()) {
            BuildingRequirement requirement = BuildingRequirement.forPurpose(purpose);
            assertEquals(purpose.requiredFacility(), requirement.facility());
            assertTrue(requirement.minimumFacilities() > 0);
            assertTrue(requirement.facilitiesPerCapacity() > 0);
            assertTrue(requirement.cellsPerCapacity() > 0);
        }
    }

    @Test void unloadedEvidenceIsDistinctFromTerritoryFailure() {
        var base=evidence(Map.of(FacilityCategory.PRODUCTION,1),8);
        var incomplete=new BuildingValidation.Evidence(base.visited(),base.accepted(),true,true,true,false,false,
                base.facilities(),base.cells(),BuildingFeatures.EMPTY,false,base.queuePeak(),base.elapsedMicros());
        var result=BuildingValidation.evaluate(BuildingPurpose.WORKSHOP,incomplete,true,true,false);
        assertTrue(result.failures().contains(BuildingValidation.Failure.EVIDENCE_INCOMPLETE));
        assertFalse(result.failures().contains(BuildingValidation.Failure.OUTSIDE_TERRITORY));
    }

    @Test void reportsAllRelevantFailuresInsteadOfHidingTheCalculationPath() {
        var evidence = new BuildingValidation.Evidence(513, 20, false, true, false, true, true,
                Map.of(), Set.of(1L), 90);
        var result = BuildingValidation.evaluate(BuildingPurpose.GUARD_POST, evidence, false, true, true);
        assertEquals(java.util.List.of(BuildingValidation.Failure.NOT_MANAGER,
                BuildingValidation.Failure.ENTRANCE_INVALID, BuildingValidation.Failure.OUTSIDE_TERRITORY,
                BuildingValidation.Failure.OPEN_BOUNDARY, BuildingValidation.Failure.SCAN_LIMIT,
                BuildingValidation.Failure.MISSING_FACILITY, BuildingValidation.Failure.OVERLAP), result.failures());
    }

    @Test void staleBuildingsDoNotContributePopulationOrSecurityCapacity() {
        UUID city = UUID.randomUUID();
        BuildingRecord residence = record(city, BuildingPurpose.RESIDENCE, 3);
        BuildingRecord guard = record(city, BuildingPurpose.GUARD_POST, 2).stale("block_changed");
        var summary = BuildingCapacitySummary.from(java.util.List.of(residence, guard));
        assertEquals(1, summary.validBuildings());
        assertEquals(1, summary.staleBuildings());
        assertEquals(0, summary.invalidBuildings());
        assertEquals(3, summary.housingCapacity());
        assertEquals(0, summary.guardCapacity());
    }

    private static BuildingValidation.Evidence evidence(Map<FacilityCategory,Integer> facilities, int cells) {
        Set<Long> positions = java.util.stream.LongStream.range(1, cells + 1).boxed().collect(java.util.stream.Collectors.toSet());
        return new BuildingValidation.Evidence(cells, cells, true, true, true, false, false, facilities, positions, 50);
    }
    private static BuildingRecord record(UUID city, BuildingPurpose purpose, int capacity) {
        return new BuildingRecord(UUID.randomUUID(), city, "minecraft:overworld", purpose, 1, 2,
                Set.of(3L), Map.of(purpose.requiredFacility(), 2), capacity, BuildingStatus.VALID, 1, 20, "");
    }
}
