package com.seaquake6324.civitas.domain.security;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** Persistent authority created only after the first invasion mob enters the world. */
public record InvasionCommitment(UUID invasionId, UUID cityId, InfiltrationSource source, long location,
        int wave, List<UUID> survivingEnemyIds, List<Participant> participants, long committedAt,
        long lastRoundAt, int round, long revision) {
    public static final int HARD_ENEMY_CAP = 512, HARD_PARTICIPANT_CAP = 128;
    public InvasionCommitment {
        if (invasionId == null || cityId == null || source == null || wave < 1 || survivingEnemyIds == null
                || survivingEnemyIds.size() > HARD_ENEMY_CAP || new HashSet<>(survivingEnemyIds).size() != survivingEnemyIds.size()
                || participants == null || participants.size() > HARD_PARTICIPANT_CAP || committedAt < 0
                || lastRoundAt < committedAt || round < 0 || revision < 1) throw new IllegalArgumentException("invalid invasion commitment");
        survivingEnemyIds = List.copyOf(survivingEnemyIds); participants = List.copyOf(participants);
        if (new HashSet<>(participants.stream().map(Participant::citizenId).toList()).size() != participants.size())
            throw new IllegalArgumentException("duplicate invasion participant");
    }
    public boolean survivingEnemy(UUID id) { return survivingEnemyIds.contains(id); }
    public InvasionCommitment nextRound(int defeated, long now) {
        int remove = Math.max(0, Math.min(defeated, survivingEnemyIds.size()));
        return new InvasionCommitment(invasionId, cityId, source, location, wave,
                survivingEnemyIds.subList(remove, survivingEnemyIds.size()), participants, committedAt,
                Math.max(lastRoundAt, now), round + 1, revision + 1);
    }
    public InvasionCommitment nextWave(int nextWave, List<UUID> enemies, long now) {
        return new InvasionCommitment(invasionId, cityId, source, location, nextWave, enemies,
                participants, committedAt, Math.max(lastRoundAt, now), round, revision + 1);
    }
    public InvasionCommitment enemyDied(UUID enemyId, long now) {
        if (!survivingEnemyIds.contains(enemyId)) return this;
        java.util.ArrayList<UUID> next = new java.util.ArrayList<>(survivingEnemyIds); next.remove(enemyId);
        return new InvasionCommitment(invasionId, cityId, source, location, wave, next, participants,
                committedAt, Math.max(lastRoundAt, now), round, revision + 1);
    }
    public record Participant(UUID citizenId, Role role, int distanceBlocks) {
        public Participant { if (citizenId == null || role == null || distanceBlocks < 0) throw new IllegalArgumentException("invalid participant"); }
    }
    public enum Role { GUARD, CIVILIAN }
}
