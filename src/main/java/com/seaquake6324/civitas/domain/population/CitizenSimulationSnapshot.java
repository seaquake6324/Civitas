package com.seaquake6324.civitas.domain.population;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable, Minecraft-free input captured on the server thread for pure background calculation. */
public record CitizenSimulationSnapshot(CitizenRecord citizen, UUID cityId, long cityRevision,
        List<BuildingRevision> buildingRevisions, long observedAt, AgeRules ageRules, LifespanRules lifespanRules) {
    public CitizenSimulationSnapshot {
        Objects.requireNonNull(citizen);
        Objects.requireNonNull(cityId);
        Objects.requireNonNull(ageRules);
        Objects.requireNonNull(lifespanRules);
        buildingRevisions=List.copyOf(buildingRevisions);
        if(!cityId.equals(citizen.cityId())||cityRevision<0||observedAt<0||buildingRevisions.size()>2)
            throw new IllegalArgumentException("invalid citizen simulation snapshot");
        HashSet<UUID> ids=new HashSet<>();
        for(BuildingRevision building:buildingRevisions)if(!ids.add(building.id()))
            throw new IllegalArgumentException("duplicate building snapshot");
    }

    public record BuildingRevision(UUID id,boolean present,long revision) {
        public BuildingRevision(UUID id,long revision){this(id,true,revision);}
        public static BuildingRevision missing(UUID id){return new BuildingRevision(id,false,0);}
        public BuildingRevision { Objects.requireNonNull(id);if(revision<0||(!present&&revision!=0))throw new IllegalArgumentException("invalid building observation"); }
    }
}
