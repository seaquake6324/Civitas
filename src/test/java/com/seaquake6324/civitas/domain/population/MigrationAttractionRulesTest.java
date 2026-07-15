package com.seaquake6324.civitas.domain.population;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MigrationAttractionRulesTest {
    @Test void retainsEveryRawContributionAndUsesNoHardThreshold(){var result=new MigrationAttractionRules().evaluate(new MigrationAttractionRules.Inputs(100,50,25,80,40));assertEquals(30,result.components().get("housing"));assertEquals(10,result.components().get("food"));assertEquals(5,result.components().get("safety"));assertEquals(12,result.components().get("civility"));assertEquals(6,result.components().get("activity"));assertEquals(63,result.score());assertEquals(.63,result.appearanceChance());assertThrows(UnsupportedOperationException.class,()->result.components().put("x",1.0));}
    @Test void evenZeroAttractionRetainsSmallBoundedArrivalPossibility(){var result=new MigrationAttractionRules().evaluate(new MigrationAttractionRules.Inputs(0,0,0,0,0));assertEquals(0,result.score());assertEquals(.02,result.appearanceChance());}
}
