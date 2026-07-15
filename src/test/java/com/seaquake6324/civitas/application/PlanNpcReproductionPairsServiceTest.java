package com.seaquake6324.civitas.application;

import static org.junit.jupiter.api.Assertions.*;
import com.seaquake6324.civitas.domain.population.Gender;
import java.util.*;
import org.junit.jupiter.api.Test;

class PlanNpcReproductionPairsServiceTest {
    @Test void pairsEligibleOppositeGenderCitizensInTheSameCityWithoutMarriageInput(){UUID city=UUID.randomUUID();var service=new PlanNpcReproductionPairsService();var male=new PlanNpcReproductionPairsService.Candidate(new UUID(0,1),city,Gender.MALE,true);var female=new PlanNpcReproductionPairsService.Candidate(new UUID(0,2),city,Gender.FEMALE,true);var result=service.plan(List.of(female,male),2);assertEquals(1,result.pairs().size());assertEquals(male,result.pairs().getFirst().first());assertEquals(female,result.pairs().getFirst().second());}
    @Test void respectsEligibilityCityAndPairCap(){UUID a=UUID.randomUUID(),b=UUID.randomUUID();var service=new PlanNpcReproductionPairsService();List<PlanNpcReproductionPairsService.Candidate>values=List.of(new PlanNpcReproductionPairsService.Candidate(new UUID(0,1),a,Gender.MALE,true),new PlanNpcReproductionPairsService.Candidate(new UUID(0,2),a,Gender.FEMALE,false),new PlanNpcReproductionPairsService.Candidate(new UUID(0,3),b,Gender.FEMALE,true));assertTrue(service.plan(values,1).pairs().isEmpty());}
    @Test void ignoresUnsettledCitizensWithoutFailingTheBoundedScan(){UUID city=UUID.randomUUID();var service=new PlanNpcReproductionPairsService();List<PlanNpcReproductionPairsService.Candidate>values=List.of(new PlanNpcReproductionPairsService.Candidate(new UUID(0,1),null,Gender.MALE,true),new PlanNpcReproductionPairsService.Candidate(new UUID(0,2),city,Gender.FEMALE,true));var result=service.plan(values,2);assertTrue(result.pairs().isEmpty());assertEquals(2,result.examined());}
}
