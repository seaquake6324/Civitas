package com.seaquake6324.civitas.domain.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegionClassificationRulesTest {
    private static final RegionClassificationRules.Settings SETTINGS = new RegionClassificationRules.Settings(
            8, 20, 8, 12, 5, 8, 0.34, 0.62);

    @Test
    void exposesTheExactSurfaceRuleReason() {
        RegionDiagnostics shortRoute = classify(new RegionEvidence(80, 20, 0, 4, true,
                12, 16, 0.8, 30, false));
        RegionDiagnostics openSky = classify(new RegionEvidence(80, 20, 2, Integer.MAX_VALUE, false,
                12, 16, 0.8, 30, false));

        assertEquals(RegionClassificationReason.SHORT_OUTDOOR_ROUTE, shortRoute.reason());
        assertEquals(RegionClassificationReason.OPEN_SKY_SAMPLING, openSky.reason());
        assertTrue(shortRoute.compactText().contains("short outdoor route"));
    }

    @Test
    void exposesInteriorAndUndergroundRuleReasons() {
        RegionDiagnostics shallowInterior = classify(new RegionEvidence(70, 4, 0, 10, true,
                3, 5, 0.5, 40, false));
        RegionDiagnostics underground = classify(new RegionEvidence(90, 30, 0, Integer.MAX_VALUE, false,
                20, 24, 0.8, 80, false));
        RegionDiagnostics thinRoofGuard = classify(new RegionEvidence(90, 30, 0, Integer.MAX_VALUE, false,
                3, 4, 0.8, 80, true));
        RegionDiagnostics insufficientEvidence = classify(new RegionEvidence(70, 9, 0, Integer.MAX_VALUE, false,
                7, 9, 0.2, 80, false));

        assertEquals(RegionClassificationReason.SHALLOW_THIN_ROOF_OUTDOOR_ROUTE, shallowInterior.reason());
        assertEquals(RegionClassificationReason.BURIED_THICK_COVERED_ENCLOSED_DISCONNECTED, underground.reason());
        assertEquals(RegionClassificationReason.THIN_OR_MAN_MADE_ROOF_GUARD, thinRoofGuard.reason());
        assertEquals(RegionClassificationReason.INSUFFICIENT_INDEPENDENT_UNDERGROUND_EVIDENCE,
                insufficientEvidence.reason());
    }

    private static RegionDiagnostics classify(RegionEvidence evidence) {
        return RegionClassificationRules.classify(evidence, SETTINGS);
    }
}
