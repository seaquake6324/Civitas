package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.BuildingRepository;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.building.BuildingRecord;
import com.seaquake6324.civitas.domain.building.BuildingStatus;
import com.seaquake6324.civitas.domain.building.BuildingValidation;

/** Re-evaluates a stale record from fresh main-thread evidence without changing its identity. */
public final class RevalidateBuildingService {
    public record Result(BuildingRecord building, BuildingValidation.Result validation) {}

    public Result revalidate(BuildingRepository repository, City city, BuildingRecord original,
            long now, BuildingValidation.Evidence evidence) {
        boolean overlap = repository.overlaps(city.id(), original.dimension(), evidence.cells(), original.id());
        BuildingValidation.Result validation = BuildingValidation.evaluate(original.purpose(), evidence,
                true, city.dimension().equals(original.dimension()), overlap);
        String reason = validation.valid() ? "" : validation.failures().getFirst().name().toLowerCase(java.util.Locale.ROOT);
        BuildingRecord updated = new BuildingRecord(original.id(), original.cityId(), original.dimension(),
                original.purpose(), original.entrance(), original.interior(),
                validation.valid() ? evidence.cells() : original.cells(),
                validation.valid() ? evidence.facilities() : original.facilities(),
                validation.valid() ? evidence.features() : original.features(),
                validation.valid() ? validation.capacity() : 0,
                validation.valid() ? BuildingStatus.VALID : BuildingStatus.INVALID,
                original.revision() + 1, validation.valid() ? now : original.validatedAt(), reason,
                validation.valid() ? original.authorizedStorageEndpoints().stream()
                        .filter(evidence.features().storageEndpoints()::contains).collect(java.util.stream.Collectors.toSet())
                        : original.authorizedStorageEndpoints());
        repository.put(updated);
        return new Result(updated, validation);
    }
}
