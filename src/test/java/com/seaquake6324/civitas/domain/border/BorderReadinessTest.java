package com.seaquake6324.civitas.domain.border;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class BorderReadinessTest { @Test void retainsExplainableWeightedParts(){var r=BorderReadiness.calculate(40,20,50,75);assertEquals(43,r.total(),.0001);assertEquals(12,r.civilityPart(),.0001);assertEquals(6,r.activityPart(),.0001);} }
