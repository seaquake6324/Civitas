package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.BuildingRepository;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.building.BuildingPurpose;
import com.seaquake6324.civitas.domain.building.BuildingRecord;
import com.seaquake6324.civitas.domain.building.BuildingStatus;
import com.seaquake6324.civitas.domain.building.BuildingValidation;
import com.seaquake6324.civitas.domain.building.CityTopologyToken;
import java.util.UUID;

public final class RegisterBuildingService {
    public record Request(City city, UUID actor, String dimension, BuildingPurpose purpose,
            long entrance, long interior, long cityRevision, long now, BuildingValidation.Evidence evidence) {}
    public record Result(BuildingValidation.Result validation, BuildingRecord building) {
        public boolean success() { return building != null; }
    }

    public Result register(BuildingRepository repository, Request request) {
        boolean overlaps = repository.overlaps(request.city().id(), request.dimension(), request.evidence().cells(), null);
        BuildingValidation.Result validation = BuildingValidation.evaluate(request.purpose(), request.evidence(),
                request.city().mayManage(request.actor()), request.city().dimension().equals(request.dimension()), overlaps);
        if (request.cityRevision() != CityTopologyToken.of(request.city())) {
            var failures = new java.util.ArrayList<>(validation.failures());
            failures.add(BuildingValidation.Failure.STALE_CITY);
            validation = new BuildingValidation.Result(failures, 0, validation.evidence(),
                    validation.requirement(), validation.facilityCount(),
                    validation.facilityCapacity(), validation.spaceCapacity());
        }
        if (!validation.valid()) return new Result(validation, null);
        BuildingRecord building = new BuildingRecord(UUID.randomUUID(), request.city().id(), request.dimension(),
                request.purpose(), request.entrance(), request.interior(), request.evidence().cells(),
                request.evidence().facilities(), request.evidence().features(), validation.capacity(), BuildingStatus.VALID, 1,
                request.now(), "");
        repository.put(building);
        return new Result(validation, building);
    }
}
