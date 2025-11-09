package net.devscape.project.supremechat.chathead.examples;

import net.devscape.project.supremechat.chathead.ChatHeadAPI;
import net.devscape.project.supremechat.chathead.SkinSource;
import net.devscape.project.supremechat.chathead.sources.MinotarSource;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Example class demonstrating how to use the ChatHeadAPI with offline mode support.
 *
 * This example shows various ways to retrieve and display player heads in chat.
 */
public class ChatHeadExample implements Listener {

    /**
     * Example 1: Simple usage with automatic mode detection (RECOMMENDED)
     *
     * This method automatically uses the best approach based on server mode:
     * - Online mode: uses UUID
     * - Offline mode: uses player name
     */
    @EventHandler
    public void onPlayerJoin_SmartExample(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Get the ChatHead API instance
        ChatHeadAPI api = ChatHeadAPI.getInstance();

        // Use the smart method - it automatically detects server mode!
        BaseComponent[] head = api.getHeadSmart(player);

        // Create a welcome message with the player's head
        TextComponent message = new TextComponent("Welcome ");
        // Add each component from the head array
        for (BaseComponent component : head) {
            message.addExtra(component);
        }
        message.addExtra(new TextComponent(" " + player.getName() + "!"));

        // Send to player
        player.spigot().sendMessage(message);
    }

    /**
     * Example 2: Using player name directly (for offline mode)
     *
     * This is the KEY method for offline mode servers!
     * It retrieves the skin using the player's name, bypassing UUID issues.
     */
    public void sendMessageWithHeadByName(Player player, String targetPlayerName) {
        ChatHeadAPI api = ChatHeadAPI.getInstance();

        // Get head by player name - works perfectly in offline mode!
        BaseComponent[] head = api.getHead(targetPlayerName);

        // Create message
        TextComponent message = new TextComponent("");
        // Add each component from the head array
        for (BaseComponent component : head) {
            message.addExtra(component);
        }
        message.addExtra(new TextComponent(" " + targetPlayerName + " says hi!"));

        player.spigot().sendMessage(message);
    }

    /**
     * Example 3: Using specific skin source (advanced)
     *
     * You can force a specific skin source if needed.
     * For offline mode, always use MinotarSource!
     */
    public void sendMessageWithCustomSource(Player player) {
        ChatHeadAPI api = ChatHeadAPI.getInstance();

        // Create a MinotarSource (best for offline mode)
        SkinSource minotarSource = new MinotarSource(false); // false = use name, not UUID

        // Get head with custom source
        BaseComponent[] head = api.getHead(player.getName(), true, minotarSource);

        // Create message
        TextComponent message = new TextComponent("Custom source: ");
        // Add each component from the head array
        for (BaseComponent component : head) {
            message.addExtra(component);
        }
        message.addExtra(new TextComponent(" " + player.getName()));

        player.spigot().sendMessage(message);
    }

    /**
     * Example 4: Multiple heads in one message
     */
    public void sendMultipleHeads(Player player, String playerName1, String playerName2) {
        ChatHeadAPI api = ChatHeadAPI.getInstance();

        // Get multiple heads
        BaseComponent[] head1 = api.getHead(playerName1);
        BaseComponent[] head2 = api.getHead(playerName2);

        // Create message with multiple heads
        TextComponent message = new TextComponent("Chat between ");
        // Add first head
        for (BaseComponent component : head1) {
            message.addExtra(component);
        }
        message.addExtra(new TextComponent(" " + playerName1 + " and "));
        // Add second head
        for (BaseComponent component : head2) {
            message.addExtra(component);
        }
        message.addExtra(new TextComponent(" " + playerName2));

        player.spigot().sendMessage(message);
    }

    /**
     * Example 5: Without overlay (just the base skin layer)
     */
    public void sendHeadWithoutOverlay(Player player) {
        ChatHeadAPI api = ChatHeadAPI.getInstance();

        // false = no overlay (no helmet layer)
        BaseComponent[] head = api.getHead(player.getName(), false);

        TextComponent message = new TextComponent("No overlay: ");
        // Add each component from the head array
        for (BaseComponent component : head) {
            message.addExtra(component);
        }

        player.spigot().sendMessage(message);
    }

    /**
     * Example 6: Converting to legacy string (for older Minecraft versions)
     */
    public void sendHeadAsLegacyString(Player player) {
        ChatHeadAPI api = ChatHeadAPI.getInstance();

        // Get head as legacy string
        String headString = api.getHeadAsString(player.getName());

        // Send as legacy message
        player.sendMessage(headString + " " + player.getName());
    }

    /**
     * Example 7: Using in PlaceholderAPI (conceptual example)
     *
     * You could create a PlaceholderAPI expansion that returns player heads:
     * %supremechat_head_<playername>%
     */
    public String getHeadForPlaceholder(String playerName) {
        try {
            ChatHeadAPI api = ChatHeadAPI.getInstance();
            return api.getHeadAsString(playerName);
        } catch (Exception e) {
            return "[HEAD]"; // Fallback
        }
    }

    /**
     * Example 8: Checking server mode
     */
    public void checkServerMode(Player player) {
        ChatHeadAPI api = ChatHeadAPI.getInstance();

        if (api.isOnlineMode()) {
            player.sendMessage("Server is in ONLINE mode - using UUID-based skin retrieval");
        } else {
            player.sendMessage("Server is in OFFLINE mode - using name-based skin retrieval");
        }

        player.sendMessage("Current skin source: " + api.getDefaultSource().getSkinSource());
    }
}
