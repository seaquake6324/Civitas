package com.seaquake6324.civitas.application;
import com.seaquake6324.civitas.domain.building.BuildingRecord;import com.seaquake6324.civitas.domain.population.*;import java.util.*;
public final class BuildPopulationDashboardService{public PopulationDashboard build(UUID cityId,Collection<CitizenRecord>citizens,Collection<Household>households,Collection<BuildingRecord>buildings,AgeRules rules,boolean truncated){return PopulationDashboard.from(cityId,citizens,households,buildings,rules,truncated);}}
