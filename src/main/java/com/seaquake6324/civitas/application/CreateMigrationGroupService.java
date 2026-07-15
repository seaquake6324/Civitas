package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.MigrationRepository;
import com.seaquake6324.civitas.domain.population.*;
import java.util.*;

/** Deterministically creates a source-backed household; persistence remains atomic. */
public final class CreateMigrationGroupService {
    public Result create(MigrationRepository repository,Request request){
        SplittableRandom random=new SplittableRandom(request.groupId().getMostSignificantBits()^request.groupId().getLeastSignificantBits());
        int maximum=Math.max(1,Math.min(MigrationGroupRecord.HARD_MEMBER_CAP,request.maximumMembers()));
        boolean couple=maximum>=2&&random.nextDouble()<request.coupleChance();
        int childCount=couple&&maximum>2&&random.nextDouble()<request.childChance()?1+(maximum>3&&random.nextDouble()<request.secondChildChance()?1:0):0;
        int count=(couple?2:1)+childCount;List<CitizenRecord>members=new ArrayList<>(count);Set<UUID>partners=new LinkedHashSet<>(),children=new LinkedHashSet<>();Map<UUID,Long>positions=new LinkedHashMap<>();
        String family=null;CitizenRace firstRace=null,secondRace=null;Gender firstGender=random.nextBoolean()?Gender.MALE:Gender.FEMALE,secondGender=childCount>0?(firstGender==Gender.MALE?Gender.FEMALE:Gender.MALE):(random.nextBoolean()?Gender.MALE:Gender.FEMALE);
        for(int index=0;index<count;index++){
            UUID id=UUID.nameUUIDFromBytes((request.groupId()+":member:"+index).getBytes(java.nio.charset.StandardCharsets.UTF_8));boolean child=index>=(couple?2:1);Gender gender=index==0?firstGender:index==1?secondGender:(random.nextBoolean()?Gender.MALE:Gender.FEMALE);
            CitizenRace race;if(child)race=random.nextBoolean()?firstRace:secondRace;else race=CitizenRace.values()[random.nextInt(CitizenRace.values().length)];if(index==0)firstRace=race;if(index==1)secondRace=race;
            CitizenNameGenerator.Name generated=CitizenNameGenerator.generate(id.getMostSignificantBits()^id.getLeastSignificantBits(),gender);if(family==null)family=generated.family();
            int years=child?2+random.nextInt(Math.max(1,request.adolescentAt()-2)):request.youngAdultAt()+random.nextInt(Math.max(1,request.elderAt()-request.youngAdultAt()));long ageTicks=Math.multiplyExact((long)years,request.ticksPerYear());
            String appearance=race.name().toLowerCase(Locale.ROOT)+"_"+(race==CitizenRace.HUMAN?random.nextInt(4):0);
            CitizenRecord citizen=new CitizenRecord(id,generated.given(),family,race,appearance,gender,ageTicks,request.now(),null,null,null,null,"",100,CitizenNeeds.neutral(),Map.of(),Set.of(),request.attractionScore(),Math.max(0,100-request.attractionScore()),CitizenRuntimeState.VIRTUAL,0,request.lifespans().years(id),0,1);
            members.add(citizen);positions.put(id,request.originPos());if(child)children.add(id);else if(couple)partners.add(id);
        }
        MigrationGroupRecord group=new MigrationGroupRecord(request.groupId(),request.cityId(),request.dimension(),request.originPos(),request.targetPos(),members.stream().map(CitizenRecord::id).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)),partners,children,positions,MigrationGroupRecord.State.APPROACHING,request.attractionScore(),request.now(),request.now(),1);
        return repository.createMigration(group,members,request.activeCap())?new Result(true,group,List.copyOf(members)):new Result(false,null,List.of());
    }
    public record Request(UUID groupId,UUID cityId,String dimension,long originPos,long targetPos,long now,double attractionScore,int activeCap,int maximumMembers,double coupleChance,double childChance,double secondChildChance,long ticksPerYear,int adolescentAt,int youngAdultAt,int elderAt,LifespanRules lifespans){public Request{if(groupId==null||cityId==null||lifespans==null||now<0||ticksPerYear<1||adolescentAt<3||youngAdultAt<=adolescentAt||elderAt<=youngAdultAt)throw new IllegalArgumentException("invalid migration request");coupleChance=chance(coupleChance);childChance=chance(childChance);secondChildChance=chance(secondChildChance);attractionScore=Math.max(0,Math.min(100,attractionScore));}private static double chance(double v){if(!Double.isFinite(v))throw new IllegalArgumentException("invalid chance");return Math.max(0,Math.min(1,v));}}
    public record Result(boolean success,MigrationGroupRecord group,List<CitizenRecord>members){}
}
