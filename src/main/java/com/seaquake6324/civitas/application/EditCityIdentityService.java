package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.CityRepository;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.CityName;
import java.util.UUID;

public final class EditCityIdentityService {
    public Result edit(CityRepository cities, Request request) {
        City city = request.city();
        if (!city.mayManage(request.playerId())) return Result.failure("civitas.core_manage.not_manager");
        if (!city.id().equals(request.coreCityId()) || city.corePosition() != request.corePosition())
            return Result.failure("civitas.core_manage.stale_core");
        if (request.distanceSquared() > 64.0D) return Result.failure("civitas.error.too_far");
        CityName.Validation name = CityName.validate(request.requestedName());
        if (!name.valid()) return Result.failure(name.errorKey());
        if (cities.byName(name.normalized()).filter(existing -> !existing.id().equals(city.id())).isPresent())
            return Result.failure("civitas.error.name_taken");
        City updated = city.updateIdentity(name.normalized(), request.requestedColor());
        cities.add(updated);
        return Result.success(updated);
    }

    public record Request(City city, UUID playerId, UUID coreCityId, long corePosition,
                          double distanceSquared, String requestedName, int requestedColor) {}
    public record Result(boolean success, String errorKey, City city) {
        static Result success(City city) { return new Result(true, "", city); }
        static Result failure(String key) { return new Result(false, key, null); }
    }
}
