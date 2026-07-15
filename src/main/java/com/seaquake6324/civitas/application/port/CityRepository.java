package com.seaquake6324.civitas.application.port;

import com.seaquake6324.civitas.domain.City;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface CityRepository {
    Collection<City> cities();
    default Optional<City> byId(UUID id){return cities().stream().filter(city->city.id().equals(id)).findFirst();}
    Optional<City> byName(String name);
    Optional<City> cityLedBy(UUID playerId);
    void add(City city);
}
