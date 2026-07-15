package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.domain.City;
import java.util.UUID;

/** Pure core-relocation rules; Minecraft placement and block mutation remain infrastructure concerns. */
public final class MoveCityCoreService {
    public Result move(Request request) {
        City city = request.city();
        if (!city.mayManage(request.playerId())) return Result.failure("civitas.core_move.not_manager");
        if (!city.id().equals(request.coreCityId()) || city.corePosition() != request.originalPosition())
            return Result.failure("civitas.core_move.stale_core");
        if (!city.dimension().equals(request.dimension())) return Result.failure("civitas.core_move.wrong_dimension");
        if (request.originalPosition() == request.targetPosition()) return Result.failure("civitas.core_move.same_position");
        if (!city.ownsChunk(request.dimension(), request.targetChunk())) return Result.failure("civitas.core_move.outside_city");
        if (!request.targetReplaceable()) return Result.failure("civitas.core_move.blocked");
        if (!request.hasSupport()) return Result.failure("civitas.core_move.no_support");
        return Result.success(city.relocateCore(request.targetPosition(), request.targetChunk()));
    }

    public record Request(City city, UUID playerId, UUID coreCityId, String dimension,
            long originalPosition, long targetPosition, long targetChunk,
            boolean targetReplaceable, boolean hasSupport) {}

    public record Result(boolean success, City city, String errorKey) {
        static Result success(City city) { return new Result(true, city, ""); }
        static Result failure(String errorKey) { return new Result(false, null, errorKey); }
    }
}
