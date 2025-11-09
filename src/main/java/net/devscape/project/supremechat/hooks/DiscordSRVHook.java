package net.devscape.project.supremechat.hooks;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.devscape.project.supremechat.SupremeChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Hook for DiscordSRV integration
 * Allows SupremeChat messages to be sent to Discord
 *
 * Features enhanced debug logging for troubleshooting
 */
public class DiscordSRVHook {

    private static boolean enabled = false;
    private static boolean available = false;
    private static int messagesSent = 0;
    private static int messagesFiltered = 0;
    private static int messagesFailed = 0;
    private static String lastError = "None";

    /**
     * Initialize the DiscordSRV hook
     * Call this during plugin initialization
     */
    public static void initialize() {
        boolean debug = SupremeChat.getInstance().getConfig().getBoolean("discordsrv.debug", false);

        debugLog("===========================================");
        debugLog("DISCORDSRV INTEGRATION INITIALIZATION");
        debugLog("===========================================");

        // Reset statistics
        messagesSent = 0;
        messagesFiltered = 0;
        messagesFailed = 0;
        lastError = "None";

        // Check if DiscordSRV plugin is loaded
        Plugin discordSRVPlugin = Bukkit.getPluginManager().getPlugin("DiscordSRV");

        if (discordSRVPlugin == null) {
            debugLog("[CHECK] DiscordSRV plugin: NOT FOUND");
            SupremeChat.getInstance().getLogger().warning("[DiscordSRV] Plugin not found, integration disabled.");
            SupremeChat.getInstance().getLogger().warning("[DiscordSRV] Please install DiscordSRV to use this feature.");
            available = false;
            enabled = false;
            debugLog("[RESULT] Integration Status: DISABLED (Plugin not found)");
            debugLog("===========================================");
            return;
        }

        debugLog("[CHECK] DiscordSRV plugin: FOUND");
        debugLog("[INFO] DiscordSRV version: " + discordSRVPlugin.getDescription().getVersion());
        debugLog("[INFO] DiscordSRV enabled: " + discordSRVPlugin.isEnabled());

        // Check if integration is enabled in config
        boolean configEnabled = SupremeChat.getInstance().getConfig().getBoolean("discordsrv.enabled", true);
        debugLog("[CHECK] Config 'discordsrv.enabled': " + configEnabled);

        if (!configEnabled) {
            SupremeChat.getInstance().getLogger().info("[DiscordSRV] Integration disabled in config.");
            available = true;
            enabled = false;
            debugLog("[RESULT] Integration Status: DISABLED (Config setting)");
            debugLog("===========================================");
            return;
        }

        // Check if DiscordSRV is ready
        try {
            boolean discordSRVReady = DiscordSRV.isReady;
            debugLog("[CHECK] DiscordSRV ready state: " + discordSRVReady);

            if (!discordSRVReady) {
                debugLog("[WARNING] DiscordSRV is not ready yet - this is normal during startup");
                debugLog("[INFO] Integration will still work once DiscordSRV finishes loading");
            }
        } catch (Exception e) {
            debugLog("[ERROR] Failed to check DiscordSRV ready state: " + e.getMessage());
        }

        available = true;
        enabled = true;

        // Log configuration
        debugLog("-------------------------------------------");
        debugLog("CONFIGURATION:");
        debugLog("  Channel: " + SupremeChat.getInstance().getConfig().getString("discordsrv.channel", "global"));
        debugLog("  Required Permission: " + SupremeChat.getInstance().getConfig().getString("discordsrv.required-permission", "(none)"));
        debugLog("  Filter Enabled: " + SupremeChat.getInstance().getConfig().getBoolean("discordsrv.filter-enabled", false));
        debugLog("  Filter Prefix: " + SupremeChat.getInstance().getConfig().getString("discordsrv.filter-prefix", "(none)"));
        debugLog("  Filtered Words: " + SupremeChat.getInstance().getConfig().getStringList("discordsrv.filtered-words").size() + " words");
        debugLog("  Debug Mode: " + debug);
        debugLog("-------------------------------------------");

        SupremeChat.getInstance().getLogger().info("[DiscordSRV] Integration enabled successfully!");
        debugLog("[RESULT] Integration Status: ENABLED");
        debugLog("===========================================");
    }

    /**
     * Check if the hook is enabled
     * @return true if DiscordSRV integration is active
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if DiscordSRV is available but disabled by config
     * @return true if DiscordSRV is available
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * Send a chat message to Discord
     * @param player The player who sent the message
     * @param message The message content (already stripped of formatting if needed)
     * @param cancelled Whether the event was cancelled
     */
    public static void sendToDiscord(Player player, String message, boolean cancelled) {
        boolean debug = SupremeChat.getInstance().getConfig().getBoolean("discordsrv.debug", false);

        // Pre-checks with detailed logging
        if (!enabled) {
            debugLog("[SEND] Message blocked: Integration not enabled");
            return;
        }

        if (player == null) {
            debugLog("[SEND] Message blocked: Player is null");
            return;
        }

        if (message == null || message.isEmpty()) {
            debugLog("[SEND] Message blocked: Message is null or empty");
            return;
        }

        debugLog("-------------------------------------------");
        debugLog("[SEND] Processing message from: " + player.getName());
        debugLog("[SEND] Message content: " + message);
        debugLog("[SEND] Event cancelled: " + cancelled);

        // Check if player has permission to send to Discord
        String permission = SupremeChat.getInstance().getConfig().getString("discordsrv.required-permission", "");
        if (!permission.isEmpty()) {
            boolean hasPermission = player.hasPermission(permission);
            debugLog("[SEND] Required permission: " + permission);
            debugLog("[SEND] Player has permission: " + hasPermission);

            if (!hasPermission) {
                debugLog("[SEND] Message blocked: Player lacks required permission");
                messagesFiltered++;
                return;
            }
        } else {
            debugLog("[SEND] No permission required");
        }

        // Check if message should be filtered
        if (shouldFilterMessage(message)) {
            debugLog("[SEND] Message blocked: Filtered by filter rules");
            messagesFiltered++;
            return;
        }

        try {
            // Get the Discord channel name from config
            String channel = SupremeChat.getInstance().getConfig().getString("discordsrv.channel", "global");
            debugLog("[SEND] Target Discord channel: " + channel);

            // Convert message to Adventure Component (DiscordSRV uses Kyori Adventure)
            Component component = LegacyComponentSerializer.legacySection().deserialize(message);
            debugLog("[SEND] Message converted to Adventure Component");

            // Check if DiscordSRV is ready
            if (!DiscordSRV.isReady) {
                debugLog("[SEND] WARNING: DiscordSRV is not ready yet!");
                debugLog("[SEND] Message will be queued by DiscordSRV");
            }

            // Send to DiscordSRV for processing
            debugLog("[SEND] Calling DiscordSRV.processChatMessage()...");
            DiscordSRV.getPlugin().processChatMessage(
                    player,
                    component,
                    channel,
                    cancelled,
                    null // We don't have the original event anymore
            );

            messagesSent++;
            debugLog("[SEND] SUCCESS: Message sent to DiscordSRV (Total sent: " + messagesSent + ")");

            if (debug) {
                SupremeChat.getInstance().getLogger().info(
                        "[DiscordSRV-DEBUG] Sent: " + player.getName() + ": " + message
                );
            }

        } catch (NoClassDefFoundError e) {
            messagesFailed++;
            lastError = "NoClassDefFoundError - DiscordSRV classes not found: " + e.getMessage();
            SupremeChat.getInstance().getLogger().severe(
                    "[DiscordSRV] CRITICAL: DiscordSRV classes not found! Is DiscordSRV properly installed?"
            );
            debugLog("[SEND] CRITICAL ERROR: " + lastError);
            if (debug) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            messagesFailed++;
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            SupremeChat.getInstance().getLogger().warning(
                    "[DiscordSRV] Failed to send message: " + lastError
            );
            debugLog("[SEND] ERROR: " + lastError);
            debugLog("[SEND] Stack trace:");
            if (debug) {
                e.printStackTrace();
            } else {
                debugLog("  (Enable debug mode for full stack trace)");
            }
        }

        debugLog("-------------------------------------------");
    }

    /**
     * Check if a message should be filtered (not sent to Discord)
     * @param message The message to check
     * @return true if the message should be filtered
     */
    private static boolean shouldFilterMessage(String message) {
        boolean filterEnabled = SupremeChat.getInstance().getConfig().getBoolean("discordsrv.filter-enabled", false);

        if (!filterEnabled) {
            debugLog("[FILTER] Filter disabled, message allowed");
            return false;
        }

        debugLog("[FILTER] Checking message against filters...");

        // Check prefix filter
        String prefix = SupremeChat.getInstance().getConfig().getString("discordsrv.filter-prefix", "");
        if (!prefix.isEmpty() && message.startsWith(prefix)) {
            debugLog("[FILTER] BLOCKED: Message starts with filtered prefix '" + prefix + "'");
            return true;
        }

        // Check if message contains filtered words
        List<String> filteredWords = SupremeChat.getInstance().getConfig().getStringList("discordsrv.filtered-words");
        for (String filtered : filteredWords) {
            if (message.toLowerCase().contains(filtered.toLowerCase())) {
                debugLog("[FILTER] BLOCKED: Message contains filtered word '" + filtered + "'");
                return true;
            }
        }

        debugLog("[FILTER] Message passed all filters");
        return false;
    }

    /**
     * Reload the hook configuration
     */
    public static void reload() {
        debugLog("[RELOAD] Reloading DiscordSRV integration...");
        initialize();
    }

    /**
     * Generate a detailed debug report for troubleshooting
     * @return List of debug information lines
     */
    public static List<String> generateDebugReport() {
        List<String> report = new ArrayList<>();

        report.add("===========================================");
        report.add("DISCORDSRV INTEGRATION DEBUG REPORT");
        report.add("===========================================");
        report.add("");

        // Plugin status
        report.add("--- PLUGIN STATUS ---");
        Plugin discordSRV = Bukkit.getPluginManager().getPlugin("DiscordSRV");
        if (discordSRV != null) {
            report.add("DiscordSRV Found: YES");
            report.add("DiscordSRV Version: " + discordSRV.getDescription().getVersion());
            report.add("DiscordSRV Enabled: " + discordSRV.isEnabled());
            try {
                report.add("DiscordSRV Ready: " + DiscordSRV.isReady);
            } catch (Exception e) {
                report.add("DiscordSRV Ready: ERROR - " + e.getMessage());
            }
        } else {
            report.add("DiscordSRV Found: NO");
        }
        report.add("");

        // Integration status
        report.add("--- INTEGRATION STATUS ---");
        report.add("Available: " + available);
        report.add("Enabled: " + enabled);
        report.add("");

        // Configuration
        report.add("--- CONFIGURATION ---");
        report.add("Enabled: " + SupremeChat.getInstance().getConfig().getBoolean("discordsrv.enabled", true));
        report.add("Channel: " + SupremeChat.getInstance().getConfig().getString("discordsrv.channel", "global"));
        report.add("Required Permission: " + SupremeChat.getInstance().getConfig().getString("discordsrv.required-permission", "(none)"));
        report.add("Filter Enabled: " + SupremeChat.getInstance().getConfig().getBoolean("discordsrv.filter-enabled", false));
        report.add("Filter Prefix: " + SupremeChat.getInstance().getConfig().getString("discordsrv.filter-prefix", "(none)"));
        report.add("Filtered Words: " + SupremeChat.getInstance().getConfig().getStringList("discordsrv.filtered-words"));
        report.add("Debug Mode: " + SupremeChat.getInstance().getConfig().getBoolean("discordsrv.debug", false));
        report.add("");

        // Statistics
        report.add("--- STATISTICS ---");
        report.add("Messages Sent: " + messagesSent);
        report.add("Messages Filtered: " + messagesFiltered);
        report.add("Messages Failed: " + messagesFailed);
        report.add("Last Error: " + lastError);
        report.add("");

        // Server info
        report.add("--- SERVER INFO ---");
        report.add("Bukkit Version: " + Bukkit.getVersion());
        report.add("Online Players: " + Bukkit.getOnlinePlayers().size());
        report.add("");

        // SupremeChat info
        report.add("--- SUPREMECHAT INFO ---");
        report.add("Version: " + SupremeChat.getInstance().getDescription().getVersion());
        report.add("Chat Format Enabled: " + SupremeChat.getInstance().getConfig().getBoolean("enable-chat-format"));
        report.add("");

        report.add("===========================================");
        report.add("END OF DEBUG REPORT");
        report.add("===========================================");

        return report;
    }

    /**
     * Print debug report to console
     */
    public static void printDebugReport() {
        for (String line : generateDebugReport()) {
            SupremeChat.getInstance().getLogger().info(line);
        }
    }

    /**
     * Internal debug logging method
     * @param message Message to log
     */
    private static void debugLog(String message) {
        if (SupremeChat.getInstance().getConfig().getBoolean("discordsrv.debug", false)) {
            SupremeChat.getInstance().getLogger().info("[DiscordSRV-DEBUG] " + message);
        }
    }
}
