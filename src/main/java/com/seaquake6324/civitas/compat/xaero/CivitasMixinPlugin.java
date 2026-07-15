package com.seaquake6324.civitas.compat.xaero;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.slf4j.Logger;

public final class CivitasMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern VERSION = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:[-+].*)?$");
    private static final VersionRange MINIMAP_RANGE = new VersionRange(26, 2);
    private static final VersionRange WORLD_MAP_RANGE = new VersionRange(1, 42);

    @Override public void onLoad(String mixinPackage) {
        warnIfUnsupported("xaerominimap", "Xaero's Minimap", MINIMAP_RANGE);
        warnIfUnsupported("xaeroworldmap", "Xaero's World Map", WORLD_MAP_RANGE);
    }
    @Override public String getRefMapperConfig() { return null; }

    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("minimap")) return hasCompatibleVersion("xaerominimap", MINIMAP_RANGE);
        if (mixinClassName.contains("worldmap")) return hasCompatibleVersion("xaeroworldmap", WORLD_MAP_RANGE);
        return false;
    }

    private static boolean hasCompatibleVersion(String modId, VersionRange range) {
        return FMLLoader.getCurrent().getLoadingModList().getMods().stream()
                .anyMatch(mod -> mod.getModId().equals(modId) && isCompatibleVersion(mod.getVersion().toString(), range));
    }

    static boolean isCompatibleVersion(String installed, VersionRange range) {
        Matcher matcher = VERSION.matcher(installed);
        if (!matcher.matches()) return false;
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        return major == range.major() && minor >= range.minimumMinor();
    }

    private static void warnIfUnsupported(String modId, String displayName, VersionRange supportedRange) {
        Optional<String> installed = FMLLoader.getCurrent().getLoadingModList().getMods().stream()
                .filter(mod -> mod.getModId().equals(modId))
                .map(mod -> mod.getVersion().toString())
                .findFirst();
        installed.filter(version -> !isCompatibleVersion(version, supportedRange)).ifPresent(version -> LOGGER.warn(
                "Civitas disabled {} integration: installed version {} is unsupported (expected {}).",
                displayName, version, supportedRange));
    }

    record VersionRange(int major, int minimumMinor) {
        @Override public String toString() {
            return ">=" + major + "." + minimumMinor + ".0 and <" + (major + 1) + ".0.0";
        }
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
