package com.seaquake6324.civitas.domain.region;

/** Stable, machine-readable explanation for the rule that selected a region type. */
public enum RegionClassificationReason {
    SHORT_OUTDOOR_ROUTE("short outdoor route"),
    OPEN_SKY_SAMPLING("open sky sampling"),
    SHALLOW_THIN_ROOF_OUTDOOR_ROUTE("shallow thin-roof enclosure with an outdoor route"),
    BURIED_THICK_COVERED_ENCLOSED_DISCONNECTED("buried, thick-covered, enclosed and surface-disconnected"),
    THIN_OR_MAN_MADE_ROOF_GUARD("thin/man-made roof guard"),
    INSUFFICIENT_INDEPENDENT_UNDERGROUND_EVIDENCE("insufficient independent underground evidence");

    private final String compactText;

    RegionClassificationReason(String compactText) {
        this.compactText = compactText;
    }

    public String compactText() {
        return compactText;
    }
}
