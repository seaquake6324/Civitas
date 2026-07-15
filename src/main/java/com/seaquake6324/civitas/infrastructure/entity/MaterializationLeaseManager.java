package com.seaquake6324.civitas.infrastructure.entity;
import com.seaquake6324.civitas.domain.population.MaterializationLease;import java.util.*;
/** Main-thread lease table; one persistent citizen can have at most one entity representation. */
public final class MaterializationLeaseManager{
 private static final Map<UUID,MaterializationLease>LEASES=new HashMap<>();private static long acquired,reused,rejected,released;
 public static boolean acquire(MaterializationLease lease){return acquire(lease,Integer.MAX_VALUE);}
 public static boolean acquire(MaterializationLease lease,int maxActive){MaterializationLease current=LEASES.get(lease.citizenId());if(current==null){if(LEASES.size()>=Math.max(1,maxActive)){rejected++;return false;}LEASES.put(lease.citizenId(),lease);acquired++;return true;}if(current.leaseId().equals(lease.leaseId())&&current.entityId().equals(lease.entityId())){LEASES.put(lease.citizenId(),lease);reused++;return true;}rejected++;return false;}
 public static void release(UUID citizenId,UUID leaseId,UUID entityId){MaterializationLease current=LEASES.get(citizenId);if(current!=null&&current.leaseId().equals(leaseId)&&current.entityId().equals(entityId)){LEASES.remove(citizenId);released++;}}
 public static boolean active(UUID citizenId){return LEASES.containsKey(citizenId);}
 public static Optional<MaterializationLease> lease(UUID citizenId){return Optional.ofNullable(LEASES.get(citizenId));}
 public static void revoke(UUID citizenId){if(LEASES.remove(citizenId)!=null)released++;}
 public static void clear(){LEASES.clear();}
 public static Metrics metrics(){return new Metrics(LEASES.size(),acquired,reused,rejected,released);}public record Metrics(int active,long acquired,long reused,long rejected,long released){}
 private MaterializationLeaseManager(){}
}
