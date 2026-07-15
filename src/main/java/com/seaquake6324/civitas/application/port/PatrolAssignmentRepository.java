package com.seaquake6324.civitas.application.port;

import com.seaquake6324.civitas.domain.security.PatrolAssignment;
import java.util.Optional;
import java.util.UUID;

public interface PatrolAssignmentRepository {
    Optional<PatrolAssignment> patrolAssignment(UUID id);
    Optional<PatrolAssignment> patrolAssignmentForCitizen(UUID citizenId);
    boolean createPatrolAssignment(PatrolAssignment assignment, int cityCap);
    boolean replacePatrolAssignment(PatrolAssignment assignment, long expectedRevision);
}
