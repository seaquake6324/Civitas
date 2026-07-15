package com.seaquake6324.civitas.infrastructure.activity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small insertion-ordered cache that evicts oldest transient windows at a fixed capacity. */
final class BoundedActivityWindowCache<K> {
    private final int capacity;
    private final LinkedHashMap<K, com.seaquake6324.civitas.domain.civilization.ActivityWindowEvidence> values = new LinkedHashMap<>();

    BoundedActivityWindowCache(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    com.seaquake6324.civitas.domain.civilization.ActivityWindowEvidence get(K key) { return values.get(key); }
    com.seaquake6324.civitas.domain.civilization.ActivityWindowEvidence getOrDefault(K key,
            com.seaquake6324.civitas.domain.civilization.ActivityWindowEvidence fallback) {
        return values.getOrDefault(key, fallback);
    }
    void put(K key, com.seaquake6324.civitas.domain.civilization.ActivityWindowEvidence value) {
        values.put(key, value);
        while (values.size() > capacity) values.remove(values.keySet().iterator().next());
    }
    void remove(K key) { values.remove(key); }
    int size() { return values.size(); }
    List<Map.Entry<K, com.seaquake6324.civitas.domain.civilization.ActivityWindowEvidence>> entries() {
        return new ArrayList<>(values.entrySet());
    }
}
