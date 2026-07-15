package com.seaquake6324.civitas.domain.population;

/** Configured first-pass hypothesis with an explainable calculation path. */
public record AgeRules(long ticksPerYear, int adolescentAt, int youngAdultAt, int matureAdultAt, int elderAt) {
    /** Uses the documented first-pass balance of one Minecraft day per citizen year. */
    public AgeRules(int adolescentAt,int youngAdultAt,int matureAdultAt,int elderAt){this(24_000,adolescentAt,youngAdultAt,matureAdultAt,elderAt);}
    public AgeRules {
        if(ticksPerYear<1||adolescentAt<1||youngAdultAt<=adolescentAt||matureAdultAt<=youngAdultAt||elderAt<=matureAdultAt)
            throw new IllegalArgumentException("invalid age rules");
    }
    public Result evaluate(long ageTicks) {
        long safe=Math.max(0,ageTicks);long years=safe/ticksPerYear;
        AgeStage stage=years>=elderAt?AgeStage.ELDER:years>=matureAdultAt?AgeStage.MATURE_ADULT:
                years>=youngAdultAt?AgeStage.YOUNG_ADULT:years>=adolescentAt?AgeStage.ADOLESCENT:AgeStage.CHILD;
        int next=switch(stage){case CHILD->adolescentAt;case ADOLESCENT->youngAdultAt;case YOUNG_ADULT->matureAdultAt;case MATURE_ADULT->elderAt;case ELDER->-1;};
        long remaining=next<0?0:Math.max(0,next*ticksPerYear-safe);
        return new Result(safe,years,stage,next,remaining);
    }
    public record Result(long rawAgeTicks,long completedYears,AgeStage stage,int nextStageAtYears,long ticksToNextStage){}
}
