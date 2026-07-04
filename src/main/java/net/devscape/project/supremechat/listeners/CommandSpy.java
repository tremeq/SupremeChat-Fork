package net.devscape.project.supremechat.listeners;

import net.devscape.project.supremechat.SupremeChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import static net.devscape.project.supremechat.utils.Message.createLog;
import static net.devscape.project.supremechat.utils.Message.format;
import static net.devscape.project.supremechat.utils.Message.msgPlayer;

public class CommandSpy implements Listener {

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (e.isCancelled()) return;

        Player player = e.getPlayer();

        // Log every command a player runs. createLog respects the 'logging.commands'
        // switch, so this does nothing when command logging is off.
        createLog(player, e.getMessage(), true);

        if (SupremeChat.getInstance().getConfig().getBoolean("enable-command-spy")) {
            boolean isWhitelisted = false;
            for (String str : SupremeChat.getInstance().getConfig().getStringList("whitelist-spy-commands")) {
                if (e.getMessage().equalsIgnoreCase(str)) {
                    isWhitelisted = true;
                    break;
                }
            }

            if (!isWhitelisted) {
                String alert = SupremeChat.getInstance().getConfig().getString("cs-spy");
                if (alert == null) {
                    alert = "&7[CommandSpy] &e%name% &7used: &f%command%"; // Default format
                }
                alert = alert.replaceAll("%command%", e.getMessage());
                alert = alert.replaceAll("%name%", player.getName());

                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.hasPermission("supremechat.commandspy.alert")) {
                        // Don't send alert to the player who executed the command
                        if (!staff.getName().equalsIgnoreCase(player.getName())) {
                            msgPlayer(staff, format(alert));
                        }
                        // Removed break - all staff members with permission should receive alerts
                    }
                }
            }
        }
    }
}