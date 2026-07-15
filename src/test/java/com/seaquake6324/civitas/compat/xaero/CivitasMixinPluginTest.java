package com.seaquake6324.civitas.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CivitasMixinPluginTest {
    @Test
    void acceptsMinimapMinorAndPatchUpdatesWithinMajorVersion() {
        CivitasMixinPlugin.VersionRange range = new CivitasMixinPlugin.VersionRange(26, 2);

        assertTrue(CivitasMixinPlugin.isCompatibleVersion("26.2.0", range));
        assertTrue(CivitasMixinPlugin.isCompatibleVersion("26.4.0", range));
        assertTrue(CivitasMixinPlugin.isCompatibleVersion("26.4.1-beta", range));
    }

    @Test
    void rejectsOlderAndNextMajorMinimapVersions() {
        CivitasMixinPlugin.VersionRange range = new CivitasMixinPlugin.VersionRange(26, 2);

        assertFalse(CivitasMixinPlugin.isCompatibleVersion("26.1.9", range));
        assertFalse(CivitasMixinPlugin.isCompatibleVersion("27.0.0", range));
        assertFalse(CivitasMixinPlugin.isCompatibleVersion("unknown", range));
    }

    @Test
    void acceptsWorldMapMinorUpdatesWithinMajorVersion() {
        CivitasMixinPlugin.VersionRange range = new CivitasMixinPlugin.VersionRange(1, 42);

        assertTrue(CivitasMixinPlugin.isCompatibleVersion("1.42.0", range));
        assertTrue(CivitasMixinPlugin.isCompatibleVersion("1.44.0", range));
        assertFalse(CivitasMixinPlugin.isCompatibleVersion("2.0.0", range));
    }
}
