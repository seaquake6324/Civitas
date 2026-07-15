package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.PopulationRepository;
import com.seaquake6324.civitas.domain.population.Gender;
import com.seaquake6324.civitas.domain.population.PlayerCivitasProfile;
import java.util.UUID;

public final class SelectPlayerGenderService {
    public enum Failure { ALREADY_SELECTED }
    public record Result(PlayerCivitasProfile profile,Failure failure){public boolean success(){return profile!=null;}}
    public Result select(PopulationRepository repository,UUID playerId,Gender gender){
        if(repository.profile(playerId).isPresent())return new Result(null,Failure.ALREADY_SELECTED);
        PlayerCivitasProfile profile=new PlayerCivitasProfile(playerId,gender,1);repository.putProfile(profile);
        return new Result(profile,null);
    }
    public PlayerCivitasProfile adminOverride(PopulationRepository repository,UUID playerId,Gender gender){
        PlayerCivitasProfile profile=repository.profile(playerId).map(existing->existing.withGender(gender))
                .orElseGet(()->new PlayerCivitasProfile(playerId,gender,1));repository.putProfile(profile);return profile;
    }
}
