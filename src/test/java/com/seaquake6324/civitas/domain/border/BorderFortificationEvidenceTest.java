package com.seaquake6324.civitas.domain.border;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;
class BorderFortificationEvidenceTest{@Test void gapsLowerBoundedScoreAndEvidenceStaysExplainable(){var weak=BorderFortificationEvidence.calculate(16,2,0,12,0,2);var strong=BorderFortificationEvidence.calculate(16,14,2,0,1,14);assertTrue(strong.score()>weak.score());assertTrue(strong.score()<=100);assertEquals(16,strong.scannedColumns());}}
