package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.membership.MembershipApplication;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public final class MembershipService {
    public enum Failure { NOT_AT_CORE, ALREADY_MEMBER, ALREADY_APPLIED, NOT_MANAGER, APPLICATION_MISSING,
        CANNOT_REMOVE_FOUNDER, CANNOT_REMOVE_LORD }
    public record Result(City city, Failure failure) { public boolean success(){return failure==null;} }
    public Result apply(City city, UUID player, long now, boolean atCore) {
        if(!atCore)return new Result(city,Failure.NOT_AT_CORE); if(city.isMember(player))return new Result(city,Failure.ALREADY_MEMBER);
        if(city.applications().containsKey(player))return new Result(city,Failure.ALREADY_APPLIED);
        var apps=new HashMap<>(city.applications()); apps.put(player,new MembershipApplication(player,now,MembershipApplication.Status.PENDING));
        return new Result(city.withMembership(city.residents(),apps),null);
    }
    public Result decide(City city, UUID manager, UUID player, boolean approve) {
        if(!city.mayManage(manager))return new Result(city,Failure.NOT_MANAGER);
        if(!city.applications().containsKey(player))return new Result(city,Failure.APPLICATION_MISSING);
        var apps=new HashMap<>(city.applications()); apps.remove(player); var members=new HashSet<>(city.residents()); if(approve)members.add(player);
        return new Result(city.withMembership(members,apps),null);
    }
    public Result leaveOrRemove(City city, UUID actor, UUID target) {
        if(!actor.equals(target)&&!city.mayManage(actor))return new Result(city,Failure.NOT_MANAGER);
        if(target.equals(city.founderId()))return new Result(city,Failure.CANNOT_REMOVE_FOUNDER);
        if(target.equals(city.lordId()))return new Result(city,Failure.CANNOT_REMOVE_LORD);
        var members=new HashSet<>(city.residents()); members.remove(target);
        return new Result(city.withMembership(members,city.applications()),null);
    }
}
