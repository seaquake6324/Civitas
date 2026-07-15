package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.domain.population.CitizenIntent;
import com.seaquake6324.civitas.domain.population.CitizenSimulationSnapshot;
import java.util.Optional;

/** Background-safe pure calculation over an immutable domain snapshot. */
public final class PlanCitizenSimulationService {
    private final PopulationAgingService aging=new PopulationAgingService();

    public Optional<CitizenIntent> plan(CitizenSimulationSnapshot snapshot) {
        var citizen=snapshot.citizen();
        if(!citizen.alive())return Optional.empty();
        var result=aging.advance(citizen,snapshot.observedAt(),snapshot.ageRules(),snapshot.lifespanRules());
        return Optional.of(new CitizenIntent(CitizenIntent.Type.AGE_ADVANCE,citizen.id(),citizen.revision(),
                snapshot.cityId(),snapshot.cityRevision(),snapshot.buildingRevisions(),snapshot.observedAt(),
                snapshot.ageRules(),snapshot.lifespanRules(),result.citizen(),result.before().stage(),result.after().stage(),result.died()));
    }
}
