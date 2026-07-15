package com.seaquake6324.civitas.domain.building;

import com.seaquake6324.civitas.domain.City;

/** Stable session token for the topology inputs that make a building registration valid. */
public final class CityTopologyToken {
    public static long of(City city) {
        long hash = 0xcbf29ce484222325L;
        hash = mix(hash, city.dimension().hashCode());
        hash = mix(hash, city.corePosition());
        for (long chunk : city.territory().stream().sorted().mapToLong(Long::longValue).toArray()) hash = mix(hash, chunk);
        return hash;
    }
    private static long mix(long hash,long value){hash^=value;return hash*0x100000001b3L;}
    private CityTopologyToken() {}
}
