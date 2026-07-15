package com.seaquake6324.civitas.domain.population;

import java.util.UUID;

/** Last server-thread-observed physical position; virtual simulation never advances it. */
public record CitizenLocationRecord(UUID citizenId, UUID cityId, String dimension, long position,
        long updatedAt, long revision) {
    public CitizenLocationRecord {
        if (citizenId == null || cityId == null || dimension == null || !dimension.contains(":")
                || updatedAt < 0 || revision < 1) throw new IllegalArgumentException("invalid citizen location");
    }
}
