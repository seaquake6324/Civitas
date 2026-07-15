package com.seaquake6324.civitas.infrastructure.civilization;

import com.mojang.logging.LogUtils;
import com.seaquake6324.civitas.domain.civilization.ChunkCivilization;
import com.seaquake6324.civitas.domain.civilization.CivilizationLayer;
import com.seaquake6324.civitas.domain.civilization.SpawnSuppressionCurve;
import com.seaquake6324.civitas.domain.region.RegionType;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.activity.ActivityManager;
import com.seaquake6324.civitas.infrastructure.persistence.CivilizationSavedData;
import com.seaquake6324.civitas.infrastructure.spawn.ServerRegionClassifier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

/** Minecraft adapter for domain civilization state and configurable suppression curves. */
public final class CivilizationAccess {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SpawnSuppressionCurve DEFAULT_CURVE = new SpawnSuppressionCurve(List.of(
            new SpawnSuppressionCurve.Point(0, 0), new SpawnSuppressionCurve.Point(20, .10),
            new SpawnSuppressionCurve.Point(40, .30), new SpawnSuppressionCurve.Point(60, .55),
            new SpawnSuppressionCurve.Point(80, .75), new SpawnSuppressionCurve.Point(100, .90)));
    private static List<? extends String> cachedConfig;
    private static SpawnSuppressionCurve cachedCurve = DEFAULT_CURVE;

    public static CivilizationLayer layer(ServerLevel level, BlockPos pos) {
        return ServerRegionClassifier.classify(level, pos).type() == RegionType.UNDERGROUND
                ? CivilizationLayer.UNDERGROUND : CivilizationLayer.SURFACE;
    }

    public static ChunkCivilization state(ServerLevel level, BlockPos pos) {
        return state(level, pos, layer(level, pos));
    }

    public static ChunkCivilization state(ServerLevel level, BlockPos pos, CivilizationLayer knownLayer) {
        return ActivityManager.applyDecay(level, ChunkPos.pack(pos), knownLayer);
    }

    public static double suppression(ServerLevel level, BlockPos pos) {
        ChunkCivilization state = state(level, pos);
        return suppressionBreakdown(state).finalSuppression();
    }

    public static SpawnSuppressionCurve.Breakdown suppressionBreakdown(ChunkCivilization state) {
        return curve().breakdown(state.civility(), state.activity());
    }

    private static SpawnSuppressionCurve curve() {
        List<? extends String> config = CivitasConfig.CIVILITY_SUPPRESSION_CURVE.get();
        if (config.equals(cachedConfig)) return cachedCurve;
        try {
            List<SpawnSuppressionCurve.Point> points = new ArrayList<>();
            for (String entry : config) {
                String[] pair = entry.split(":", 2);
                points.add(new SpawnSuppressionCurve.Point(Double.parseDouble(pair[0]), Double.parseDouble(pair[1])));
            }
            cachedCurve = new SpawnSuppressionCurve(points);
        } catch (RuntimeException exception) {
            LOGGER.warn("Invalid Civitas suppression curve; using defaults", exception);
            cachedCurve = DEFAULT_CURVE;
        }
        cachedConfig = List.copyOf(config);
        return cachedCurve;
    }

    private CivilizationAccess() {}
}
