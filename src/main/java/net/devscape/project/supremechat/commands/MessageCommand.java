package net.devscape.project.supremechat.commands;

import me.clip.placeholderapi.PlaceholderAPI;
import net.devscape.project.supremechat.SupremeChat;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.devscape.project.supremechat.utils.Message.*;
import static net.devscape.project.supremechat.utils.VanishCheckUtil.isVanished;

public class MessageCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player senderPlayer = (Player) sender;

        // Check if private messages are enabled
        if (!SupremeChat.getInstance().getConfig().getBoolean("private-messages.enable", true)) {
            msgPlayer(senderPlayer, SupremeChat.getInstance().getConfig().getString("private-messages.disabled-message", "&cPrivate messages are currently disabled."));
            return true;
        }

        // Usage check
        if (args.length < 2) {
            msgPlayer(senderPlayer, "&cUsage: /" + label + " <player> <message>");
            return true;
        }

        // Get target player
        Player target = Bukkit.getPlayer(args[0]);

        // Check if target exists
        if (target == null || !target.isOnline()) {
            String notFoundMsg = SupremeChat.getInstance().getConfig().getString("private-messages.player-not-found", "&cPlayer not found.");
            notFoundMsg = notFoundMsg.replace("%player%", args[0]);
            msgPlayer(senderPlayer, notFoundMsg);
            return true;
        }

        // Check if target is vanished (if vanish support enabled)
        if (SupremeChat.getInstance().getConfig().getBoolean("vanish-support", false)) {
            if (isVanished(target) && !senderPlayer.hasPermission("supremechat.see.vanished")) {
                String notFoundMsg = SupremeChat.getInstance().getConfig().getString("private-messages.player-not-found", "&cPlayer not found.");
                notFoundMsg = notFoundMsg.replace("%player%", args[0]);
                msgPlayer(senderPlayer, notFoundMsg);
                return true;
            }
        }

        // Check if trying to message self
        if (target.equals(senderPlayer)) {
            msgPlayer(senderPlayer, SupremeChat.getInstance().getConfig().getString("private-messages.cannot-message-self", "&cYou cannot send a message to yourself."));
            return true;
        }

        // Build message from args[1] onwards
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) {
                messageBuilder.append(" ");
            }
        }
        String message = messageBuilder.toString();

        // Send private message
        sendPrivateMessage(senderPlayer, target, message);

        // Track last conversation for /reply
        SupremeChat.getInstance().setLastMessenger(senderPlayer, target);
        SupremeChat.getInstance().setLastMessenger(target, senderPlayer);

        return true;
    }

    /**
     * Sends a formatted private message between two players
     */
    private void sendPrivateMessage(Player sender, Player receiver, String message) {
        SupremeChat plugin = SupremeChat.getInstance();
        boolean debugMode = plugin.getConfig().getBoolean("debug-mode", false);

        // Get formats
        String senderFormat = plugin.getConfig().getString("private-messages.format.sender", "&d[You -> %receiver_name%] &f%message%");
        String receiverFormat = plugin.getConfig().getString("private-messages.format.receiver", "&d[%sender_name% -> You] &f%message%");

        // Replace placeholders for sender message
        String senderMessage = replacePMPlaceholders(senderFormat, sender, receiver, message);

        // Replace placeholders for receiver message
        String receiverMessage = replacePMPlaceholders(receiverFormat, sender, receiver, message);

        if (debugMode) {
            plugin.getLogger().info("[DEBUG] PM from " + sender.getName() + " to " + receiver.getName());
            plugin.getLogger().info("[DEBUG] Sender sees: " + senderMessage);
            plugin.getLogger().info("[DEBUG] Receiver sees: " + receiverMessage);
        }

        // Build sender component
        TextComponent senderComponent = new TextComponent(TextComponent.fromLegacyText(format(senderMessage)));
        addPMHoverAndClick(senderComponent, sender, receiver, true);

        // Build receiver component
        TextComponent receiverComponent = new TextComponent(TextComponent.fromLegacyText(format(receiverMessage)));
        addPMHoverAndClick(receiverComponent, sender, receiver, false);

        // Send messages
        sender.spigot().sendMessage(ChatMessageType.CHAT, senderComponent);
        receiver.spigot().sendMessage(ChatMessageType.CHAT, receiverComponent);

        // Social spy
        sendToSocialSpy(sender, receiver, message);
    }

    /**
     * Replaces placeholders in private message format
     */
    private String replacePMPlaceholders(String format, Player sender, Player receiver, String message) {
        String result = format;

        // Basic placeholders
        result = result.replace("%sender_name%", sender.getName());
        result = result.replace("%sender_displayname%", sender.getDisplayName());
        result = result.replace("%receiver_name%", receiver.getName());
        result = result.replace("%receiver_displayname%", receiver.getDisplayName());
        result = result.replace("%message%", message);

        // PlaceholderAPI support
        if (isPAPI()) {
            result = PlaceholderAPI.setPlaceholders(sender, result);
            result = PlaceholderAPI.setRelationalPlaceholders(sender, receiver, result);
        }

        return result;
    }

    /**
     * Adds hover and click events to private message
     */
    private void addPMHoverAndClick(TextComponent component, Player sender, Player receiver, boolean isSender) {
        SupremeChat plugin = SupremeChat.getInstance();

        // Hover event
        if (plugin.getConfig().getBoolean("private-messages.hover.enable", true)) {
            ComponentBuilder hoverBuilder = new ComponentBuilder();

            // Use sender or receiver info depending on perspective
            Player targetForHover = isSender ? receiver : sender;

            for (String hoverLine : plugin.getConfig().getStringList("private-messages.hover.string")) {
                String processedLine = hoverLine
                    .replace("%player_name%", targetForHover.getName())
                    .replace("%player_displayname%", targetForHover.getDisplayName())
                    .replace("%world%", targetForHover.getWorld().getName());

                if (isPAPI()) {
                    processedLine = PlaceholderAPI.setPlaceholders(targetForHover, processedLine);
                }

                hoverBuilder.append(new TextComponent(format(processedLine))).append("\n");
            }
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverBuilder.create()));
        }

        // Click event - using our new extended system
        if (plugin.getConfig().getBoolean("private-messages.click.enable", true)) {
            Player targetForClick = isSender ? receiver : sender;
            ClickEvent clickEvent = createPMClickEvent(targetForClick);
            if (clickEvent != null) {
                component.setClickEvent(clickEvent);
            }
        }
    }

    /**
     * Creates a ClickEvent for private messages using the extended click system
     */
    private ClickEvent createPMClickEvent(Player target) {
        SupremeChat plugin = SupremeChat.getInstance();

        // Get click value
        String clickValue = plugin.getConfig().getString("private-messages.click.value");
        if (clickValue == null || clickValue.isEmpty()) {
            clickValue = "/msg %player_name% ";
        }

        // Replace placeholders
        clickValue = clickValue
            .replace("%player_name%", target.getName())
            .replace("%player_displayname%", target.getDisplayName());

        if (isPAPI()) {
            clickValue = PlaceholderAPI.setPlaceholders(target, clickValue);
        }

        // Get click type (default: suggest_command)
        String clickType = plugin.getConfig().getString("private-messages.click.type", "suggest_command").toLowerCase();

        // Determine action
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
                plugin.getLogger().warning("Invalid PM click type '" + clickType + "'. Using 'suggest_command' as default.");
                action = ClickEvent.Action.SUGGEST_COMMAND;
                break;
        }

        boolean debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        if (debugMode) {
            plugin.getLogger().info("[DEBUG] PM Click event - Type: " + clickType + ", Action: " + action + ", Value: " + clickValue);
        }

        return new ClickEvent(action, clickValue);
    }

    /**
     * Sends message to staff with social spy enabled
     */
    private void sendToSocialSpy(Player sender, Player receiver, String message) {
        SupremeChat plugin = SupremeChat.getInstance();

        if (!plugin.getConfig().getBoolean("private-messages.social-spy.enable", false)) {
            return;
        }

        String spyFormat = plugin.getConfig().getString("private-messages.social-spy.format",
            "&7[SPY] &d%sender_name% &7-> &d%receiver_name%&7: &f%message%");

        String spyMessage = spyFormat
            .replace("%sender_name%", sender.getName())
            .replace("%receiver_name%", receiver.getName())
            .replace("%message%", message);

        String permission = plugin.getConfig().getString("private-messages.social-spy.permission", "supremechat.socialspy");

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(permission) && !staff.equals(sender) && !staff.equals(receiver)) {
                msgPlayer(staff, spyMessage);
            }
        }
    }
}
