package com.seaquake6324.civitas.domain.building;

import com.seaquake6324.civitas.domain.civilization.FacilityCategory;

/**
 * Explainable first-pass requirement for a declared building use.
 * Values preserve the already shipped validation behaviour and are kept here so
 * registration, revalidation, diagnostics and presentation consume one rule.
 */
public record BuildingRequirement(FacilityCategory facility, int minimumFacilities,
        int facilitiesPerCapacity, int cellsPerCapacity) {
    public BuildingRequirement {
        if (minimumFacilities < 1 || facilitiesPerCapacity < 1 || cellsPerCapacity < 1) {
            throw new IllegalArgumentException("building requirement values must be positive");
        }
    }

    public static BuildingRequirement forPurpose(BuildingPurpose purpose) {
        return switch (purpose) {
            case RESIDENCE -> new BuildingRequirement(purpose.requiredFacility(), 1, 2, 6);
            case GUARD_POST -> new BuildingRequirement(purpose.requiredFacility(), 1, 1, 8);
            default -> new BuildingRequirement(purpose.requiredFacility(), 1, 1, 4);
        };
    }

    public Capacity explainCapacity(int facilityCount, int acceptedCells) {
        int facilityCapacity = Math.max(0, facilityCount) / facilitiesPerCapacity;
        int spaceCapacity = Math.max(1, Math.max(0, acceptedCells) / cellsPerCapacity);
        return new Capacity(facilityCapacity, spaceCapacity, Math.min(facilityCapacity, spaceCapacity));
    }

    public record Capacity(int facilityCapacity, int spaceCapacity, int effectiveCapacity) {}
}
