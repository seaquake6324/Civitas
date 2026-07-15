package com.seaquake6324.civitas.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import com.seaquake6324.civitas.domain.membership.MembershipApplication;
import com.seaquake6324.civitas.domain.territory.TerritoryChunkState;

public final class City {
    private final UUID id;
    private final String name;
    private final int color;
    private final String dimension;
    private final long corePosition;
    private final long coreChunk;
    private final long activatedAt;
    private final UUID founderId;
    private UUID lordId;
    private final Set<UUID> residents;
    private final Set<Long> territory;
    private final Set<Long> heartland;
    private final Map<Long, TerritoryChunkState> territoryStates;
    private final Map<UUID, MembershipApplication> applications;
    private final long lastExpansionAt;
    private final long lastMemberOnlineEpochMillis;
    private final long revision;

    public City(UUID id, String name, int color, String dimension, long corePosition, long coreChunk, long activatedAt,
                UUID founderId, UUID lordId, Set<UUID> residents, Set<Long> territory) {
        this.id = id;
        this.name = name;
        this.color = color & 0xFFFFFF;
        this.dimension = dimension;
        this.corePosition = corePosition;
        this.coreChunk = coreChunk;
        this.activatedAt = activatedAt;
        this.founderId = founderId;
        this.lordId = lordId;
        this.residents = new HashSet<>(residents);
        this.territory = new HashSet<>(territory);
        this.heartland = new HashSet<>(territory);
        this.territoryStates = new HashMap<>();
        for (long chunk : territory) territoryStates.put(chunk, TerritoryChunkState.initial(activatedAt, true));
        this.applications = new HashMap<>();
        this.lastExpansionAt = 0;
        this.lastMemberOnlineEpochMillis = 0;
        this.revision = 0;
    }

    public City(UUID id, String name, int color, String dimension, long corePosition, long coreChunk, long activatedAt,
                UUID founderId, UUID lordId, Set<UUID> residents, Set<Long> territory, Set<Long> heartland,
                Map<Long, TerritoryChunkState> territoryStates, Map<UUID, MembershipApplication> applications,
                long lastExpansionAt) {
        this(id,name,color,dimension,corePosition,coreChunk,activatedAt,founderId,lordId,residents,territory,heartland,territoryStates,applications,lastExpansionAt,0,0);
    }

    public City(UUID id, String name, int color, String dimension, long corePosition, long coreChunk, long activatedAt,
                UUID founderId, UUID lordId, Set<UUID> residents, Set<Long> territory, Set<Long> heartland,
                Map<Long, TerritoryChunkState> territoryStates, Map<UUID, MembershipApplication> applications,
                long lastExpansionAt, long lastMemberOnlineEpochMillis) {
        this(id,name,color,dimension,corePosition,coreChunk,activatedAt,founderId,lordId,residents,territory,heartland,
                territoryStates,applications,lastExpansionAt,lastMemberOnlineEpochMillis,0);
    }

    public City(UUID id, String name, int color, String dimension, long corePosition, long coreChunk, long activatedAt,
                UUID founderId, UUID lordId, Set<UUID> residents, Set<Long> territory, Set<Long> heartland,
                Map<Long, TerritoryChunkState> territoryStates, Map<UUID, MembershipApplication> applications,
                long lastExpansionAt, long lastMemberOnlineEpochMillis, long revision) {
        this.id=id; this.name=name; this.color=color&0xFFFFFF; this.dimension=dimension; this.corePosition=corePosition;
        this.coreChunk=coreChunk; this.activatedAt=activatedAt; this.founderId=founderId; this.lordId=lordId;
        this.residents=new HashSet<>(residents); this.territory=new HashSet<>(territory); this.heartland=new HashSet<>(heartland);
        this.territoryStates=new HashMap<>(territoryStates); this.applications=new HashMap<>(applications);
        this.lastExpansionAt=lastExpansionAt;
        this.lastMemberOnlineEpochMillis=Math.max(0,lastMemberOnlineEpochMillis);
        if(revision<0)throw new IllegalArgumentException("negative city revision");
        this.revision=revision;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public int color() { return color; }
    public String dimension() { return dimension; }
    public long corePosition() { return corePosition; }
    public long coreChunk() { return coreChunk; }
    public long activatedAt() { return activatedAt; }
    public UUID founderId() { return founderId; }
    public UUID lordId() { return lordId; }
    public Set<UUID> residents() { return Collections.unmodifiableSet(residents); }
    public Set<Long> territory() { return Collections.unmodifiableSet(territory); }
    public Set<Long> heartland() { return Collections.unmodifiableSet(heartland); }
    public Map<Long, TerritoryChunkState> territoryStates() { return Collections.unmodifiableMap(territoryStates); }
    public Map<UUID, MembershipApplication> applications() { return Collections.unmodifiableMap(applications); }
    public long lastExpansionAt() { return lastExpansionAt; }
    public long lastMemberOnlineEpochMillis() { return lastMemberOnlineEpochMillis; }
    public long revision() { return revision; }
    public boolean isMember(UUID playerId) { return residents.contains(playerId); }
    public boolean mayManage(UUID playerId) { return founderId.equals(playerId) || lordId.equals(playerId); }
    /** 0.2a contract: founding grants military command; delegation is intentionally deferred. */
    public boolean mayCommandMilitary(UUID playerId) { return founderId.equals(playerId); }
    public boolean ownsChunk(String dimensionId, long chunk) { return dimension.equals(dimensionId) && territory.contains(chunk); }
    public City relocateCore(long newCorePosition, long newCoreChunk) {
        return new City(id, name, color, dimension, newCorePosition, newCoreChunk, activatedAt,
                founderId, lordId, residents, territory, heartland, territoryStates, applications, lastExpansionAt,lastMemberOnlineEpochMillis,revision+1);
    }
    public City updateIdentity(String newName, int newColor) {
        return new City(id, newName, newColor, dimension, corePosition, coreChunk, activatedAt,
                founderId, lordId, residents, territory, heartland, territoryStates, applications, lastExpansionAt,lastMemberOnlineEpochMillis,revision+1);
    }

    public City withMembership(Set<UUID> members, Map<UUID, MembershipApplication> pending) {
        return new City(id,name,color,dimension,corePosition,coreChunk,activatedAt,founderId,lordId,members,territory,heartland,territoryStates,pending,lastExpansionAt,lastMemberOnlineEpochMillis,revision+1);
    }
    public City claim(long chunk, long sourceChunk, long now) {
        Set<Long> claims=new HashSet<>(territory); claims.add(chunk);
        Map<Long,TerritoryChunkState> states=new HashMap<>(territoryStates); states.put(chunk,TerritoryChunkState.expansion(now,sourceChunk));
        return new City(id,name,color,dimension,corePosition,coreChunk,activatedAt,founderId,lordId,residents,claims,heartland,states,applications,now,lastMemberOnlineEpochMillis,revision+1);
    }
    public City retract(long chunk) {
        Set<Long> claims=new HashSet<>(territory); claims.remove(chunk);
        Map<Long,TerritoryChunkState> states=new HashMap<>(territoryStates); states.remove(chunk);
        return new City(id,name,color,dimension,corePosition,coreChunk,activatedAt,founderId,lordId,residents,claims,heartland,states,applications,lastExpansionAt,lastMemberOnlineEpochMillis,revision+1);
    }
    public City withTerritoryState(long chunk,TerritoryChunkState state){
        if(!territory.contains(chunk))return this;Map<Long,TerritoryChunkState> states=new HashMap<>(territoryStates);states.put(chunk,state);
        return new City(id,name,color,dimension,corePosition,coreChunk,activatedAt,founderId,lordId,residents,territory,heartland,states,applications,lastExpansionAt,lastMemberOnlineEpochMillis,revision+1);
    }
    public City withLastMemberOnline(long epochMillis){return new City(id,name,color,dimension,corePosition,coreChunk,activatedAt,founderId,lordId,residents,territory,heartland,territoryStates,applications,lastExpansionAt,epochMillis,revision+1);}
}
