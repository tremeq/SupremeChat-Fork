package net.devscape.project.supremechat.hooks;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Hook for Floodgate/Geyser integration.
 *
 * Floodgate allows Bedrock Edition players to join Java Edition servers.
 * This hook detects Bedrock players to disable ChatHeads for them,
 * preventing "confused characters" in their chat.
 *
 * Features:
 * - Detects Bedrock Edition players via Floodgate API
 * - Optional integration (works without Floodgate installed)
 * - Configurable via config.yml
 *
 * @author SupremeChat
 * @version 1.0
 */
public class FloodgateHook {

    private static boolean available = false;
    private static FloodgateApi floodgateApi = null;

    /**
     * Initialize Floodgate integration.
     * Called during plugin startup if Floodgate is detected.
     */
    public static void initialize() {
        try {
            floodgateApi = FloodgateApi.getInstance();
            available = true;
        } catch (Exception e) {
            available = false;
            throw new RuntimeException("Failed to initialize Floodgate API", e);
        }
    }

    /**
     * Check if Floodgate integration is available and working.
     *
     * @return true if Floodgate is installed and initialized
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * Check if a player is a Bedrock Edition player.
     *
     * This method is safe to call even if Floodgate is not installed.
     * It will return false if Floodgate is not available.
     *
     * @param player The player to check
     * @return true if the player is from Bedrock Edition, false otherwise
     */
    public static boolean isBedrockPlayer(Player player) {
        if (!available || floodgateApi == null) {
            return false;
        }

        try {
            return floodgateApi.isFloodgatePlayer(player.getUniqueId());
        } catch (Exception e) {
            // If any error occurs, assume not a Bedrock player
            return false;
        }
    }

    /**
     * Get the Bedrock player's original username.
     * Bedrock players have a prefix (usually ".") added to their name.
     *
     * @param player The Bedrock player
     * @return The original Bedrock username, or regular name if not a Bedrock player
     */
    public static String getBedrockUsername(Player player) {
        if (!available || floodgateApi == null || !isBedrockPlayer(player)) {
            return player.getName();
        }

        try {
            return floodgateApi.getPlayer(player.getUniqueId()).getUsername();
        } catch (Exception e) {
            return player.getName();
        }
    }

    /**
     * Disable Floodgate integration.
     * Called during plugin shutdown or reload.
     */
    public static void disable() {
        available = false;
        floodgateApi = null;
    }
}
