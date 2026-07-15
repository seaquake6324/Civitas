package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.PopulationRepository;
import com.seaquake6324.civitas.domain.population.CitizenRecord;
import com.seaquake6324.civitas.domain.population.Gender;
import java.util.UUID;

public final class ChangeCitizenGenderService {
    public CitizenRecord adminOverride(PopulationRepository repository,UUID citizenId,Gender gender){
        CitizenRecord updated=repository.citizen(citizenId).orElseThrow().withGender(gender);repository.putCitizen(updated);return updated;
    }
}
