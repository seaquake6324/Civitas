package com.seaquake6324.civitas.domain.security;
public record SecurityContribution(double risk,double weight,double weightedRisk){public SecurityContribution{risk=clamp(risk);weight=Math.max(0,weight);weightedRisk=Math.max(0,weightedRisk);}private static double clamp(double value){return Math.max(0,Math.min(100,value));}}
