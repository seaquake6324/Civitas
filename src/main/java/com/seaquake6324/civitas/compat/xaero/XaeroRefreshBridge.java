package com.seaquake6324.civitas.compat.xaero;

public final class XaeroRefreshBridge {
    public static void refresh() {
        refreshMinimap();
        refreshWorldMap();
    }

    private static void refreshMinimap() {
        try {
            xaero.common.XaeroMinimapSession session = xaero.common.XaeroMinimapSession.getCurrentSession();
            if (session != null && session.getMinimapProcessor() != null
                    && session.getMinimapProcessor().getMinimapWriter().getDimensionHighlightHandler() != null) {
                session.getMinimapProcessor().getMinimapWriter().getDimensionHighlightHandler().requestRefresh();
            }
        } catch (RuntimeException | LinkageError ignored) {}
    }

    private static void refreshWorldMap() {
        try {
            xaero.map.WorldMapSession session = xaero.map.WorldMapSession.getCurrentSession();
            if (session == null || session.getMapProcessor() == null || session.getMapProcessor().getMapWorld() == null) return;
            for (xaero.map.world.MapDimension dimension : session.getMapProcessor().getMapWorld().getDimensionsList()) {
                dimension.getHighlightHandler().clearCachedHashes();
            }
        } catch (RuntimeException | LinkageError ignored) {}
    }

    private XaeroRefreshBridge() {}
}
