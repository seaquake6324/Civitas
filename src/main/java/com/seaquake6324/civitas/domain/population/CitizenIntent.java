package com.seaquake6324.civitas.domain.population;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Pure-calculation result. It has no authority until the server thread revalidates every revision. */
public record CitizenIntent(Type type, UUID citizenId, long expectedCitizenRevision, UUID cityId,
        long expectedCityRevision, List<CitizenSimulationSnapshot.BuildingRevision> expectedBuildings,
        long snapshotAt, AgeRules ageRules, LifespanRules lifespanRules, CitizenRecord replacement,
        AgeStage beforeStage, AgeStage afterStage, boolean permanentDeath) {
    public CitizenIntent {
        Objects.requireNonNull(type);Objects.requireNonNull(citizenId);Objects.requireNonNull(cityId);
        Objects.requireNonNull(ageRules);Objects.requireNonNull(lifespanRules);Objects.requireNonNull(replacement);
        Objects.requireNonNull(beforeStage);Objects.requireNonNull(afterStage);
        expectedBuildings=List.copyOf(expectedBuildings);
        if(expectedCitizenRevision<0||expectedCityRevision<0||snapshotAt<0||!citizenId.equals(replacement.id())
                ||replacement.revision()<=expectedCitizenRevision)
            throw new IllegalArgumentException("invalid citizen intent");
    }
    public enum Type { AGE_ADVANCE }
}
