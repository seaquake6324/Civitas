package com.seaquake6324.civitas.domain.population;
import java.util.UUID;
public record MaterializationLease(UUID citizenId,UUID leaseId,UUID entityId,long citizenRevision,long acquiredAt){}
