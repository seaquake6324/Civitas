package com.seaquake6324.civitas.application;

import static org.junit.jupiter.api.Assertions.*;
import com.seaquake6324.civitas.application.port.BuildingRepository;
import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.building.*;
import com.seaquake6324.civitas.domain.civilization.FacilityCategory;
import java.util.*;
import org.junit.jupiter.api.Test;

class RegisterBuildingServiceTest {
    @Test void persistsValidatedBuildingAndRejectsOverlappingSecondRegistration() {
        UUID leader = UUID.randomUUID();
        City city = new City(UUID.randomUUID(), "Test", 0, "minecraft:overworld", 0,
                ChunkCoordinate.pack(0,0), 1, leader, leader, Set.of(leader), Set.of(ChunkCoordinate.pack(0,0)));
        MemoryRepository repository = new MemoryRepository();
        var evidence = new BuildingValidation.Evidence(12, 12, true, true, true, false, false,
                Map.of(FacilityCategory.PRODUCTION, 2), Set.of(1L,2L,3L), 10);
        RegisterBuildingService service = new RegisterBuildingService();
        var request = new RegisterBuildingService.Request(city, leader, city.dimension(), BuildingPurpose.WORKSHOP,
                10, 11, CityTopologyToken.of(city), 100, evidence);
        assertTrue(service.register(repository, request).success());
        var duplicate = service.register(repository, request);
        assertFalse(duplicate.success());
        assertTrue(duplicate.validation().failures().contains(BuildingValidation.Failure.OVERLAP));
        assertEquals(1, repository.forCity(city.id()).size());
    }

    @Test void revalidationPreservesIdentityAndRestoresOnlyFreshValidCapacity() {
        UUID leader = UUID.randomUUID();
        City city = new City(UUID.randomUUID(), "Test", 0, "minecraft:overworld", 0,
                ChunkCoordinate.pack(0,0), 1, leader, leader, Set.of(leader), Set.of(ChunkCoordinate.pack(0,0)));
        MemoryRepository repository = new MemoryRepository();
        BuildingRecord stale = new BuildingRecord(UUID.randomUUID(), city.id(), city.dimension(),
                BuildingPurpose.WORKSHOP, 10, 11, Set.of(1L), Map.of(FacilityCategory.PRODUCTION,1),
                0, BuildingStatus.STALE, 2, 10, "block_changed");
        repository.put(stale);
        var evidence = new BuildingValidation.Evidence(8, 8, true, true, true, false, false,
                Map.of(FacilityCategory.PRODUCTION,2), Set.of(1L,2L), 5);
        var result = new RevalidateBuildingService().revalidate(repository, city, stale, 30, evidence);
        assertTrue(result.validation().valid());
        assertEquals(stale.id(), result.building().id());
        assertEquals(BuildingStatus.VALID, result.building().status());
        assertEquals(2, result.building().capacity());
        assertEquals(3, result.building().revision());
    }

    @Test void failedFreshRevalidationBecomesConfirmedInvalidInsteadOfPollingStaleForever() {
        UUID leader=UUID.randomUUID();City city=new City(UUID.randomUUID(),"Test",0,"minecraft:overworld",0,ChunkCoordinate.pack(0,0),1,leader,leader,Set.of(leader),Set.of(ChunkCoordinate.pack(0,0)));MemoryRepository repository=new MemoryRepository();
        BuildingRecord stale=new BuildingRecord(UUID.randomUUID(),city.id(),city.dimension(),BuildingPurpose.WORKSHOP,10,11,Set.of(1L),Map.of(FacilityCategory.PRODUCTION,1),1,BuildingStatus.STALE,2,10,"block_changed");repository.put(stale);
        var invalidEvidence=new BuildingValidation.Evidence(8,8,false,true,true,false,false,Map.of(FacilityCategory.PRODUCTION,1),Set.of(1L),5);
        var result=new RevalidateBuildingService().revalidate(repository,city,stale,30,invalidEvidence);
        assertEquals(BuildingStatus.INVALID,result.building().status());assertEquals(0,result.building().capacity());assertEquals("entrance_invalid",result.building().invalidReason());
    }

    @Test void topologyTokenRejectsSessionAfterAnyTerritoryChange() {
        UUID leader=UUID.randomUUID();City before=new City(UUID.randomUUID(),"Test",0,"minecraft:overworld",0,ChunkCoordinate.pack(0,0),1,leader,leader,Set.of(leader),Set.of(ChunkCoordinate.pack(0,0)));long token=CityTopologyToken.of(before);City changed=before.claim(ChunkCoordinate.pack(1,0),ChunkCoordinate.pack(0,0),50);
        var evidence=new BuildingValidation.Evidence(8,8,true,true,true,false,false,Map.of(FacilityCategory.PRODUCTION,1),Set.of(1L),5);
        var result=new RegisterBuildingService().register(new MemoryRepository(),new RegisterBuildingService.Request(changed,leader,changed.dimension(),BuildingPurpose.WORKSHOP,10,11,token,60,evidence));
        assertFalse(result.success());assertTrue(result.validation().failures().contains(BuildingValidation.Failure.STALE_CITY));
    }

    private static final class MemoryRepository implements BuildingRepository {
        private final Map<UUID,BuildingRecord> records = new HashMap<>();
        public Collection<BuildingRecord> forCity(UUID cityId){return records.values().stream().filter(r->r.cityId().equals(cityId)).toList();}
        public Optional<BuildingRecord> byId(UUID id){return Optional.ofNullable(records.get(id));}
        public void put(BuildingRecord building){records.put(building.id(),building);}
    }
}
