package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.BuildingRepository;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.building.BuildingRecord;
import com.seaquake6324.civitas.domain.building.BuildingStatus;
import java.util.UUID;

/** Pure authorization rule; the infrastructure adapter supplies fresh main-thread container evidence. */
public final class AuthorizeBuildingStorageService {
    public enum Failure { NONE, NOT_MANAGER, MISSING_BUILDING, WRONG_CITY, STALE_REVISION, BUILDING_NOT_VALID, NOT_SCANNED_ENDPOINT, NOT_CONTAINER }
    public record Result(BuildingRecord building, Failure failure) { public boolean success() { return failure == Failure.NONE; } }

    public Result toggle(BuildingRepository repository, City city, UUID actor, UUID buildingId,
            long expectedRevision, long endpoint, boolean actualContainer) {
        if (!city.mayManage(actor)) return new Result(null, Failure.NOT_MANAGER);
        BuildingRecord building = repository.byId(buildingId).orElse(null);
        if (building == null) return new Result(null, Failure.MISSING_BUILDING);
        if (!building.cityId().equals(city.id())) return new Result(null, Failure.WRONG_CITY);
        if (building.revision() != expectedRevision) return new Result(null, Failure.STALE_REVISION);
        if (building.status() != BuildingStatus.VALID) return new Result(null, Failure.BUILDING_NOT_VALID);
        if (!building.features().storageEndpoints().contains(endpoint)) return new Result(null, Failure.NOT_SCANNED_ENDPOINT);
        if (!actualContainer) return new Result(null, Failure.NOT_CONTAINER);
        BuildingRecord updated = building.authorizeStorage(endpoint, !building.authorizedStorageEndpoints().contains(endpoint));
        repository.put(updated);
        return new Result(updated, Failure.NONE);
    }
}
