package com.seaquake6324.civitas.infrastructure.entity;

import com.seaquake6324.civitas.domain.population.*;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.CitizenEquipmentSavedData;
import com.seaquake6324.civitas.infrastructure.population.PopulationAgingManager;
import com.seaquake6324.civitas.infrastructure.population.CitizenMaterializationManager;
import com.seaquake6324.civitas.infrastructure.population.MigrationManager;
import com.seaquake6324.civitas.application.TransitionCitizenRuntimeService;
import com.seaquake6324.civitas.application.UpdateCitizenHealthService;
import com.seaquake6324.civitas.application.UpdateCitizenLocationService;
import com.seaquake6324.civitas.application.CreateCitizenService;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.security.PatrolExecutionManager;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Visible representation only; PopulationSavedData remains authoritative. */
public final class CitizenEntity extends PathfinderMob{
    private static final TransitionCitizenRuntimeService TRANSITIONS=new TransitionCitizenRuntimeService();
    private static final UpdateCitizenHealthService HEALTH=new UpdateCitizenHealthService();
    private static final UpdateCitizenLocationService LOCATIONS=new UpdateCitizenLocationService();
    private static final AgePhysicalRules PHYSICAL_RULES=new AgePhysicalRules();
    private static final CreateCitizenService CREATION=new CreateCitizenService();
    private static final EntityDataAccessor<String>CITIZEN=SynchedEntityData.defineId(CitizenEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String>LEASE=SynchedEntityData.defineId(CitizenEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer>RACE=SynchedEntityData.defineId(CitizenEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer>APPEARANCE=SynchedEntityData.defineId(CitizenEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer>AGE_STAGE=SynchedEntityData.defineId(CitizenEntity.class,EntityDataSerializers.INT);
    private long interactionLockUntil;
    private CitizenRace pendingSpawnRace;
    private AgeStage pendingSpawnAge;
    public CitizenEntity(EntityType<? extends CitizenEntity>type,Level level){super(type,level);setPersistenceRequired();}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes(){return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH,AgePhysicalRules.PLAYER_MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED,AgePhysicalRules.PLAYER_MOVEMENT_SPEED)
            .add(Attributes.ATTACK_DAMAGE,AgePhysicalRules.PLAYER_ATTACK_DAMAGE)
            .add(Attributes.STEP_HEIGHT,AgePhysicalRules.PLAYER_STEP_HEIGHT)
            .add(Attributes.SCALE,1.0).add(Attributes.FOLLOW_RANGE,24);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new MeleeAttackGoal(this,1.0,true));goalSelector.addGoal(2,new MigrationTravelGoal());goalSelector.addGoal(3,new PatrolTravelGoal());goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,1.05,0.0025F));goalSelector.addGoal(6,new LookAtPlayerGoal(this,CitizenEntity.class,8,0.08F));goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,8,0.06F));goalSelector.addGoal(8,new RandomLookAroundGoal(this));}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(CITIZEN,"");builder.define(LEASE,"");builder.define(RACE,0);builder.define(APPEARANCE,0);builder.define(AGE_STAGE,AgeStage.YOUNG_ADULT.ordinal());}
    public void bind(CitizenRecord citizen,UUID leaseId,long now){entityData.set(CITIZEN,citizen.id().toString());entityData.set(LEASE,leaseId.toString());sync(citizen);if(level() instanceof ServerLevel serverLevel)CitizenEquipmentSavedData.get(serverLevel.getServer()).restore(this);MaterializationLease lease=new MaterializationLease(citizen.id(),leaseId,getUUID(),citizen.revision(),now);if(!MaterializationLeaseManager.acquire(lease,CivitasConfig.NPC_ENTITY_CAP.get()))discard();}
    private void sync(CitizenRecord citizen){entityData.set(RACE,citizen.race().ordinal());entityData.set(APPEARANCE,appearance(citizen.appearanceKey()));AgeStage stage=PopulationAgingManager.rules().evaluate(citizen.ageTicks()).stage();entityData.set(AGE_STAGE,stage.ordinal());applyPhysicalProfile(stage,citizen.health());setCustomName(net.minecraft.network.chat.Component.literal(citizen.displayName()));setCustomNameVisible(false);}
    private void applyPhysicalProfile(AgeStage stage,int healthPercent){
        setBaby(stage==AgeStage.CHILD);
        AgePhysicalRules.Profile profile=PHYSICAL_RULES.forStage(stage);
        setBase(Attributes.MAX_HEALTH,profile.maxHealth());
        setBase(Attributes.MOVEMENT_SPEED,profile.movementSpeed());
        setBase(Attributes.ATTACK_DAMAGE,profile.attackDamage());
        setBase(Attributes.STEP_HEIGHT,profile.stepHeight());
        // Baby humanoids already apply a 0.5 age scale; compensate so the final
        // collision and render scale remains the domain profile's exact value.
        setBase(Attributes.SCALE,profile.bodyScale()/getAgeScale());
        setHealth(getMaxHealth()*Math.max(0,Math.min(100,healthPercent))/100F);
    }
    private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>attribute,double value){var instance=getAttribute(attribute);if(instance!=null&&Double.compare(instance.getBaseValue(),value)!=0)instance.setBaseValue(value);}
    @Override protected void customServerAiStep(ServerLevel level){super.customServerAiStep(level);if(tickCount%20!=0)return;if(citizenId().isEmpty()&&pendingSpawnRace!=null&&!createSpawnEggCitizen(level)){discard();return;}Optional<UUID>citizenId=citizenId(),leaseId=leaseId();if(citizenId.isEmpty()||leaseId.isEmpty()){discard();return;}PopulationSavedData data=PopulationSavedData.get(level.getServer());CitizenRecord citizen=data.citizen(citizenId.get()).orElse(null);if(citizen==null||!citizen.alive()||!MaterializationLeaseManager.acquire(new MaterializationLease(citizen.id(),leaseId.get(),getUUID(),citizen.revision(),level.getGameTime()),CivitasConfig.NPC_ENTITY_CAP.get())){discard();return;}CitizenEquipmentSavedData.get(level.getServer()).capture(this,citizen.revision());interactionLockUntil=Math.max(interactionLockUntil,citizen.runtimeLockUntilTick());if(citizen.runtimeState()==CitizenRuntimeState.VIRTUAL||citizen.runtimeState()==CitizenRuntimeState.PREWARMING){persistLocation(data,citizen,level);MigrationManager.capturePosition(this,level);discard();return;}int liveHealth=Math.max(0,Math.min(100,Math.round(getHealth()*100F/getMaxHealth())));if(liveHealth!=citizen.health()){var health=HEALTH.update(data,citizen.id(),citizen.revision(),liveHealth);if(!health.success()){discard();return;}citizen=health.citizen();}if(CitizenMaterializationManager.shouldVirtualize(this,level)){persistLocation(data,citizen,level);MigrationManager.capturePosition(this,level);TRANSITIONS.transition(data,citizen.id(),citizen.revision(),CitizenRuntimeState.VIRTUAL);discard();return;}CitizenRuntimeState desired=locked(level.getGameTime())?CitizenRuntimeState.LOCKED:CitizenRuntimeState.MATERIALIZED;if(citizen.runtimeState()!=desired){var result=desired==CitizenRuntimeState.LOCKED?TRANSITIONS.lock(data,citizen.id(),citizen.revision(),interactionLockUntil):TRANSITIONS.transition(data,citizen.id(),citizen.revision(),desired);if(!result.success()){discard();return;}citizen=result.citizen();}sync(citizen);}
    private boolean createSpawnEggCitizen(ServerLevel level){PopulationSavedData data=PopulationSavedData.get(level.getServer());if(data.readOnly())return false;UUID id=UUID.randomUUID();Gender gender=(id.getLeastSignificantBits()&1)==0?Gender.MALE:Gender.FEMALE;CitizenNameGenerator.Name name=CitizenNameGenerator.generate(id.getMostSignificantBits()^id.getLeastSignificantBits(),gender);AgeRules rules=PopulationAgingManager.rules();AgeStage stage=pendingSpawnAge==null?AgeStage.YOUNG_ADULT:pendingSpawnAge;int years=switch(stage){case CHILD->0;case ADOLESCENT->rules.adolescentAt();case YOUNG_ADULT->rules.youngAdultAt();case MATURE_ADULT->rules.matureAdultAt();case ELDER->rules.elderAt();};long ageTicks=Math.multiplyExact((long)years,rules.ticksPerYear());UUID cityId=CitySavedData.get(level.getServer()).cityAt(level.dimension().identifier().toString(),net.minecraft.world.level.ChunkPos.pack(blockPosition())).map(com.seaquake6324.civitas.domain.City::id).orElse(null);CitizenRace race=pendingSpawnRace;String key=race.name().toLowerCase(java.util.Locale.ROOT)+"_"+(race==CitizenRace.HUMAN?Math.floorMod(id.hashCode(),4):0);CitizenRecord created=CREATION.create(data,new CreateCitizenService.Request(id,name.given(),name.family(),race,key,gender,ageTicks,level.getGameTime(),cityId,PopulationAgingManager.lifespanRules().years(id)));var prepared=TRANSITIONS.transition(data,created.id(),created.revision(),CitizenRuntimeState.PREWARMING);if(!prepared.success())return false;var materialized=TRANSITIONS.transition(data,created.id(),prepared.citizen().revision(),CitizenRuntimeState.MATERIALIZED);if(!materialized.success())return false;pendingSpawnRace=null;pendingSpawnAge=null;bind(materialized.citizen(),UUID.randomUUID(),level.getGameTime());return !isRemoved();}
    private void persistLocation(PopulationSavedData data,CitizenRecord citizen,ServerLevel level){LOCATIONS.update(data,citizen.id(),citizen.revision(),level.dimension().identifier().toString(),blockPosition().asLong(),level.getGameTime());}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public void remove(RemovalReason reason){if(level() instanceof ServerLevel serverLevel)citizenId().ifPresent(id->{PopulationSavedData data=PopulationSavedData.get(serverLevel.getServer());data.citizen(id).filter(CitizenRecord::alive).ifPresent(c->{CitizenEquipmentSavedData.get(serverLevel.getServer()).capture(this,c.revision());persistLocation(data,c,serverLevel);});});citizenId().ifPresent(c->leaseId().ifPresent(l->MaterializationLeaseManager.release(c,l,getUUID())));super.remove(reason);}
    @Override protected void addAdditionalSaveData(ValueOutput output){super.addAdditionalSaveData(output);if(!entityData.get(CITIZEN).isBlank())output.putString("CitizenId",entityData.get(CITIZEN));if(!entityData.get(LEASE).isBlank())output.putString("LeaseId",entityData.get(LEASE));if(pendingSpawnRace!=null){output.putBoolean("CivitasSpawnEgg",true);output.putString("CivitasRace",pendingSpawnRace.name());output.putString("CivitasAgeStage",(pendingSpawnAge==null?AgeStage.YOUNG_ADULT:pendingSpawnAge).name());}}
    @Override protected void readAdditionalSaveData(ValueInput input){super.readAdditionalSaveData(input);entityData.set(CITIZEN,input.getStringOr("CitizenId",""));entityData.set(LEASE,input.getStringOr("LeaseId",""));if(input.getBooleanOr("CivitasSpawnEgg",false)){pendingSpawnRace=parse(CitizenRace.class,input.getStringOr("CivitasRace","HUMAN"),CitizenRace.HUMAN);pendingSpawnAge=parse(AgeStage.class,input.getStringOr("CivitasAgeStage","YOUNG_ADULT"),AgeStage.YOUNG_ADULT);entityData.set(RACE,pendingSpawnRace.ordinal());entityData.set(AGE_STAGE,pendingSpawnAge.ordinal());applyPhysicalProfile(pendingSpawnAge,100);}}
    public Optional<UUID>citizenId(){return uuid(entityData.get(CITIZEN));}public Optional<UUID>leaseId(){return uuid(entityData.get(LEASE));}
    public void lockFor(long now,long ticks){interactionLockUntil=Math.max(interactionLockUntil,now+Math.max(1,ticks));if(level() instanceof ServerLevel serverLevel)citizenId().ifPresent(id->{PopulationSavedData data=PopulationSavedData.get(serverLevel.getServer());data.citizen(id).ifPresent(c->TRANSITIONS.lock(data,id,c.revision(),interactionLockUntil));});}
    public boolean locked(long now){return now<interactionLockUntil||isPassenger()||isVehicle()||isLeashed();}
    public void persistHealth(int percent){if(level() instanceof ServerLevel serverLevel)citizenId().ifPresent(id->{PopulationSavedData data=PopulationSavedData.get(serverLevel.getServer());data.citizen(id).ifPresent(c->HEALTH.update(data,id,c.revision(),percent));});}
    public CitizenRace citizenRace(){return enumAt(CitizenRace.values(),entityData.get(RACE),CitizenRace.HUMAN);}public AgeStage ageStage(){return enumAt(AgeStage.values(),entityData.get(AGE_STAGE),AgeStage.YOUNG_ADULT);}public int appearanceIndex(){return Math.max(0,entityData.get(APPEARANCE));}
    private static int appearance(String key){int underscore=key.lastIndexOf('_');if(underscore<0)return 0;try{return Math.max(0,Integer.parseInt(key.substring(underscore+1)));}catch(NumberFormatException ignored){return 0;}}
    private static Optional<UUID>uuid(String value){try{return value.isBlank()?Optional.empty():Optional.of(UUID.fromString(value));}catch(IllegalArgumentException exception){return Optional.empty();}}
    private static<T>T enumAt(T[]values,int index,T fallback){return index>=0&&index<values.length?values[index]:fallback;}
    private static<E extends Enum<E>>E parse(Class<E>type,String value,E fallback){try{return Enum.valueOf(type,value);}catch(IllegalArgumentException ignored){return fallback;}}
    private final class MigrationTravelGoal extends Goal{
        MigrationTravelGoal(){setFlags(java.util.EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){return level() instanceof ServerLevel serverLevel&&MigrationManager.drive(CitizenEntity.this,serverLevel);}
        @Override public boolean canContinueToUse(){return canUse();}
        @Override public void tick(){if(level() instanceof ServerLevel serverLevel)MigrationManager.drive(CitizenEntity.this,serverLevel);}
    }
    private final class PatrolTravelGoal extends Goal{PatrolTravelGoal(){setFlags(java.util.EnumSet.of(Flag.MOVE));}@Override public boolean canUse(){return level() instanceof ServerLevel serverLevel&&PatrolExecutionManager.drive(CitizenEntity.this,serverLevel);}@Override public boolean canContinueToUse(){return canUse();}@Override public void tick(){if(level() instanceof ServerLevel serverLevel)PatrolExecutionManager.drive(CitizenEntity.this,serverLevel);}}
}
