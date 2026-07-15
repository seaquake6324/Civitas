package com.seaquake6324.civitas.domain.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InfiltrationDefenseRulesTest {
    @Test void lowerRiskCanInterceptAndImprovesWarningAndWaveSize() {
        var rules = new InfiltrationDefenseRules();
        var safe = rules.evaluate(20, .5, 1200, .5);
        var weak = rules.evaluate(90, .5, 1200, .5);
        assertTrue(safe.intercepted()); assertFalse(weak.intercepted());
        assertTrue(safe.warningBonusTicks() > weak.warningBonusTicks());
        assertTrue(safe.waveScale() < weak.waveScale());
    }
}
