package com.seaquake6324.civitas.domain.building;

import com.seaquake6324.civitas.domain.civilization.FacilityCategory;

/** Player-declared use. The required real facility is deliberately domain-owned. */
public enum BuildingPurpose {
    RESIDENCE(FacilityCategory.RESIDENTIAL),
    WORKSHOP(FacilityCategory.PRODUCTION),
    STORAGE(FacilityCategory.STORAGE),
    FARM(FacilityCategory.FOOD),
    PUBLIC(FacilityCategory.PUBLIC),
    GUARD_POST(FacilityCategory.PUBLIC);

    private final FacilityCategory requiredFacility;
    BuildingPurpose(FacilityCategory requiredFacility) { this.requiredFacility = requiredFacility; }
    public FacilityCategory requiredFacility() { return requiredFacility; }
}
