package com.seaquake6324.civitas.domain.civilization;

public enum FacilityCategory {
    RESIDENTIAL("residential"), PRODUCTION("production"), STORAGE("storage"),
    FOOD("food"), KNOWLEDGE("knowledge"), PUBLIC("public");

    private final String tagPath;
    FacilityCategory(String tagPath) { this.tagPath = tagPath; }
    public String tagPath() { return tagPath; }
}
