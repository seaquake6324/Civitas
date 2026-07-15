package com.seaquake6324.civitas.infrastructure.civilization;

import com.seaquake6324.civitas.domain.civilization.CivilizationLayer;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToLongFunction;

/** Exact-key deduplicating queue. Overflow is surfaced to the caller for progressive recovery. */
public final class CivilityDirtyQueue {
    public static final int DEFAULT_CAPACITY = 4096;
    private final int capacity;
    private final Map<Key, Task> tasks = new HashMap<>();

    public CivilityDirtyQueue() { this(DEFAULT_CAPACITY); }
    public CivilityDirtyQueue(int capacity) { this.capacity = Math.max(1, capacity); }

    public boolean offer(Key key, long now, Reason reason) {
        Task current = tasks.get(key);
        if (current != null) {
            current.reasons |= 1 << reason.ordinal();
            return true;
        }
        if (tasks.size() >= capacity) return false;
        tasks.put(key, new Task(key, now, 1 << reason.ordinal(), 0));
        return true;
    }

    public Optional<Task> poll(ToLongFunction<Key> playerDistanceSquared) {
        Task selected = tasks.values().stream().min(Comparator
                .comparingLong((Task task) -> playerDistanceSquared.applyAsLong(task.key))
                .thenComparingLong(task -> task.createdAt)).orElse(null);
        if (selected != null) tasks.remove(selected.key);
        return Optional.ofNullable(selected);
    }

    public void defer(Task task) {
        task.deferrals++;
        tasks.putIfAbsent(task.key, task);
    }
    public int size() { return tasks.size(); }
    public long oldestAge(long now) { return tasks.values().stream().mapToLong(t -> now - t.createdAt).max().orElse(0); }
    public Collection<Task> tasks() { return java.util.List.copyOf(tasks.values()); }

    public record Key(String dimension, long chunk, CivilizationLayer layer) {}
    public enum Reason { BLOCK_CHANGE, FOUNDING, BORDER_CHANGE, MIGRATION_RESCAN, DEFERRED_UNLOADED, ADMIN_RESCAN }
    public static final class Task {
        private final Key key;
        private final long createdAt;
        private int reasons;
        private int deferrals;
        private Task(Key key, long createdAt, int reasons, int deferrals) {
            this.key = key; this.createdAt = createdAt; this.reasons = reasons; this.deferrals = deferrals;
        }
        public Key key() { return key; }
        public long createdAt() { return createdAt; }
        public int reasons() { return reasons; }
        public int deferrals() { return deferrals; }
    }
}
