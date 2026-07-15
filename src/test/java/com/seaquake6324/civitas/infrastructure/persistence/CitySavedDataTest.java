package com.seaquake6324.civitas.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.membership.MembershipApplication;
import com.seaquake6324.civitas.domain.territory.TerritoryChunkState;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class CitySavedDataTest {
    @Test void nullCityLookupFailsClosed(){assertTrue(new CitySavedData().byId(null).isEmpty());}

    @Test
    void writesExplicitSchemaVersionAndStillLoadsVersionZeroRoots() {
        assertEquals(5, new CitySavedData().saveTag().getIntOr("SchemaVersion", -1));

        CompoundTag legacyRoot = new CompoundTag();
        ListTag cities = new ListTag();
        cities.add(validCity("Version Zero"));
        legacyRoot.put("Cities", cities);

        assertTrue(CitySavedData.load(legacyRoot).byName("Version Zero").isPresent());
    }

    @Test
    void skipsMalformedCityWithoutDroppingValidRecords() {
        CompoundTag root = new CompoundTag();
        ListTag cities = new ListTag();
        CompoundTag malformed = validCity("Broken");
        malformed.putString("Id", "not-a-uuid");
        cities.add(malformed);
        cities.add(validCity("Valid"));
        root.put("Cities", cities);

        CitySavedData loaded = CitySavedData.load(root);

        assertEquals(1, loaded.cities().size());
        assertTrue(loaded.byName("Valid").isPresent());
    }

    @Test
    void migratesCoreChunkFromLegacyBlockPosition() {
        CompoundTag root = new CompoundTag();
        ListTag cities = new ListTag();
        CompoundTag legacy = validCity("Legacy");
        legacy.remove("CoreChunk");
        long packedBlock = ((long)-33 & 0x3FFFFFFL) << 38 | ((long)49 & 0x3FFFFFFL) << 12 | 64L;
        legacy.putLong("CorePosition", packedBlock);
        cities.add(legacy);
        root.put("Cities", cities);

        CitySavedData loaded = CitySavedData.load(root);

        assertEquals(new ChunkCoordinate(-3, 3), ChunkCoordinate.unpack(loaded.byName("Legacy").orElseThrow().coreChunk()));
    }

    @Test void roundTripsHeartlandClaimSourceApplicationsAndOfflineAnchor(){
        UUID leader=UUID.randomUUID(),applicant=UUID.randomUUID();long core=ChunkCoordinate.pack(0,0),claim=ChunkCoordinate.pack(1,0);
        City city=new City(UUID.randomUUID(),"Round Trip",0x123456,"minecraft:overworld",0,core,10,leader,leader,Set.of(leader),Set.of(core,claim),Set.of(core),Map.of(core,TerritoryChunkState.initial(10,true),claim,TerritoryChunkState.expansion(20,core)),Map.of(applicant,new MembershipApplication(applicant,30,MembershipApplication.Status.PENDING)),20,123456L);
        CitySavedData data=new CitySavedData();data.add(city);City loaded=CitySavedData.load(data.saveTag()).byId(city.id()).orElseThrow();
        assertEquals(Set.of(core),loaded.heartland());assertEquals(core,loaded.territoryStates().get(claim).sourceChunk());assertTrue(loaded.applications().containsKey(applicant));assertEquals(123456L,loaded.lastMemberOnlineEpochMillis());
    }

    @Test void newerSchemaIsProtectedFromDownLevelWrites(){CompoundTag root=new CompoundTag();root.putInt("SchemaVersion",99);root.put("Cities",new ListTag());CitySavedData loaded=CitySavedData.load(root);assertTrue(loaded.readOnly());loaded.add(new City(UUID.randomUUID(),"Ignored",0,"minecraft:overworld",0,0,0,UUID.randomUUID(),UUID.randomUUID(),Set.of(),Set.of()));assertEquals(99,loaded.saveTag().getIntOr("SchemaVersion",0));}

    @Test void negativeSchemaIsAlsoProtectedReadOnly(){CompoundTag root=new CompoundTag();root.putInt("SchemaVersion",-1);root.put("Cities",new ListTag());CitySavedData loaded=CitySavedData.load(root);assertTrue(loaded.readOnly());assertEquals(-1,loaded.saveTag().getIntOr("SchemaVersion",0));}

    @Test void heartlandSetOverridesContradictoryPerChunkFlag(){CompoundTag root=new CompoundTag();root.putInt("SchemaVersion",4);CompoundTag city=validCity("Authority");long expansion=ChunkCoordinate.pack(1,0);city.putLongArray("Territory",new long[]{ChunkCoordinate.pack(0,0),expansion});city.putLongArray("Heartland",new long[]{ChunkCoordinate.pack(0,0)});CompoundTag state=new CompoundTag();state.putLong("Chunk",expansion);state.putBoolean("PermanentHeartland",true);ListTag states=new ListTag();states.add(state);city.put("TerritoryStates",states);ListTag cities=new ListTag();cities.add(city);root.put("Cities",cities);City loaded=CitySavedData.load(root).byName("Authority").orElseThrow();assertTrue(!loaded.territoryStates().get(expansion).permanentHeartland());}

    @Test void cityBatchIsStableExactlyBoundedAndWraps(){CitySavedData data=new CitySavedData();UUID leader=UUID.randomUUID();City one=new City(new UUID(0,1),"One",0,"minecraft:overworld",0,0,0,leader,leader,Set.of(),Set.of(0L));City two=new City(new UUID(0,2),"Two",0,"minecraft:overworld",1,0,0,leader,leader,Set.of(),Set.of(1L));data.add(two);data.add(one);var first=data.cityBatch(null,1);assertEquals(java.util.List.of(one),first.cities());assertTrue(!first.wrapped());var second=data.cityBatch(first.nextCursor(),1);assertEquals(java.util.List.of(two),second.cities());assertTrue(second.wrapped());var wrapped=data.cityBatch(second.nextCursor(),1);assertEquals(java.util.List.of(one),wrapped.cities());assertTrue(wrapped.wrapped());}
    @Test void nonWrappingCityBatchSupportsFairBoundedSchedulers(){CitySavedData data=new CitySavedData();UUID leader=UUID.randomUUID();City one=new City(new UUID(0,1),"One",0,"minecraft:overworld",0,0,0,leader,leader,Set.of(),Set.of(0L));City two=new City(new UUID(0,2),"Two",0,"minecraft:overworld",1,0,0,leader,leader,Set.of(),Set.of(1L));data.add(two);data.add(one);var first=data.cityBatchAfter(null,1);assertEquals(java.util.List.of(one),first.cities());assertTrue(!first.exhausted());var second=data.cityBatchAfter(first.nextCursor(),1);assertEquals(java.util.List.of(two),second.cities());assertTrue(second.exhausted());var end=data.cityBatchAfter(second.nextCursor(),1);assertTrue(end.cities().isEmpty());assertTrue(end.exhausted());}

    @Test void migratesRevisionAndRequiresMonotonicReplacement(){CompoundTag root=new CompoundTag();root.putInt("SchemaVersion",4);ListTag cities=new ListTag();cities.add(validCity("Revision"));root.put("Cities",cities);CitySavedData loaded=CitySavedData.load(root);City city=loaded.byName("Revision").orElseThrow();assertEquals(0,city.revision());City changed=city.updateIdentity("Revision Two",city.color());assertEquals(1,changed.revision());loaded.add(changed);org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,()->loaded.add(city));City restored=CitySavedData.load(loaded.saveTag()).byId(city.id()).orElseThrow();assertEquals(1,restored.revision());}

    @Test void allCityMutationsAdvanceRevisionAndCollectionCannotBypassRepository(){UUID leader=UUID.randomUUID();long core=ChunkCoordinate.pack(0,0),next=ChunkCoordinate.pack(1,0);City city=new City(UUID.randomUUID(),"A",0,"minecraft:overworld",0,core,0,leader,leader,Set.of(leader),Set.of(core));assertEquals(1,city.relocateCore(1,core).revision());assertEquals(1,city.updateIdentity("B",1).revision());assertEquals(1,city.withMembership(Set.of(leader),Map.of()).revision());assertEquals(1,city.claim(next,core,10).revision());City claimed=city.claim(next,core,10);assertEquals(2,claimed.retract(next).revision());assertEquals(1,city.withTerritoryState(core,city.territoryStates().get(core)).revision());assertEquals(1,city.withLastMemberOnline(10).revision());CitySavedData data=new CitySavedData();data.add(city);org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,()->data.cities().clear());assertTrue(data.byId(city.id()).isPresent());}

    @Test void schema5NegativeRevisionAndDuplicateIdAreQuarantined(){CompoundTag root=new CompoundTag();root.putInt("SchemaVersion",5);CompoundTag invalid=validCity("Invalid Revision");invalid.putLong("Revision",-1);CompoundTag valid=validCity("Valid Revision");ListTag cities=new ListTag();cities.add(invalid);cities.add(valid);cities.add(valid.copy());root.put("Cities",cities);CitySavedData loaded=CitySavedData.load(root);assertEquals(1,loaded.cities().size());assertEquals(2,loaded.quarantinedCities());assertEquals(2,CitySavedData.load(loaded.saveTag()).quarantinedCities());}

    private static CompoundTag validCity(String name) {
        UUID leader = UUID.fromString("11111111-1111-1111-1111-111111111111");
        CompoundTag city = new CompoundTag();
        city.putString("Id", UUID.randomUUID().toString());
        city.putString("Name", name);
        city.putInt("Color", 0x123456);
        city.putString("Dimension", "minecraft:overworld");
        city.putLong("CorePosition", 0L);
        city.putLong("CoreChunk", ChunkCoordinate.pack(0, 0));
        city.putLong("ActivatedAt", 10L);
        city.putString("FounderId", leader.toString());
        city.putString("LordId", leader.toString());
        city.put("Residents", new ListTag());
        city.putLongArray("Territory", new long[]{ChunkCoordinate.pack(0, 0)});
        return city;
    }
}
