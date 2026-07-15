package com.seaquake6324.civitas.infrastructure.population;

import com.seaquake6324.civitas.domain.population.ReproductionRules;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;

/** Maps server configuration into the pure domain rule input. */
public final class ReproductionSettings {
    public static ReproductionRules.Weights weights(){
        return new ReproductionRules.Weights(CivitasConfig.REPRODUCTION_HOUSING_WEIGHT.get(),CivitasConfig.REPRODUCTION_FOOD_WEIGHT.get(),
                CivitasConfig.REPRODUCTION_SAFETY_WEIGHT.get(),CivitasConfig.REPRODUCTION_CIVILITY_WEIGHT.get(),
                CivitasConfig.REPRODUCTION_ACTIVITY_WEIGHT.get(),CivitasConfig.REPRODUCTION_FAMILY_WEIGHT.get(),
                CivitasConfig.REPRODUCTION_CASUALTY_WEIGHT.get(),CivitasConfig.REPRODUCTION_UNCROWDED_WEIGHT.get());
    }
    private ReproductionSettings(){}
}
