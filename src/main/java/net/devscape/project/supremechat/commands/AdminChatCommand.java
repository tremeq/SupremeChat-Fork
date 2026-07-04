package net.devscape.project.supremechat.commands;

import net.devscape.project.supremechat.SupremeChat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.devscape.project.supremechat.utils.Message.format;
import static net.devscape.project.supremechat.utils.Message.getMsg;
import static net.devscape.project.supremechat.utils.Message.msgPlayer;

/**
 * /ac <message> - admin/staff chat. Broadcasts to everyone who holds the
 * configurable permission (admin-chat.permission) plus the console.
 * Format and permission are configurable in config.yml.
 */
public class AdminChatCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        SupremeChat plugin = SupremeChat.getInstance();

        if (!plugin.getConfig().getBoolean("admin-chat.enable", true)) {
            return true;
        }

        String permission = plugin.getConfig().getString("admin-chat.permission", "supremechat.adminchat");

        // Console may always use admin chat; players need the configurable permission.
        if (sender instanceof Player && !sender.hasPermission(permission)) {
            msgPlayer(sender, getMsg("no-permission"));
            return true;
        }

        if (args.length < 1) {
            msgPlayer(sender, getMsg("adminchat.usage"));
            return true;
        }

        String message = String.join(" ", args);
        String senderName = (sender instanceof Player) ? sender.getName()
                : plugin.getConfig().getString("admin-chat.console-name", "Console");

        String formatted = plugin.getConfig().getString("admin-chat.format", "&c&l[AdminChat] &f%name% &8➟ &c%message%")
                .replace("%name%", senderName)
                .replace("%message%", message);

        // Deliver to every online holder of the permission.
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(permission)) {
                msgPlayer(online, formatted);
            }
        }

        // Always echo to console so staff chat is logged/visible server-side.
        Bukkit.getConsoleSender().sendMessage(format(formatted));
        return true;
    }
}
