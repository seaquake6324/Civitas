package com.seaquake6324.civitas.domain.territory;

public enum NeglectStage {
    HEALTHY, WARNING, ABANDONED, RETRACTABLE;

    public NeglectStage worsen() { return values()[Math.min(ordinal() + 1, values().length - 1)]; }
    public NeglectStage recover() { return values()[Math.max(ordinal() - 1, 0)]; }
}
