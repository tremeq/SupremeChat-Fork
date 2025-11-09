package net.devscape.project.supremechat.chathead;

import net.devscape.project.supremechat.chathead.sources.CrafatarSource;
import net.devscape.project.supremechat.chathead.sources.MinotarSource;
import net.devscape.project.supremechat.chathead.sources.MojangSource;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Enhanced ChatHeadAPI with full offline mode support.
 *
 * Key Features:
 * - Automatic detection of server online/offline mode
 * - Intelligent skin source selection based on server mode
 * - New getHead(String playerName) method for offline mode
 * - Backwards compatible with original API
 *
 * Usage Examples:
 *
 * // Initialize (usually in onEnable)
 * ChatHeadAPI.initialize(plugin);
 *
 * // Get head in online mode (works as before)
 * BaseComponent[] head = ChatHeadAPI.getInstance().getHead(player.getUniqueId());
 *
 * // Get head in offline mode (NEW - recommended for offline servers)
 * BaseComponent[] head = ChatHeadAPI.getInstance().getHead(player.getName());
 *
 * // The API automatically detects server mode and uses appropriate method
 * BaseComponent[] head = ChatHeadAPI.getInstance().getHeadSmart(player);
 */
public class ChatHeadAPI {

    public static SkinSource defaultSource;
    private static ChatHeadAPI instance;

    private final JavaPlugin plugin;
    private final HeadCache headCache;
    private final boolean isOnlineMode;
    private final boolean enabled;
    private final boolean useOverlayByDefault;

    private ChatHeadAPI(JavaPlugin plugin) {
        this.plugin = plugin;
        this.headCache = new HeadCache(plugin);
        this.isOnlineMode = Bukkit.getServer().getOnlineMode();
        this.enabled = plugin.getConfig().getBoolean("chathead.enabled", true);
        this.useOverlayByDefault = plugin.getConfig().getBoolean("chathead.use-overlay-by-default", true);

        plugin.getLogger().info("ChatHeadAPI initialized in " +
            (isOnlineMode ? "ONLINE" : "OFFLINE") + " mode");
        plugin.getLogger().info("ChatHeadAPI status: " + (enabled ? "ENABLED" : "DISABLED"));
    }

    public static ChatHeadAPI getInstance() {
        if (instance == null) {
            throw new IllegalArgumentException("ChatHeadAPI has not been initialized.");
        }
        return instance;
    }

    /**
     * Initialize the ChatHeadAPI with automatic server mode detection.
     *
     * @param plugin Your plugin instance
     */
    public static void initialize(JavaPlugin plugin) {
        if (instance != null) {
            throw new IllegalStateException("ChatHeadAPI has already been initialized.");
        }

        // Check if ChatHead is enabled in config
        boolean enabled = plugin.getConfig().getBoolean("chathead.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("ChatHeadAPI is DISABLED in config - skipping initialization");
            return; // Don't initialize if disabled
        }

        boolean isOnlineMode = Bukkit.getServer().getOnlineMode();
        String configSource = plugin.getConfig().getString("chathead.skin-source", "AUTO");

        // Intelligent source selection based on server mode
        if (configSource.equalsIgnoreCase("AUTO")) {
            if (isOnlineMode) {
                // Online mode: use Mojang (most reliable for premium players)
                defaultSource = new MojangSource();
                plugin.getLogger().info("ChatHeadAPI: Auto-selected MOJANG source for online mode");
            } else {
                // Offline mode: use Minotar (supports username retrieval)
                defaultSource = new MinotarSource(false); // Use username, not UUID
                plugin.getLogger().info("ChatHeadAPI: Auto-selected MINOTAR source for offline mode");
            }
        } else {
            // Manual configuration
            String sourceUpper = configSource.toUpperCase();
            switch (sourceUpper) {
                case "CRAFATAR":
                    defaultSource = new CrafatarSource();
                    break;
                case "MINOTAR":
                    defaultSource = new MinotarSource(false); // Force username for offline compatibility
                    break;
                case "MCHEADS":
                    defaultSource = new MinotarSource(false); // Minotar is used for both
                    break;
                default:
                    defaultSource = new MojangSource();
                    break;
            }
            plugin.getLogger().info("ChatHeadAPI: Using configured source: " + configSource);
        }

        // Warn if incompatible source is selected for offline mode
        if (!isOnlineMode && !defaultSource.getSkinSource().isOfflineModeCompatible()) {
            plugin.getLogger().warning("WARNING: Selected skin source " + configSource +
                " is not compatible with offline mode!");
            plugin.getLogger().warning("Player heads may not load correctly. Consider using MINOTAR instead.");
        }

        instance = new ChatHeadAPI(plugin);
    }

    // ==================== UUID-based methods (original API) ====================

    public BaseComponent[] getHead(UUID uuid) {
        if (!enabled) return new BaseComponent[]{};
        return headCache.getCachedHead(uuid, useOverlayByDefault, defaultSource);
    }

    public BaseComponent[] getHead(UUID uuid, boolean overlay) {
        if (!enabled) return new BaseComponent[]{};
        return headCache.getCachedHead(uuid, overlay, defaultSource);
    }

    public BaseComponent[] getHead(UUID uuid, boolean overlay, SkinSource skinSource) {
        if (!enabled) return new BaseComponent[]{};
        return headCache.getCachedHead(uuid, overlay, skinSource);
    }

    public BaseComponent[] getHead(OfflinePlayer player) {
        if (!enabled) return new BaseComponent[]{};
        return headCache.getCachedHead(player, useOverlayByDefault, defaultSource);
    }

    public BaseComponent[] getHead(OfflinePlayer player, boolean overlay) {
        if (!enabled) return new BaseComponent[]{};
        return headCache.getCachedHead(player, overlay, defaultSource);
    }

    public BaseComponent[] getHead(OfflinePlayer player, boolean overlay, SkinSource skinSource) {
        if (!enabled) return new BaseComponent[]{};
        return headCache.getCachedHead(player, overlay, skinSource);
    }

    // ==================== NEW: Name-based methods (offline mode) ====================

    /**
     * NEW: Get player head by name - essential for offline mode!
     *
     * In offline mode, UUID is locally generated and unreliable for skin retrieval.
     * This method uses the player's name directly to fetch the skin from external services.
     *
     * @param playerName The player's name
     * @return BaseComponent array representing the player's head
     */
    public BaseComponent[] getHead(String playerName) {
        if (!enabled) return new BaseComponent[]{};
        return getHead(playerName, useOverlayByDefault);
    }

    /**
     * NEW: Get player head by name with overlay option.
     *
     * @param playerName The player's name
     * @param overlay Whether to include the second skin layer (helmet)
     * @return BaseComponent array representing the player's head
     */
    public BaseComponent[] getHead(String playerName, boolean overlay) {
        if (!enabled) return new BaseComponent[]{};
        return getHead(playerName, overlay, defaultSource);
    }

    /**
     * NEW: Get player head by name with custom skin source.
     *
     * @param playerName The player's name
     * @param overlay Whether to include the second skin layer
     * @param skinSource The skin source to use (must support username retrieval)
     * @return BaseComponent array representing the player's head
     */
    public BaseComponent[] getHead(String playerName, boolean overlay, SkinSource skinSource) {
        if (!enabled) return new BaseComponent[]{};
        if (!skinSource.hasUsernameSupport()) {
            throw new UnsupportedOperationException(
                "SkinSource " + skinSource.getSkinSource() + " does not support username-based retrieval. " +
                "Use MinotarSource for offline mode servers."
            );
        }
        return headCache.getCachedHeadByName(playerName, overlay, skinSource);
    }

    // ==================== NEW: Smart method (auto-detects best approach) ====================

    /**
     * NEW: Intelligent method that automatically uses the best approach based on server mode.
     *
     * - In online mode: uses UUID (more reliable)
     * - In offline mode: uses player name (works with cracked UUIDs)
     *
     * This is the recommended method to use in your code!
     *
     * @param player The player
     * @return BaseComponent array representing the player's head
     */
    public BaseComponent[] getHeadSmart(OfflinePlayer player) {
        if (!enabled) return new BaseComponent[]{};
        return getHeadSmart(player, useOverlayByDefault);
    }

    /**
     * NEW: Intelligent method with overlay option.
     *
     * @param player The player
     * @param overlay Whether to include the second skin layer
     * @return BaseComponent array representing the player's head
     */
    public BaseComponent[] getHeadSmart(OfflinePlayer player, boolean overlay) {
        if (!enabled) return new BaseComponent[]{};
        if (isOnlineMode) {
            // Online mode: use UUID
            return headCache.getCachedHead(player, overlay, defaultSource);
        } else {
            // Offline mode: use player name
            String playerName = player.getName();
            if (playerName == null || playerName.isEmpty()) {
                plugin.getLogger().warning("Cannot retrieve head for player with null/empty name");
                return new BaseComponent[]{};
            }
            return headCache.getCachedHeadByName(playerName, overlay, defaultSource);
        }
    }

    // ==================== String conversion methods ====================

    public String getHeadAsString(UUID uuid, boolean overlay, SkinSource skinSource) {
        return getHeadAsString(Bukkit.getOfflinePlayer(uuid), overlay, skinSource);
    }

    public String getHeadAsString(OfflinePlayer player, boolean overlay, SkinSource skinSource) {
        return TextComponent.toLegacyText(this.getHead(player, overlay, skinSource));
    }

    public String getHeadAsString(OfflinePlayer player) {
        return getHeadAsString(player, true, defaultSource);
    }

    /**
     * NEW: Get head as string by player name.
     */
    public String getHeadAsString(String playerName) {
        return TextComponent.toLegacyText(this.getHead(playerName));
    }

    /**
     * NEW: Get head as string by player name with overlay.
     */
    public String getHeadAsString(String playerName, boolean overlay) {
        return TextComponent.toLegacyText(this.getHead(playerName, overlay));
    }

    // ==================== Utility methods ====================

    /**
     * Check if the ChatHeadAPI is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if the server is in online mode.
     */
    public boolean isOnlineMode() {
        return isOnlineMode;
    }

    /**
     * Get the default skin source being used.
     */
    public SkinSource getDefaultSource() {
        return defaultSource;
    }

    /**
     * Check if overlay is used by default.
     */
    public boolean isUseOverlayByDefault() {
        return useOverlayByDefault;
    }

    /**
     * Shutdown the API and cleanup resources.
     */
    public void shutdown() {
        if (headCache != null) {
            headCache.shutdown();
        }
    }
}
