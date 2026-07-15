package com.seaquake6324.civitas.application;

import static org.junit.jupiter.api.Assertions.*;
import com.seaquake6324.civitas.domain.civilization.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class CivilityEvaluationServiceTest {
    private final CivilityEvaluationService service = new CivilityEvaluationService();
    private final CivilityEvidence evidence = new CivilityEvidence(100, 64, 64, 64, 1, 64, 4, 4,
            List.of(1, 1, 1, 1, 1, 1), 6, 2, 2, true, true, new BoundaryPorts(1, 1, 1, 1));

    @Test void sameCandidateDoesNotResetAndCommitsAtExactly1200Ticks() {
        var scan = new CivilityEvaluationService.ScanSnapshot("same", evidence);
        ChunkCivilization at0 = service.evaluate(ChunkCivilization.empty(), scan, 100, 1200, .25, .5);
        ChunkCivilization at1199 = service.evaluate(at0, scan, 1299, 1200, .25, .5);
        assertNotNull(at1199.candidate());
        assertEquals(100, at1199.candidate().firstSeen());
        ChunkCivilization at1200 = service.evaluate(at1199, scan, 1300, 1200, .25, .5);
        assertNull(at1200.candidate());
        assertTrue(at1200.targetCivility() > 0);
    }

    @Test void differentCandidateResetsWindow() {
        ChunkCivilization first = service.evaluate(ChunkCivilization.empty(),
                new CivilityEvaluationService.ScanSnapshot("a", evidence), 100, 1200, .25, .5);
        ChunkCivilization changed = service.evaluate(first,
                new CivilityEvaluationService.ScanSnapshot("b", evidence), 1000, 1200, .25, .5);
        assertEquals(1000, changed.candidate().firstSeen());
    }

    @Test void elapsedCyclesGrowDeclineAndNeverOvershoot() {
        ChunkCivilization growing = new ChunkCivilization(new CivilizationFactors(100,100,100,100), 100, 0, 0, 0, 100);
        assertEquals(2.5, service.evolve(growing, 2100, .25, .5).civility());
        ChunkCivilization falling = new ChunkCivilization(CivilizationFactors.empty(), 0, 100, 0, 0, 100);
        assertEquals(95, service.evolve(falling, 2100, .25, .5).civility());
        assertEquals(100, service.evolve(growing, 999999, .25, .5).civility());
        assertEquals(0, service.evolve(falling, 999999, .25, .5).civility());
    }
}
