package com.seaquake6324.civitas.infrastructure.persistence;

import com.seaquake6324.civitas.application.port.CityRepository;
import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.CityName;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import com.seaquake6324.civitas.domain.membership.MembershipApplication;
import com.seaquake6324.civitas.domain.territory.NeglectStage;
import com.seaquake6324.civitas.domain.territory.TerritoryChunkState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;

public final class CitySavedData extends SavedData implements CityRepository {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int SCHEMA_VERSION = 5;
    private static final Codec<CitySavedData> CODEC = CompoundTag.CODEC.xmap(CitySavedData::load, CitySavedData::saveTag);
    private static final SavedDataType<CitySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("civitas", "cities"), CitySavedData::new, CODEC);
    private final NavigableMap<UUID, City> cities = new TreeMap<>();
    private final Map<UUID,UUID> memberCityIndex=new HashMap<>();
    private final Map<String,Map<Long,UUID>>territoryIndex=new HashMap<>();
    private boolean readOnly;private CompoundTag protectedRoot;private String migrationResult="none";private int quarantinedCities;

    public static CitySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Collection<City> cities() { return java.util.List.copyOf(cities.values()); }
    public CityBatchAfter cityBatchAfter(UUID after,int limit){int cap=Math.max(0,limit);java.util.ArrayList<City>out=new java.util.ArrayList<>(Math.min(cap,cities.size()));if(cap>0){for(var entry:(after==null?cities:cities.tailMap(after,false)).entrySet()){out.add(entry.getValue());if(out.size()>=cap)break;}}UUID next=out.isEmpty()?after:out.getLast().id();boolean exhausted=next==null||cities.higherKey(next)==null;return new CityBatchAfter(out,next,exhausted,out.size());}
    public record CityBatchAfter(java.util.List<City>cities,UUID nextCursor,boolean exhausted,int examined){public CityBatchAfter{cities=java.util.List.copyOf(cities);}}
    public CityBatch cityBatch(UUID after,int limit){int cap=Math.max(0,limit);if(cap==0||cities.isEmpty())return new CityBatch(java.util.List.of(),after,false,0);java.util.ArrayList<City>out=new java.util.ArrayList<>(Math.min(cap,cities.size()));UUID key=after==null?cities.firstKey():cities.higherKey(after);boolean wrapped=false;if(key==null){key=cities.firstKey();wrapped=true;}int examined=0,maximum=Math.min(cap,cities.size());while(out.size()<maximum){City city=cities.get(key);if(city!=null)out.add(city);examined++;UUID next=cities.higherKey(key);if(next==null){wrapped=true;if(out.size()<maximum)next=cities.firstKey();}key=next;}return new CityBatch(out,out.getLast().id(),wrapped,examined);}
    public record CityBatch(java.util.List<City>cities,UUID nextCursor,boolean wrapped,int examined){public CityBatch{cities=java.util.List.copyOf(cities);}}
    public Optional<City> byId(UUID id) { return id==null?Optional.empty():Optional.ofNullable(cities.get(id)); }
    public Optional<City> byName(String name) {
        String key = CityName.uniquenessKey(name);
        return cities.values().stream().filter(city -> CityName.uniquenessKey(city.name()).equals(key)).findFirst();
    }
    public Optional<City> cityAt(String dimension, long chunk) {
        UUID id=territoryIndex.getOrDefault(dimension,Map.of()).get(chunk);return id==null?Optional.empty():Optional.ofNullable(cities.get(id));
    }
    public Optional<City> cityForMember(UUID player) {
        UUID cityId=memberCityIndex.get(player);return cityId==null?Optional.empty():Optional.ofNullable(cities.get(cityId));
    }
    public Optional<City> cityLedBy(UUID player) {
        return cities.values().stream().filter(city -> city.founderId().equals(player) || city.lordId().equals(player)).findFirst();
    }
    public void add(City city) { if(readOnly)return;City previous=cities.get(city.id());if(previous!=null&&city.revision()<=previous.revision())throw new IllegalArgumentException("city revision must increase");validateTerritoryIndex(city);if(previous!=null){unindexTerritory(previous);for(UUID member:previous.residents())memberCityIndex.remove(member,previous.id());}cities.put(city.id(),city);indexTerritory(city);for(UUID member:city.residents())memberCityIndex.put(member,city.id());setDirty(); }
    public Optional<City> remove(UUID cityId) {
        if(readOnly)return Optional.empty();City removed = cities.remove(cityId);if(removed!=null){unindexTerritory(removed);for(UUID member:removed.residents())memberCityIndex.remove(member,removed.id());}
        if (removed != null) setDirty();
        return Optional.ofNullable(removed);
    }

    CompoundTag saveTag() {
        if(readOnly&&protectedRoot!=null)return protectedRoot.copy();
        CompoundTag root = new CompoundTag();
        root.putInt("SchemaVersion", SCHEMA_VERSION);
        root.putString("MigrationResult",migrationResult);
        root.putInt("QuarantinedCities",quarantinedCities);
        ListTag cityList = new ListTag();
        for (City city : cities.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Id", city.id().toString());
            tag.putString("Name", city.name());
            tag.putInt("Color", city.color());
            tag.putString("Dimension", city.dimension());
            tag.putLong("CorePosition", city.corePosition());
            tag.putLong("CoreChunk", city.coreChunk());
            tag.putLong("ActivatedAt", city.activatedAt());
            tag.putString("FounderId", city.founderId().toString());
            tag.putString("LordId", city.lordId().toString());
            ListTag residents = new ListTag();
            for (UUID resident : city.residents()) {
                CompoundTag residentTag = new CompoundTag();
                residentTag.putString("Id", resident.toString());
                residents.add(residentTag);
            }
            tag.put("Residents", residents);
            tag.putLongArray("Territory", city.territory().stream().mapToLong(Long::longValue).toArray());
            tag.putLongArray("Heartland", city.heartland().stream().mapToLong(Long::longValue).toArray());
            tag.putLong("LastExpansionAt", city.lastExpansionAt());
            tag.putLong("LastMemberOnlineEpochMillis",city.lastMemberOnlineEpochMillis());
            tag.putLong("Revision",city.revision());
            ListTag territoryStates = new ListTag();
            for (var entry : city.territoryStates().entrySet()) {
                TerritoryChunkState state = entry.getValue();
                CompoundTag stateTag = new CompoundTag();
                stateTag.putLong("Chunk", entry.getKey()); stateTag.putLong("ClaimedAt", state.claimedAt());
                if (state.sourceChunk() != null) stateTag.putLong("SourceChunk", state.sourceChunk());
                stateTag.putLong("LastHealthyAt", state.lastHealthyAt()); stateTag.putLong("NeglectStartedAt", state.neglectStartedAt());
                stateTag.putString("NeglectStage", state.neglectStage().name()); stateTag.putLong("StageChangedAt", state.stageChangedAt());
                stateTag.putLong("RecoveryStartedAt", state.recoveryStartedAt()); stateTag.putString("LastDefenseResult", state.lastDefenseResult().name());
                stateTag.putLong("LastDefenseAt", state.lastDefenseAt()); stateTag.putBoolean("PermanentHeartland", state.permanentHeartland());
                territoryStates.add(stateTag);
            }
            tag.put("TerritoryStates", territoryStates);
            ListTag applications = new ListTag();
            for (MembershipApplication application : city.applications().values()) {
                CompoundTag app = new CompoundTag(); app.putString("PlayerId", application.playerId().toString());
                app.putLong("SubmittedAt", application.submittedAt()); app.putString("Status", application.status().name()); applications.add(app);
            }
            tag.put("MembershipApplications", applications);
            cityList.add(tag);
        }
        root.put("Cities", cityList);
        return root;
    }

    static CitySavedData load(CompoundTag root) {
        int schemaVersion = root.getIntOr("SchemaVersion", 0);
        CitySavedData data = new CitySavedData();
        data.quarantinedCities=Math.max(0,root.getIntOr("QuarantinedCities",0));
        if (schemaVersion > SCHEMA_VERSION) {
            LOGGER.warn("Loading Civitas city data from newer schema {}; known schema is {}", schemaVersion, SCHEMA_VERSION);
            data.readOnly=true;data.protectedRoot=root.copy();data.migrationResult="newer_schema_"+schemaVersion+"_read_only";
        }else if(schemaVersion<0){data.readOnly=true;data.protectedRoot=root.copy();data.migrationResult="invalid_schema_read_only";LOGGER.warn("Protecting invalid Civitas city schema {} as read-only",schemaVersion);}
        else if(schemaVersion<SCHEMA_VERSION){data.migrationResult="v"+schemaVersion+"_to_v"+SCHEMA_VERSION;LOGGER.info("Migrating Civitas city data {}",data.migrationResult);}
        else{
            data.migrationResult=root.getStringOr("MigrationResult","none");
        }
        ListTag list = root.getListOrEmpty("Cities");
        for (Tag raw : list) {
            if (!(raw instanceof CompoundTag tag)) {
                LOGGER.warn("Skipping malformed Civitas city record: expected a compound tag");
                continue;
            }
            try {
                loadCity(tag,schemaVersion).ifPresent(city -> {if(data.cities.containsKey(city.id()))throw new IllegalArgumentException("duplicate city id");data.validateTerritoryIndex(city);data.cities.put(city.id(),city);data.indexTerritory(city);for(UUID member:city.residents())data.memberCityIndex.putIfAbsent(member,city.id());});
            } catch (RuntimeException exception) {
                data.quarantinedCities++;
                LOGGER.warn("Skipping malformed Civitas city record", exception);
            }
        }
        return data;
    }
    private void validateTerritoryIndex(City city){Map<Long,UUID>dimension=territoryIndex.getOrDefault(city.dimension(),Map.of());for(long chunk:city.territory()){UUID owner=dimension.get(chunk);if(owner!=null&&!owner.equals(city.id()))throw new IllegalArgumentException("overlapping city territory index");}}
    private void indexTerritory(City city){Map<Long,UUID>dimension=territoryIndex.computeIfAbsent(city.dimension(),ignored->new HashMap<>());for(long chunk:city.territory())dimension.put(chunk,city.id());}
    private void unindexTerritory(City city){Map<Long,UUID>dimension=territoryIndex.get(city.dimension());if(dimension==null)return;for(long chunk:city.territory())dimension.remove(chunk,city.id());if(dimension.isEmpty())territoryIndex.remove(city.dimension());}
    public boolean readOnly(){return readOnly;}public String migrationResult(){return migrationResult;}public int quarantinedCities(){return quarantinedCities;}

    private static Optional<City> loadCity(CompoundTag tag,int schemaVersion) {
        Optional<UUID> id = readUuid(tag, "Id");
        Optional<UUID> founderId = readUuid(tag, "FounderId");
        Optional<UUID> lordId = readUuid(tag, "LordId");
        if (id.isEmpty() || founderId.isEmpty() || lordId.isEmpty()) {
            LOGGER.warn("Skipping Civitas city record with a missing or invalid required UUID");
            return Optional.empty();
        }
        Set<UUID> residents = new HashSet<>();
        for (Tag residentRaw : tag.getListOrEmpty("Residents")) {
            if (residentRaw instanceof CompoundTag residentTag) {
                readUuid(residentTag, "Id").ifPresent(residents::add);
            }
        }
        Set<Long> territory = new HashSet<>();
        for (long chunk : tag.getLongArray("Territory").orElseGet(() -> new long[0])) territory.add(chunk);
        long corePosition = tag.getLongOr("CorePosition", 0L);
        long coreChunk = tag.getLong("CoreChunk").orElseGet(() -> chunkFromBlockPosition(corePosition));
        Set<Long> heartland = new HashSet<>();
        for (long chunk : tag.getLongArray("Heartland").orElseGet(() -> new long[0])) heartland.add(chunk);
        if (heartland.isEmpty()) heartland.addAll(territory);
        Map<Long, TerritoryChunkState> states = new HashMap<>();
        for (Tag rawState : tag.getListOrEmpty("TerritoryStates")) if (rawState instanceof CompoundTag s) {
            long chunk=s.getLongOr("Chunk",Long.MIN_VALUE); if(!territory.contains(chunk)) continue;
            states.put(chunk,new TerritoryChunkState(s.getLongOr("ClaimedAt",tag.getLongOr("ActivatedAt",0L)),
                    s.getLong("SourceChunk").isPresent()?s.getLongOr("SourceChunk",0L):null,
                    s.getLongOr("LastHealthyAt",tag.getLongOr("ActivatedAt",0L)),s.getLongOr("NeglectStartedAt",0L),
                    enumValue(NeglectStage.class,s.getStringOr("NeglectStage","HEALTHY"),NeglectStage.HEALTHY),
                    s.getLongOr("StageChangedAt",tag.getLongOr("ActivatedAt",0L)),s.getLongOr("RecoveryStartedAt",0L),
                    enumValue(TerritoryChunkState.DefenseResult.class,s.getStringOr("LastDefenseResult","NONE"),TerritoryChunkState.DefenseResult.NONE),
                    s.getLongOr("LastDefenseAt",0L),heartland.contains(chunk)));
        }
        for(long chunk:territory)states.putIfAbsent(chunk,TerritoryChunkState.initial(tag.getLongOr("ActivatedAt",0L),heartland.contains(chunk)));
        Map<UUID,MembershipApplication> applications=new HashMap<>();
        for(Tag rawApp:tag.getListOrEmpty("MembershipApplications"))if(rawApp instanceof CompoundTag a)readUuid(a,"PlayerId").ifPresent(player->applications.put(player,
                new MembershipApplication(player,a.getLongOr("SubmittedAt",0L),enumValue(MembershipApplication.Status.class,a.getStringOr("Status","PENDING"),MembershipApplication.Status.PENDING))));
        City city = new City(id.get(), tag.getStringOr("Name", ""), tag.getIntOr("Color", 0),
                tag.getStringOr("Dimension", "minecraft:overworld"), corePosition, coreChunk,
                tag.getLongOr("ActivatedAt", 0L), founderId.get(), lordId.get(), residents, territory, heartland, states,
                applications, tag.getLongOr("LastExpansionAt",0L),tag.getLongOr("LastMemberOnlineEpochMillis",0L),
                schemaVersion>=5?tag.getLongOr("Revision",0L):0L);
        return Optional.of(city);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type,String value,E fallback){try{return Enum.valueOf(type,value);}catch(IllegalArgumentException ex){return fallback;}}

    private static Optional<UUID> readUuid(CompoundTag tag, String key) {
        String value = tag.getStringOr(key, "");
        if (value.isEmpty()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static long chunkFromBlockPosition(long packedBlockPosition) {
        int blockX = (int)(packedBlockPosition >> 38);
        int blockZ = (int)(packedBlockPosition << 26 >> 38);
        return ChunkCoordinate.pack(Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16));
    }
}
