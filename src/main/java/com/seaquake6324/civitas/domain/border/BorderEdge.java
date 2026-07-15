package com.seaquake6324.civitas.domain.border;
import com.seaquake6324.civitas.domain.territory.TerritoryTopology;
public record BorderEdge(long chunk, TerritoryTopology.Direction direction) {}
