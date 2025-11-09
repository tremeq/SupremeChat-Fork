package net.devscape.project.supremechat.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import net.devscape.project.supremechat.SupremeChat;
import net.devscape.project.supremechat.hooks.DiscordSRVHook;
import net.devscape.project.supremechat.hooks.FloodgateHook;
import net.devscape.project.supremechat.object.Channel;
import net.devscape.project.supremechat.utils.FormatUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.devscape.project.supremechat.chathead.ChatHeadAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.devscape.project.supremechat.utils.FormatUtil.emojiReplacer;
import static net.devscape.project.supremechat.utils.Message.*;

public class Formatting implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();

        // Respect if the event has already been cancelled by another plugin
        if (e.isCancelled()) {
            return;
        }

        boolean repeated = false;
        boolean containsbadword = false;

        boolean anti_bot = SupremeChat.getInstance().getConfig().getBoolean("anti-bot.chat");

        if (anti_bot) {
            if (SupremeChat.getInstance().getPrevention().contains(player)) {
                e.setCancelled(true);
                String detect_alert = SupremeChat.getInstance().getConfig().getString("anti-bot.message");
                detect_alert = detect_alert.replace("%name%", player.getName());

                msgPlayer(player, detect_alert);
                return;
            }
        }

        if (SupremeChat.getInstance().getConfig().getBoolean("mute-chat")) {
            if (!player.hasPermission(Objects.requireNonNull(SupremeChat.getInstance().getConfig().getString("bypass-mute-chat-permission"))) || !player.isOp()) {
                msgPlayer(player, "&cChat is currently muted!");
                e.setCancelled(true);
                return;
            }
        }

        // BANNED WORD DETECTION
        if (SupremeChat.getInstance().getConfig().getBoolean("word-detect-enable")) {
            if (!player.hasPermission("supremechat.bypass.filter") && !player.isOp()) {
                for (String word : SupremeChat.getInstance().getConfig().getStringList("banned-words")) {
                    if (isWordBlocked(e.getMessage(), word)) {
                        e.setCancelled(true);

                        containsbadword = true;

                        String detect = SupremeChat.getInstance().getConfig().getString("word-detect");
                        detect = detect.replace("%word%", word);

                        msgPlayer(player, detect);

                        // alert staff
                        for (Player staff : Bukkit.getOnlinePlayers()) {
                            if (staff.hasPermission(SupremeChat.getInstance().getConfig().getString("detect-alert-staff-permission"))) {
                                String detect_alert = SupremeChat.getInstance().getConfig().getString("word-detect-staff");
                                detect_alert = detect_alert.replaceAll("%message%", e.getMessage());
                                detect_alert = detect_alert.replace("%name%", player.getName());

                                msgPlayer(staff, detect_alert);
                                break;
                            }
                        }

                        createLog(player, e.getMessage() + " (BANNED WORD)", false);
                        break;
                    }
                }
            }
        }


        // CHAT DELAY
        if (SupremeChat.getInstance().getConfig().getInt("chat-delay") >= 1) {
            if (!containsbadword) {
                if (!player.hasPermission("supremechat.bypass.chatcooldown") && !player.isOp()) {
                    if (!SupremeChat.getInstance().getChatDelayList().contains(player)) {
                        SupremeChat.getInstance().getChatDelayList().add(player);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                SupremeChat.getInstance().getChatDelayList().remove(player);
                            }
                        }.runTaskLaterAsynchronously(SupremeChat.getInstance(), 20L * SupremeChat.getInstance().getConfig().getInt("chat-delay"));
                    } else {
                        e.setCancelled(true);
                        // Only send warning message if enabled in config
                        if (SupremeChat.getInstance().getConfig().getBoolean("chat-warn-enabled", true)) {
                            msgPlayer(player, SupremeChat.getInstance().getConfig().getString("chat-warn"));
                        }
                    }
                }
            }
        }

        // REPEAT FILTER
        if (SupremeChat.getInstance().getConfig().getBoolean("repeat-enable")) {
            if (!containsbadword) {
                if (!player.hasPermission("supremechat.bypass.filter") && !player.isOp()) {
                    if (SupremeChat.getInstance().getLastMessage().containsKey(player)) {
                        String lastMessage = SupremeChat.getInstance().getLastMessage().get(player);
                        String newMessage = e.getMessage();

                        if (newMessage.contains(lastMessage)) {
                            e.setCancelled(true);

                            repeated = true;

                            msgPlayer(player, SupremeChat.getInstance().getConfig().getString("repeat-warn"));
                            createLog(player, e.getMessage() + " (REPEATED)", false);
                        } else {
                            SupremeChat.getInstance().getLastMessage().remove(player);
                            SupremeChat.getInstance().getLastMessage().put(player, newMessage);
                        }
                    } else {
                        String newMessage = e.getMessage();
                        SupremeChat.getInstance().getLastMessage().put(player, newMessage);
                    }
                }
            }
        }


        // CAPS FILTER
        if (SupremeChat.getInstance().getConfig().getBoolean("caps-lowercase")) {
            if (!player.hasPermission("supremechat.bypass.filter") && !player.isOp()) {
                if (e.getMessage().chars().filter(Character::isUpperCase).count() >= SupremeChat.getInstance().getConfig().getInt("caps-limit")) {
                    for (final char c : e.getMessage().toCharArray()) {
                        if (Character.isUpperCase(c)) {
                            if (!SupremeChat.getInstance().getConfig().getBoolean("disable-caps-warn")) {
                                msgPlayer(player, SupremeChat.getInstance().getConfig().getString("caps-warn"));
                            }
                            e.setMessage(format(e.getMessage().toLowerCase()));
                            break;
                        }
                    }
                }
            }
        }

        boolean itemChat = SupremeChat.getInstance().getConfig().getBoolean("enable-chat-item");


        if (itemChat) {
            // ITEM IN CHAT
            ItemStack item = player.getInventory().getItemInMainHand();

            String replacement = SupremeChat.getInstance().getConfig().getString("chat-item-replace");
            assert replacement != null;

            if (item.getItemMeta() != null) {
                String displayName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
                replacement = replacement.replaceAll("%item%", format("x" + item.getAmount() + " " + displayName));
            } else {
                replacement = replacement.replaceAll("%item%", format("x" + item.getAmount() + " " + item.getType().name()));
            }

            String message = e.getMessage();

            for (String itemString : SupremeChat.getInstance().getConfig().getStringList("chat-item-strings")) {
                if (message.contains(itemString)) {
                    e.setMessage(message.replace(itemString, format(replacement)));
                    break;
                }
            }
        }

        /// CHAT FORMATTING
        if (!containsbadword && !repeated) {
            if (SupremeChat.getInstance().getChannelManager().isInChannel(player)) {
                handleChannelFormat(e);
            } else {
                handleChatFormat(e);
            }
        } else {
            e.setCancelled(true);
        }
    }

    private void handleChatFormat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        SupremeChat plugin = SupremeChat.getInstance();

        if (e.isCancelled()) return;

        // Config checks
        boolean enableChatFormat = plugin.getConfig().getBoolean("enable-chat-format");
        boolean perWorldChat = plugin.getConfig().getBoolean("per-world-chat");
        String colorPermission = plugin.getConfig().getString("chat-color-permission", "supremechat.chat.color");
        List<String> disabledWorlds = plugin.getConfig().getStringList("disabled-worlds");

        if (disabledWorlds.contains(player.getWorld().getName())) return;
        if (!enableChatFormat) return;

        String originalMessage = e.getMessage();
        String cleanMessage = originalMessage;

        // Remove color codes if no permission
        if (!player.hasPermission(colorPermission)) {
            // This first line handles all legacy codes like &c and §c
            cleanMessage = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', originalMessage));

            // Now, let's strip all hex color codes supported by the plugin
            cleanMessage = cleanMessage.replaceAll("(?i)&#[a-f0-9]{6}", "");
            cleanMessage = cleanMessage.replaceAll("(?i)#[a-f0-9]{6}", "");
            cleanMessage = cleanMessage.replaceAll("(?i)\\{#[a-f0-9]{6}}", "");
            cleanMessage = cleanMessage.replaceAll("(?i)<#[a-f0-9]{6}>", "");
            cleanMessage = cleanMessage.replaceAll("(?i)<#&[a-f0-9]{6}>", "");
        }

        // Get chat format
        boolean grouping = plugin.getConfig().getBoolean("group-formatting");
        boolean debugMode = plugin.getConfig().getBoolean("debug-mode", false);

        // DEBUG: Log values
        if (debugMode) {
            plugin.getLogger().info("=== DEBUG CHAT FORMAT ===");
            plugin.getLogger().info("Player: " + player.getName());
            plugin.getLogger().info("Group formatting enabled: " + grouping);

            // DEBUG: Show what's actually in config
            plugin.getLogger().info("Global format from config: " + plugin.getConfig().getString("format"));
            if (plugin.getConfig().getConfigurationSection("groups") != null) {
                plugin.getLogger().info("Available groups in config: " + plugin.getConfig().getConfigurationSection("groups").getKeys(false));
            } else {
                plugin.getLogger().info("Available groups in config: NONE (section is null)");
            }
        }

        String playerRank = FormatUtil.getRank(player);
        if (debugMode) {
            plugin.getLogger().info("Player rank: " + playerRank);
        }

        String chatFormat = grouping ? getRankFormat(playerRank) : getGlobalFormat();
        if (debugMode) {
            plugin.getLogger().info("Initial chatFormat: " + chatFormat);
        }

        if (chatFormat == null) {
            if (debugMode) {
                plugin.getLogger().info("chatFormat is NULL, trying global format...");
            }
            chatFormat = getGlobalFormat();
            if (debugMode) {
                plugin.getLogger().info("Global format result: " + chatFormat);
            }
        }

        if (chatFormat == null) {
            plugin.getLogger().warning("FINAL chatFormat is NULL! Exiting...");
            return;
        }

        if (debugMode) {
            plugin.getLogger().info("Final chatFormat: " + chatFormat);
            plugin.getLogger().info("=== END DEBUG ===");
        }

        String formattedMessage = addChatPlaceholders(chatFormat, player)
                .replace("%message%", cleanMessage);
        formattedMessage = emojiReplacer(player, formattedMessage, false, true);

        // Allow other plugins like DiscordSRV to see the message
        e.setMessage(cleanMessage);

        // Send to DiscordSRV if enabled
        try {
            if (Bukkit.getPluginManager().getPlugin("DiscordSRV") != null) {
                DiscordSRVHook.sendToDiscord(player, cleanMessage, false);
            }
        } catch (NoClassDefFoundError ignored) {
            // DiscordSRV classes not available
        }

        // Build chat component for players with ChatHead
        TextComponent msg;

        // Check if ChatHead is enabled (follows resourcepack auto-send setting)
        boolean chatHeadEnabled = plugin.getConfig().getBoolean("chathead.resourcepack.auto-send", true);

        // Check if this is a Bedrock player (they can't see custom fonts)
        boolean disableForBedrock = plugin.getConfig().getBoolean("chathead.disable-for-bedrock", true);
        boolean isBedrockPlayer = false;
        if (disableForBedrock && FloodgateHook.isAvailable()) {
            isBedrockPlayer = FloodgateHook.isBedrockPlayer(player);
        }

        // Only show ChatHead for Java Edition players
        if (chatHeadEnabled && !isBedrockPlayer) {
            // Get player's head from ChatHeadAPI
            BaseComponent[] head = ChatHeadAPI.getInstance().getHeadSmart(player);

            if (head != null && head.length > 0) {
                // Build message with head prepended
                ComponentBuilder builder = new ComponentBuilder();

                // Add head components
                for (BaseComponent component : head) {
                    builder.append(component);
                }

                // Add space after head
                builder.append(" ");

                // Add formatted message
                builder.append(TextComponent.fromLegacyText(format(formattedMessage)));

                // Create final message from builder
                BaseComponent[] components = builder.create();
                msg = new TextComponent(components);
            } else {
                // Fallback if head is null
                msg = new TextComponent(TextComponent.fromLegacyText(format(formattedMessage)));
            }
        } else {
            // ChatHead disabled, use normal message
            msg = new TextComponent(TextComponent.fromLegacyText(format(formattedMessage)));
        }

        if (plugin.getConfig().getBoolean("hover.enable")) {
            ComponentBuilder hoverBuilder = new ComponentBuilder();
            for (String hoverLine : plugin.getConfig().getStringList("hover.string")) {
                hoverBuilder.append(new TextComponent(format(addOtherPlaceholders(hoverLine, player)))).append("\n");
            }
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverBuilder.create()));
        }

        if (plugin.getConfig().getBoolean("click.enable")) {
            ClickEvent clickEvent = createClickEvent(player);
            if (clickEvent != null) {
                msg.setClickEvent(clickEvent);
            }
        }

        // Send the formatted message manually to players
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!perWorldChat || online.getWorld().equals(player.getWorld())) {
                online.spigot().sendMessage(ChatMessageType.CHAT, msg);
            }
        }

        // Cancel default Bukkit chat broadcast (avoids duplicate + cleans console)
        e.setCancelled(true);

        // Manually log clean message to console
        String consoleOutput = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', formattedMessage));
        Bukkit.getConsoleSender().sendMessage(consoleOutput);
    }



    private void handleChannelFormat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();

        if (e.isCancelled()) {
            return;
        }

        e.setCancelled(true);

        boolean enableChatFormat = SupremeChat.getInstance().getConfig().getBoolean("enable-chat-format");
        String originalMessage = e.getMessage();

        // Send to DiscordSRV if enabled (for channel messages)
        try {
            if (Bukkit.getPluginManager().getPlugin("DiscordSRV") != null) {
                DiscordSRVHook.sendToDiscord(player, originalMessage, false);
            }
        } catch (NoClassDefFoundError ignored) {
            // DiscordSRV classes not available
        }
        String permission = SupremeChat.getInstance().getConfig().getString("chat-color-permission");

        List<String> worlds = SupremeChat.getInstance().getConfig().getStringList("disabled-worlds");

        // Skip formatting if the player is in a disabled world
        for (String wld : worlds) {
            if (player.getWorld().getName().equalsIgnoreCase(wld)) {
                return;
            }
        }

        if (enableChatFormat) {
            Channel c = SupremeChat.getInstance().getChannelManager().getChannel(player);
            String chatFormat = c.getFormat();
            List<Player> channelPlayers = new ArrayList<>();

            // Get all players in the same channel
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (SupremeChat.getInstance().getChannelManager().isInChannel(p)) {
                    if (SupremeChat.getInstance().getChannelManager().getChannel(p).getName().equalsIgnoreCase(c.getName())) {
                        channelPlayers.add(p);
                    }
                }
            }

            if (chatFormat != null) {
                boolean hover = SupremeChat.getInstance().getConfig().getBoolean("hover.enable");
                boolean click = SupremeChat.getInstance().getConfig().getBoolean("click.enable");

                // Process each player in the channel
                for (Player onlinePlayer : channelPlayers) {
                    List<String[]> hoverMessages = new ArrayList<>();

                    if (hover) {
                        for (String hoverMessage : SupremeChat.getInstance().getConfig().getStringList("hover.string")) {
                            hoverMessage = addOtherPlaceholders(hoverMessage, player);
                            TextComponent hoverComponent = new TextComponent(format(hoverMessage));
                            hoverMessages.add(hoverComponent.toLegacyText().split("\n"));
                        }
                    }

                    // Step 1: Replace placeholders in the chat format
                    String formattedMessage = addChatPlaceholders(chatFormat, player);

                    // Step 2: Replace %message% placeholder with the player's message
                    formattedMessage = formattedMessage.replace("%message%", originalMessage);

                    // Step 3: Replace emojis in the formatted message
                    formattedMessage = emojiReplacer(player, formattedMessage, true, false);

                    // Step 4: Apply PlaceholderAPI replacements if needed
                    if (isPAPI()) {
                        formattedMessage = PlaceholderAPI.setRelationalPlaceholders(player, onlinePlayer, formattedMessage);
                    }

                    // Final formatting and escaping
                    formattedMessage = formattedMessage.replaceAll("%", "%%").replaceAll("%%[^\\w\\s%]", "");

                    // Create and send the chat message to the recipient
                    TextComponent msg = new TextComponent(TextComponent.fromLegacyText(format(formattedMessage)));

                    // Set up hover event
                    ComponentBuilder hoverBuilder = new ComponentBuilder("");
                    if (hover) {
                        for (String[] hoverMessage : hoverMessages) {
                            TextComponent hoverTextComponent = new TextComponent(String.join("\n", hoverMessage));
                            hoverBuilder.append(hoverTextComponent);
                        }
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverBuilder.create()));
                    }

                    // Set up click event
                    if (click) {
                        String clickMsg = SupremeChat.getInstance().getConfig().getString("click.string");
                        clickMsg = addOtherPlaceholders(clickMsg, player);
                        msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, clickMsg));
                    }

                    // Send the formatted message to the player
                    onlinePlayer.spigot().sendMessage(ChatMessageType.CHAT, msg);
                }

                // Clear the recipients list after sending the message
                e.getRecipients().clear();
            }
        }
    }


    private String stripColorCodes(String input) {
        return ChatColor.stripColor(input);
    }

    private String getChatFormat(String rank) {
        String chatFormat;

        if (rank != null) {
            chatFormat = getRankFormat(rank);
        } else {
            chatFormat = getGlobalFormat();
        }

        return chatFormat;
    }

    private static boolean isWordBlocked(String message, String blockedWord) {
        String pattern = "\\b" + blockedWord + "\\b";
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = regex.matcher(message);

        return matcher.find();

    }

    public static String stripAllColorCodes(String message) {
        String cleanMessage = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message));

        // Now, let's strip all hex color codes supported by the plugin
        cleanMessage = cleanMessage.replaceAll("(?i)&#[a-f0-9]{6}", "");
        cleanMessage = cleanMessage.replaceAll("(?i)#[a-f0-9]{6}", "");
        cleanMessage = cleanMessage.replaceAll("(?i)\\{#[a-f0-9]{6}}", "");
        cleanMessage = cleanMessage.replaceAll("(?i)<#[a-f0-9]{6}>", "");
        cleanMessage = cleanMessage.replaceAll("(?i)<#&[a-f0-9]{6}>", "");
        return cleanMessage;
    }

    /**
     * Creates a ClickEvent based on configuration settings.
     * Supports backward compatibility with click.string (deprecated).
     *
     * @param player The player who sent the message
     * @return ClickEvent or null if configuration is invalid
     */
    private ClickEvent createClickEvent(Player player) {
        SupremeChat plugin = SupremeChat.getInstance();

        // Get click value with backward compatibility
        String clickValue = plugin.getConfig().getString("click.value");
        if (clickValue == null || clickValue.isEmpty()) {
            // Fallback to old config key for backward compatibility
            clickValue = plugin.getConfig().getString("click.string");
            if (clickValue != null && !clickValue.isEmpty()) {
                plugin.getLogger().info("Config uses deprecated 'click.string'. Please update to 'click.value'.");
            }
        }

        if (clickValue == null || clickValue.isEmpty()) {
            plugin.getLogger().warning("Click is enabled but no value/string is configured!");
            return null;
        }

        // Replace placeholders in click value
        clickValue = addOtherPlaceholders(clickValue, player);

        // Get click type (default: suggest_command for backward compatibility)
        String clickType = plugin.getConfig().getString("click.type", "suggest_command").toLowerCase();

        // Determine ClickEvent.Action based on type
        ClickEvent.Action action;
        switch (clickType) {
            case "run_command":
                action = ClickEvent.Action.RUN_COMMAND;
                break;
            case "open_url":
                action = ClickEvent.Action.OPEN_URL;
                break;
            case "suggest_command":
                action = ClickEvent.Action.SUGGEST_COMMAND;
                break;
            default:
                plugin.getLogger().warning("Invalid click type '" + clickType + "'. Using 'suggest_command' as default. Valid types: run_command, suggest_command, open_url");
                action = ClickEvent.Action.SUGGEST_COMMAND;
                break;
        }

        boolean debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        if (debugMode) {
            plugin.getLogger().info("[DEBUG] Click event created - Type: " + clickType + ", Action: " + action + ", Value: " + clickValue);
        }

        return new ClickEvent(action, clickValue);
    }
}
