package com.seaquake6324.civitas.application;

import static org.junit.jupiter.api.Assertions.*;
import com.seaquake6324.civitas.application.port.BuildingRepository;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.building.*;
import com.seaquake6324.civitas.domain.civilization.FacilityCategory;
import java.util.*;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class AuthorizeBuildingStorageServiceTest {
    @Test void togglesOnlyFreshScannedRealContainerForManager() {
        UUID manager=UUID.randomUUID(),cityId=UUID.randomUUID();City city=city(cityId,manager);BlockPos cell=new BlockPos(1,64,1);long endpoint=cell.north().asLong();
        BuildingRecord building=new BuildingRecord(UUID.randomUUID(),cityId,"minecraft:overworld",BuildingPurpose.WORKSHOP,
                cell.west().asLong(),cell.asLong(),Set.of(cell.asLong()),Map.of(FacilityCategory.PRODUCTION,1,FacilityCategory.STORAGE,1),
                new BuildingFeatures(Set.of(cell.west().asLong()),Set.of(cell.east().asLong()),Set.of(endpoint),true),1,BuildingStatus.VALID,1,10,"");
        Repo repo=new Repo(building);AuthorizeBuildingStorageService service=new AuthorizeBuildingStorageService();
        assertEquals(AuthorizeBuildingStorageService.Failure.NOT_CONTAINER,service.toggle(repo,city,manager,building.id(),1,endpoint,false).failure());
        var enabled=service.toggle(repo,city,manager,building.id(),1,endpoint,true);assertTrue(enabled.success());assertEquals(Set.of(endpoint),enabled.building().authorizedStorageEndpoints());
        var disabled=service.toggle(repo,city,manager,building.id(),2,endpoint,true);assertTrue(disabled.success());assertTrue(disabled.building().authorizedStorageEndpoints().isEmpty());
        assertEquals(AuthorizeBuildingStorageService.Failure.STALE_REVISION,service.toggle(repo,city,manager,building.id(),1,endpoint,true).failure());
    }
    private static City city(UUID id,UUID manager){long chunk=net.minecraft.world.level.ChunkPos.pack(0,0);return new City(id,"Test",0,"minecraft:overworld",new BlockPos(0,64,0).asLong(),chunk,0,manager,manager,Set.of(manager),Set.of(chunk));}
    private static final class Repo implements BuildingRepository {private BuildingRecord value;Repo(BuildingRecord value){this.value=value;}public Collection<BuildingRecord>forCity(UUID id){return value.cityId().equals(id)?List.of(value):List.of();}public Optional<BuildingRecord>byId(UUID id){return value.id().equals(id)?Optional.of(value):Optional.empty();}public void put(BuildingRecord next){value=next;}}
}
