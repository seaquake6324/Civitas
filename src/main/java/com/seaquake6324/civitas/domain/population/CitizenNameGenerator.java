package com.seaquake6324.civitas.domain.population;

import java.util.SplittableRandom;

/** Deterministic culturally varied European-style names; no culture has gameplay effects. */
public final class CitizenNameGenerator {
    public enum Region { EASTERN, WESTERN, NORTHERN, SOUTHERN }
    private static final String[][] MALE={{"Aleksander","Marek","Nikolai","Tomasz"},{"Arthur","Hugo","Lucien","William"},{"Erik","Leif","Soren","Anders"},{"Luca","Mateo","Nico","Stefano"}};
    private static final String[][] FEMALE={{"Anya","Katarina","Milena","Zofia"},{"Adeline","Clara","Elise","Margot"},{"Astrid","Freja","Ingrid","Liv"},{"Alessia","Bianca","Lucia","Sofia"}};
    private static final String[][] FAMILY={{"Kowalski","Novak","Petrov","Varga"},{"Beaumont","Fischer","Moreau","Taylor"},{"Berg","Lindholm","Nielsen","Svensson"},{"Costa","Marino","Rossi","Silva"}};
    public static Name generate(long seed,Gender gender){SplittableRandom random=new SplittableRandom(seed);int region=random.nextInt(Region.values().length);String[][] given=gender==Gender.MALE?MALE:FEMALE;return new Name(given[region][random.nextInt(given[region].length)],FAMILY[region][random.nextInt(FAMILY[region].length)],Region.values()[region]);}
    public record Name(String given,String family,Region region){}
    private CitizenNameGenerator(){}
}
