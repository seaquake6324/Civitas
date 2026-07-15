package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenCityManagementPayload(BlockPos corePos,String cityName,int cityColor,
        boolean member,boolean manager,boolean founder,boolean lord,
        List<PlayerEntry> residents,List<PlayerEntry> applications,
        int territorySize,int heartlandSize,long lastExpansionAt,long expansionCooldownRemaining,
        float maxPressure,String threatDirection,String threatPhase,long threatPhaseRemaining,
        int warningChunks,int abandonedChunks,int retractableChunks,long recoveryRemaining,
        int validBuildings,int staleBuildings,int invalidBuildings,int housingCapacity,int guardCapacity,
        int securityCells,float securityAverageRisk,float securityMaxRisk,long securityWeakestChunk,
        String securityPrimaryFactor,int securityMissingCells,boolean securityTruncated,int securityExamined,long securityUpdatedAt,int patrolRoutes,int guardAssignments,int activeGuards,int recentlyPatrolledCells,int visibleGuardCells,
        List<BuildingView> buildings,boolean buildingsTruncated,List<MigrationView>migrationApplications,PopulationView population,
        List<OrphanView>orphans,List<AdoptionView>adoptions)
        implements CustomPacketPayload {
    public record PlayerEntry(UUID id,String name) {
        public PlayerEntry { name=name==null||name.isBlank()?"?":name; }
    }
    public record PopulationView(int total,int male,int female,int child,int adolescent,int youngAdult,int matureAdult,int elder,
            int human,int pigfolk,int cowfolk,int sheepfolk,int virtualCount,int prewarming,int materialized,int locked,
            int housingCapacity,int housed,int unassigned,int invalidResidence,int employed,int households,int partneredHouseholds,
            int householdsWithChildren,int permanentDeaths,long latestDeathAtTick,float averageFoodCoverage,float averageHousingCoverage,float averageSettlementWillingness,
            float averageMigrationWillingness,List<String>limitations,boolean truncated){public PopulationView{limitations=List.copyOf(limitations);}}
    public record BuildingView(UUID id,String purpose,String status,int capacity,int cells,
            String requiredFacility,int requiredFacilityCount,int boundaryPorts,int workstations,
            int storageEndpoints,int authorizedStorageEndpoints,boolean entranceConnected,long revision,String invalidReason) {
        public BuildingView { invalidReason=invalidReason==null?"":invalidReason; }
    }
    public record MigrationView(UUID id,String names,int members,int children,float attraction,long decisionRemaining,long revision){public MigrationView{names=names==null?"":names;}}
    public record OrphanView(UUID childId,String name,long admittedAt,long revision){public OrphanView{name=name==null?"?":name;}}
    public record AdoptionView(UUID id,UUID childId,String childName,int requiredPlayers,int confirmedPlayers,boolean selfConfirmed,long remaining,long revision){public AdoptionView{childName=childName==null?"?":childName;}}
    public static final Type<OpenCityManagementPayload> TYPE=new Type<>(
            Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID,"open_city_management"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OpenCityManagementPayload> STREAM_CODEC=
            CustomPacketPayload.codec(OpenCityManagementPayload::write,OpenCityManagementPayload::read);
    public OpenCityManagementPayload { residents=List.copyOf(residents);applications=List.copyOf(applications);buildings=List.copyOf(buildings);migrationApplications=List.copyOf(migrationApplications);orphans=List.copyOf(orphans);adoptions=List.copyOf(adoptions);java.util.Objects.requireNonNull(population); }
    private void write(RegistryFriendlyByteBuf b){
        b.writeBlockPos(corePos);b.writeUtf(cityName,80);b.writeInt(cityColor);b.writeBoolean(member);b.writeBoolean(manager);
        b.writeBoolean(founder);b.writeBoolean(lord);writePlayers(b,residents);writePlayers(b,applications);
        b.writeVarInt(territorySize);b.writeVarInt(heartlandSize);b.writeVarLong(lastExpansionAt);
        b.writeVarLong(expansionCooldownRemaining);b.writeFloat(maxPressure);b.writeUtf(threatDirection);
        b.writeUtf(threatPhase);b.writeVarLong(threatPhaseRemaining);b.writeVarInt(warningChunks);
        b.writeVarInt(abandonedChunks);b.writeVarInt(retractableChunks);b.writeVarLong(recoveryRemaining);
        b.writeVarInt(validBuildings);b.writeVarInt(staleBuildings);b.writeVarInt(invalidBuildings);b.writeVarInt(housingCapacity);b.writeVarInt(guardCapacity);
        b.writeVarInt(securityCells);b.writeFloat(securityAverageRisk);b.writeFloat(securityMaxRisk);b.writeLong(securityWeakestChunk);b.writeUtf(securityPrimaryFactor);b.writeVarInt(securityMissingCells);b.writeBoolean(securityTruncated);b.writeVarInt(securityExamined);b.writeVarLong(securityUpdatedAt);b.writeVarInt(patrolRoutes);b.writeVarInt(guardAssignments);b.writeVarInt(activeGuards);b.writeVarInt(recentlyPatrolledCells);b.writeVarInt(visibleGuardCells);
        b.writeVarInt(buildings.size());for(BuildingView value:buildings)writeBuilding(b,value);b.writeBoolean(buildingsTruncated);b.writeVarInt(migrationApplications.size());for(MigrationView value:migrationApplications)writeMigration(b,value);
        writePopulation(b,population);b.writeVarInt(orphans.size());for(OrphanView value:orphans)writeOrphan(b,value);b.writeVarInt(adoptions.size());for(AdoptionView value:adoptions)writeAdoption(b,value);
    }
    private static OpenCityManagementPayload read(RegistryFriendlyByteBuf b){
        return new OpenCityManagementPayload(b.readBlockPos(),b.readUtf(80),b.readInt(),b.readBoolean(),b.readBoolean(),
                b.readBoolean(),b.readBoolean(),readPlayers(b),readPlayers(b),b.readVarInt(),b.readVarInt(),
                b.readVarLong(),b.readVarLong(),b.readFloat(),b.readUtf(),b.readUtf(),b.readVarLong(),
                b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarLong(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),
                b.readVarInt(),b.readFloat(),b.readFloat(),b.readLong(),b.readUtf(),b.readVarInt(),b.readBoolean(),b.readVarInt(),b.readVarLong(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),readBuildings(b),b.readBoolean(),readMigrations(b),readPopulation(b),readOrphans(b),readAdoptions(b));
    }
    private static void writePlayers(RegistryFriendlyByteBuf b,List<PlayerEntry> players){b.writeVarInt(players.size());for(PlayerEntry player:players){b.writeUUID(player.id());b.writeUtf(player.name(),64);}}
    private static List<PlayerEntry> readPlayers(RegistryFriendlyByteBuf b){int count=b.readVarInt();List<PlayerEntry> result=new ArrayList<>(count);for(int i=0;i<count;i++)result.add(new PlayerEntry(b.readUUID(),b.readUtf(64)));return List.copyOf(result);}
    private static void writeBuilding(RegistryFriendlyByteBuf b,BuildingView value){b.writeUUID(value.id);b.writeUtf(value.purpose,32);b.writeUtf(value.status,16);b.writeVarInt(value.capacity);b.writeVarInt(value.cells);b.writeUtf(value.requiredFacility,32);b.writeVarInt(value.requiredFacilityCount);b.writeVarInt(value.boundaryPorts);b.writeVarInt(value.workstations);b.writeVarInt(value.storageEndpoints);b.writeVarInt(value.authorizedStorageEndpoints);b.writeBoolean(value.entranceConnected);b.writeVarLong(value.revision);b.writeUtf(value.invalidReason,64);}
    private static List<BuildingView> readBuildings(RegistryFriendlyByteBuf b){int count=b.readVarInt();if(count<0||count>64)throw new IllegalArgumentException("invalid building view count");List<BuildingView>values=new ArrayList<>(count);for(int i=0;i<count;i++)values.add(new BuildingView(b.readUUID(),b.readUtf(32),b.readUtf(16),b.readVarInt(),b.readVarInt(),b.readUtf(32),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readVarLong(),b.readUtf(64)));return List.copyOf(values);}
    private static void writeMigration(RegistryFriendlyByteBuf b,MigrationView value){b.writeUUID(value.id);b.writeUtf(value.names,256);b.writeVarInt(value.members);b.writeVarInt(value.children);b.writeFloat(value.attraction);b.writeVarLong(value.decisionRemaining);b.writeVarLong(value.revision);}
    private static List<MigrationView>readMigrations(RegistryFriendlyByteBuf b){int count=b.readVarInt();if(count<0||count>64)throw new IllegalArgumentException("invalid migration view count");List<MigrationView>values=new ArrayList<>(count);for(int i=0;i<count;i++)values.add(new MigrationView(b.readUUID(),b.readUtf(256),b.readVarInt(),b.readVarInt(),b.readFloat(),b.readVarLong(),b.readVarLong()));return List.copyOf(values);}
    private static void writeOrphan(RegistryFriendlyByteBuf b,OrphanView value){b.writeUUID(value.childId);b.writeUtf(value.name,128);b.writeVarLong(value.admittedAt);b.writeVarLong(value.revision);}
    private static List<OrphanView>readOrphans(RegistryFriendlyByteBuf b){int count=b.readVarInt();if(count<0||count>64)throw new IllegalArgumentException("invalid orphan view count");List<OrphanView>out=new ArrayList<>(count);for(int i=0;i<count;i++)out.add(new OrphanView(b.readUUID(),b.readUtf(128),b.readVarLong(),b.readVarLong()));return List.copyOf(out);}
    private static void writeAdoption(RegistryFriendlyByteBuf b,AdoptionView value){b.writeUUID(value.id);b.writeUUID(value.childId);b.writeUtf(value.childName,128);b.writeVarInt(value.requiredPlayers);b.writeVarInt(value.confirmedPlayers);b.writeBoolean(value.selfConfirmed);b.writeVarLong(value.remaining);b.writeVarLong(value.revision);}
    private static List<AdoptionView>readAdoptions(RegistryFriendlyByteBuf b){int count=b.readVarInt();if(count<0||count>64)throw new IllegalArgumentException("invalid adoption view count");List<AdoptionView>out=new ArrayList<>(count);for(int i=0;i<count;i++)out.add(new AdoptionView(b.readUUID(),b.readUUID(),b.readUtf(128),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readVarLong(),b.readVarLong()));return List.copyOf(out);}
    private static void writePopulation(RegistryFriendlyByteBuf b,PopulationView p){b.writeVarInt(p.total);b.writeVarInt(p.male);b.writeVarInt(p.female);b.writeVarInt(p.child);b.writeVarInt(p.adolescent);b.writeVarInt(p.youngAdult);b.writeVarInt(p.matureAdult);b.writeVarInt(p.elder);b.writeVarInt(p.human);b.writeVarInt(p.pigfolk);b.writeVarInt(p.cowfolk);b.writeVarInt(p.sheepfolk);b.writeVarInt(p.virtualCount);b.writeVarInt(p.prewarming);b.writeVarInt(p.materialized);b.writeVarInt(p.locked);b.writeVarInt(p.housingCapacity);b.writeVarInt(p.housed);b.writeVarInt(p.unassigned);b.writeVarInt(p.invalidResidence);b.writeVarInt(p.employed);b.writeVarInt(p.households);b.writeVarInt(p.partneredHouseholds);b.writeVarInt(p.householdsWithChildren);b.writeVarInt(p.permanentDeaths);b.writeVarLong(p.latestDeathAtTick);b.writeFloat(p.averageFoodCoverage);b.writeFloat(p.averageHousingCoverage);b.writeFloat(p.averageSettlementWillingness);b.writeFloat(p.averageMigrationWillingness);b.writeVarInt(p.limitations.size());for(String value:p.limitations)b.writeUtf(value,64);b.writeBoolean(p.truncated);}
    private static PopulationView readPopulation(RegistryFriendlyByteBuf b){int total=b.readVarInt(),male=b.readVarInt(),female=b.readVarInt(),child=b.readVarInt(),adolescent=b.readVarInt(),young=b.readVarInt(),mature=b.readVarInt(),elder=b.readVarInt(),human=b.readVarInt(),pig=b.readVarInt(),cow=b.readVarInt(),sheep=b.readVarInt(),virtual=b.readVarInt(),prewarm=b.readVarInt(),materialized=b.readVarInt(),locked=b.readVarInt(),housing=b.readVarInt(),housed=b.readVarInt(),unassigned=b.readVarInt(),invalid=b.readVarInt(),employed=b.readVarInt(),households=b.readVarInt(),partnered=b.readVarInt(),children=b.readVarInt(),deaths=b.readVarInt();long latestDeath=b.readVarLong();float food=b.readFloat(),housingCoverage=b.readFloat(),settlement=b.readFloat(),migration=b.readFloat();int count=b.readVarInt();if(count<0||count>64)throw new IllegalArgumentException("invalid population limitation count");List<String>limits=new ArrayList<>(count);for(int i=0;i<count;i++)limits.add(b.readUtf(64));return new PopulationView(total,male,female,child,adolescent,young,mature,elder,human,pig,cow,sheep,virtual,prewarm,materialized,locked,housing,housed,unassigned,invalid,employed,households,partnered,children,deaths,latestDeath,food,housingCoverage,settlement,migration,limits,b.readBoolean());}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
