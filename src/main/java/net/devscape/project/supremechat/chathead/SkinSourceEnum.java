package net.devscape.project.supremechat.chathead;

/**
 * Enum representing different sources from which to retrieve player skin information.
 * Enhanced with offline mode support information.
 */
public enum SkinSourceEnum {
    /**
     * Mojang API - requires valid UUID, not suitable for offline mode
     */
    MOJANG(false),

    /**
     * Crafatar - supports UUID only, not suitable for offline mode
     */
    CRAFATAR(false),

    /**
     * Minotar - supports both UUID and username, works in offline mode
     */
    MINOTAR(true),

    /**
     * mc-heads - supports both UUID and username, works in offline mode
     */
    MCHEADS(true);

    private final boolean offlineModeCompatible;

    SkinSourceEnum(boolean offlineModeCompatible) {
        this.offlineModeCompatible = offlineModeCompatible;
    }

    /**
     * Check if this skin source is compatible with offline mode servers
     * @return true if this source can retrieve skins using player names
     */
    public boolean isOfflineModeCompatible() {
        return offlineModeCompatible;
    }
}
