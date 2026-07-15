package com.seaquake6324.civitas.application.port;
import com.seaquake6324.civitas.domain.security.PatrolRoute;import java.util.*;
public interface PatrolRouteRepository{Optional<PatrolRoute> patrolRoute(UUID id);boolean createPatrolRoute(PatrolRoute route,int cityCap);}
