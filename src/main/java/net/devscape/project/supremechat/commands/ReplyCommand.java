package net.devscape.project.supremechat.commands;

import net.devscape.project.supremechat.SupremeChat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.devscape.project.supremechat.utils.Message.getMsg;
import static net.devscape.project.supremechat.utils.Message.msgPlayer;
import static net.devscape.project.supremechat.utils.VanishCheckUtil.isVanished;

public class ReplyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            msgPlayer(sender, SupremeChat.getInstance().getConfig().getString("private-messages.only-players", "&cThis command can only be used by players."));
            return true;
        }

        Player senderPlayer = (Player) sender;

        // Check if private messages are enabled
        if (!SupremeChat.getInstance().getConfig().getBoolean("private-messages.enable", true)) {
            msgPlayer(senderPlayer, SupremeChat.getInstance().getConfig().getString("private-messages.disabled-message", "&cPrivate messages are currently disabled."));
            return true;
        }

        // Usage check
        if (args.length < 1) {
            msgPlayer(senderPlayer, SupremeChat.getInstance().getConfig().getString("private-messages.usage-reply", "&cUsage: /%label% <message>").replace("%label%", label));
            return true;
        }

        // Get last messenger
        Player lastMessenger = SupremeChat.getInstance().getLastMessenger(senderPlayer);

        if (lastMessenger == null) {
            msgPlayer(senderPlayer, SupremeChat.getInstance().getConfig().getString("private-messages.no-reply-target", "&cYou have no one to reply to."));
            return true;
        }

        // Check if target is still online
        if (!lastMessenger.isOnline()) {
            msgPlayer(senderPlayer, SupremeChat.getInstance().getConfig().getString("private-messages.reply-target-offline", "&cThat player is no longer online."));
            return true;
        }

        // Check if target is vanished
        if (SupremeChat.getInstance().getConfig().getBoolean("vanish-support", false)) {
            if (isVanished(lastMessenger) && !senderPlayer.hasPermission("supremechat.see.vanished")) {
                msgPlayer(senderPlayer, SupremeChat.getInstance().getConfig().getString("private-messages.reply-target-offline", "&cThat player is no longer online."));
                return true;
            }
        }

        // Respect the target's /msgtoggle (private messages disabled)
        if (SupremeChat.getInstance().getChatDataManager().hasMessagesDisabled(lastMessenger.getUniqueId())) {
            msgPlayer(senderPlayer, getMsg("msgtoggle.target-disabled"));
            return true;
        }

        // Respect the target's ignore list (target is ignoring the sender)
        if (SupremeChat.getInstance().getChatDataManager().isIgnoring(lastMessenger.getUniqueId(), senderPlayer.getUniqueId())) {
            msgPlayer(senderPlayer, getMsg("ignore.target-ignores-you"));
            return true;
        }

        // Build message
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) {
                messageBuilder.append(" ");
            }
        }
        String message = messageBuilder.toString();

        // Execute /msg command
        Bukkit.dispatchCommand(senderPlayer, "msg " + lastMessenger.getName() + " " + message);

        return true;
    }
}
