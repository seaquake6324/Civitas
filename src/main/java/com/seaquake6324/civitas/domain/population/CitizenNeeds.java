package com.seaquake6324.civitas.domain.population;

public record CitizenNeeds(int food, int safety, int rest, int social) {
    public CitizenNeeds {
        food=clamp(food);safety=clamp(safety);rest=clamp(rest);social=clamp(social);
    }
    public static CitizenNeeds neutral() { return new CitizenNeeds(50,50,50,50); }
    private static int clamp(int value) { return Math.max(0,Math.min(100,value)); }
}
