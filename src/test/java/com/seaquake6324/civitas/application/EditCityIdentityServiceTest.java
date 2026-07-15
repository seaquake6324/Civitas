package com.seaquake6324.civitas.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.seaquake6324.civitas.application.port.CityRepository;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.CityName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditCityIdentityServiceTest {
    private static final UUID LORD = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final EditCityIdentityService service = new EditCityIdentityService();

    @Test void renamesAndRecolorsManagedCity() {
        Repository repository = new Repository();
        City city = city("Old", LORD);
        repository.add(city);
        EditCityIdentityService.Result result = service.edit(repository,
                new EditCityIdentityService.Request(city, LORD, city.id(), city.corePosition(), 4, " New Name ", 0xFF79B9));
        assertTrue(result.success());
        assertEquals("New Name", result.city().name());
        assertEquals(0xFF79B9, result.city().color());
        assertEquals(1, repository.cities.size());
    }

    @Test void rejectsDuplicateNameAndNonManager() {
        Repository repository = new Repository();
        City city = city("Old", LORD);
        repository.add(city);
        repository.add(city("Taken", UUID.randomUUID()));
        assertEquals("civitas.error.name_taken", service.edit(repository,
                new EditCityIdentityService.Request(city, LORD, city.id(), city.corePosition(), 4, "taken", 0)).errorKey());
        assertEquals("civitas.core_manage.not_manager", service.edit(repository,
                new EditCityIdentityService.Request(city, UUID.randomUUID(), city.id(), city.corePosition(), 4, "Valid", 0)).errorKey());
    }

    private static City city(String name, UUID lord) {
        return new City(UUID.randomUUID(), name, 0x123456, "minecraft:overworld", 10, 0, 1,
                lord, lord, Set.of(lord), Set.of(0L));
    }
    private static final class Repository implements CityRepository {
        private final List<City> cities = new ArrayList<>();
        public Collection<City> cities() { return cities; }
        public Optional<City> byName(String name) { String key = CityName.uniquenessKey(name); return cities.stream().filter(c -> CityName.uniquenessKey(c.name()).equals(key)).findFirst(); }
        public Optional<City> cityLedBy(UUID playerId) { return Optional.empty(); }
        public void add(City city) { cities.removeIf(existing -> existing.id().equals(city.id())); cities.add(city); }
    }
}
