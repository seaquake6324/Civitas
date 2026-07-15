package com.seaquake6324.civitas.application;

import static org.junit.jupiter.api.Assertions.*;
import com.seaquake6324.civitas.domain.population.*;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import java.util.*;
import org.junit.jupiter.api.Test;

class UpdateCitizenLocationServiceTest {
    @Test void commitsOnlyFreshSettledLivingObservationAndRevisionIncreases() {
        PopulationSavedData data=new PopulationSavedData();UUID city=UUID.randomUUID();
        CitizenRecord citizen=new CitizenRecord(UUID.randomUUID(),"Mira","North",CitizenRace.HUMAN,"human_0",Gender.FEMALE,
                20,20,city,null,null,null,"",100,CitizenNeeds.neutral(),Map.of(),Set.of(),50,0,CitizenRuntimeState.MATERIALIZED,0,75,0,1);
        data.putCitizen(citizen);UpdateCitizenLocationService service=new UpdateCitizenLocationService();
        var first=service.update(data,citizen.id(),1,"minecraft:overworld",12,100);assertTrue(first.success());assertEquals(1,first.location().revision());
        var unchanged=service.update(data,citizen.id(),1,"minecraft:overworld",12,101);assertTrue(unchanged.success());assertEquals(1,unchanged.location().revision());
        assertEquals(UpdateCitizenLocationService.Failure.STALE,service.update(data,citizen.id(),0,"minecraft:overworld",13,101).failure());
        var second=service.update(data,citizen.id(),1,"minecraft:overworld",14,102);assertTrue(second.success());assertEquals(2,second.location().revision());assertEquals(14,data.location(citizen.id()).orElseThrow().position());
    }
}
