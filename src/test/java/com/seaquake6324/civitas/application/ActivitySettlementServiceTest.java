package com.seaquake6324.civitas.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.seaquake6324.civitas.domain.civilization.ActivityCategory;
import com.seaquake6324.civitas.domain.civilization.ActivityWindowEvidence;
import com.seaquake6324.civitas.domain.civilization.ChunkCivilization;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActivitySettlementServiceTest {
    private static final ActivitySettlementService.Settings SETTINGS =
            new ActivitySettlementService.Settings(1200, 6000, .25, .5, .35, 1, 3.5);

    @Test void settlesDirectAndInboundPropagationWithoutRecursingInbound() {
        ActivityWindowEvidence evidence = ActivityWindowEvidence.empty(0)
                .record(ActivityCategory.CONSTRUCTION, UUID.randomUUID()).propagate(.8);
        ActivitySettlementService.Result result = new ActivitySettlementService()
                .settle(ChunkCivilization.empty(), evidence, 1200, SETTINGS);
        assertEquals(1.3, result.state().activity(), 1e-9);
        assertEquals(.5, result.directGain(), 1e-9);
        assertEquals(.175, result.outwardPropagationGain(), 1e-9);
        assertEquals(.8, result.state().activitySummary().lastPropagatedGain(), 1e-9);
    }
}
