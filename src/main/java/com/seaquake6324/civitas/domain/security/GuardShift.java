package com.seaquake6324.civitas.domain.security;

/** Fixed 0.2a duty shifts expressed in vanilla day time. */
public enum GuardShift {
    DAY, NIGHT;

    public boolean onDuty(long dayTime) {
        long time = Math.floorMod(dayTime, 24_000L);
        return this == DAY ? time < 12_000L : time >= 12_000L;
    }
}
