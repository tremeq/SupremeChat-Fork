package net.devscape.project.supremechat.utils;

import net.devscape.project.supremechat.SupremeChat;
import net.devscape.project.supremechat.hooks.VaultHook;
import net.devscape.project.supremechat.object.Channel;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import static net.devscape.project.supremechat.utils.Message.*;

public class FormatUtil {


    // ==================================================
    // GET RANK
    // ==================================================
    public static String getRank(Player player) {
        // Guard with a plain boolean FIRST. This must not touch any Vault class,
        // otherwise the JVM would try to load net.milkbowl.vault.chat.Chat and throw
        // NoClassDefFoundError on servers that don't have Vault installed.
        if (!SupremeChat.isVaultEnabled()) {
            return "default";
        }

        // Safe to touch Vault now (isolated inside VaultHook).
        String group = VaultHook.getPrimaryGroup(player);
        if (group == null) {
            return "default";
        }

        return group;
    }

    // ==================================================
    // SCAPECHAT HELP MESSAGE
    // ==================================================

    public static void sendHelp(Player player) {
        // Allow the help menu to be turned off entirely (e.g. reworked plugins).
        if (!SupremeChat.getInstance().getConfig().getBoolean("messages.help-enabled", true)) {
            return;
        }

        String version = SupremeChat.getInstance().getDescription().getVersion();
        for (String line : getMsgList("help")) {
            msgPlayer(player, line.replace("%version%", version));
        }
    }

    public static String emojiReplacer(Player player, String message, boolean isInChannel, boolean isNormalChat) {
        FileConfiguration config = SupremeChat.getInstance().getConfig();

        // Check if the "emojis" section exists in the config
        if (config.isConfigurationSection("emojis")) {
            for (String key : config.getConfigurationSection("emojis").getKeys(false)) {

                if (player.hasPermission("supremechat.emoji." + key) || player.hasPermission("supremechat.emoji.*")) {
                    String emoticon = config.getString("emojis." + key + ".emoticon");
                    String emoji = config.getString("emojis." + key + ".emoji");

                    if (isNormalChat) {
                        emoji = emoji + SupremeChat.getInstance().getConfig().getString("global-chat-color");
                    }

                    if (isInChannel) {
                        if (SupremeChat.getInstance().getChannelManager().isInChannel(player)) {
                            Channel c = SupremeChat.getInstance().getChannelManager().getChannel(player);

                            emoji = emoji + c.getChatColor();
                        }
                    }

                    // Ensure both emoticon and emoji are not null
                    if (emoticon != null && emoji != null) {
                        // Replace all occurrences of the emoticon in the message with the emoji
                        if (message.contains(emoticon)) {
                            message = message.replace(emoticon, emoji);
                        }
                    }
                }
            }
        }

        // Apply PlaceholderAPI replacements
        message = replacePlaceholders(player, message);

        return message;
    }

}