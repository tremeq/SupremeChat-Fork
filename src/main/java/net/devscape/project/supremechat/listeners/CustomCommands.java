package net.devscape.project.supremechat.listeners;

import net.devscape.project.supremechat.SupremeChat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import static net.devscape.project.supremechat.utils.Message.getMsg;
import static net.devscape.project.supremechat.utils.Message.msgPlayer;

public class CustomCommands implements Listener {

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        String cmd = e.getMessage();

        if (e.isCancelled()) return;

        boolean anti_bot = SupremeChat.getInstance().getConfig().getBoolean("anti-bot.commands");

        if (anti_bot) {
            if (SupremeChat.getInstance().getPrevention().contains(player)) {
                e.setCancelled(true);
                String detect_alert = SupremeChat.getInstance().getConfig().getString("anti-bot.message");
                detect_alert = detect_alert.replaceAll("%name%", player.getName());

                msgPlayer(player, detect_alert);
                return;
            }
        }

        if (SupremeChat.getInstance().getConfig().getConfigurationSection("custom-commands") != null) {
            // PlayerCommandPreprocessEvent#getMessage() returns the raw input WITH the leading
            // slash and any arguments, e.g. "/discord foo". Reduce it to the bare command label
            // (no slash, first token only) so it can be matched against the config keys.
            String label = cmd.startsWith("/") ? cmd.substring(1) : cmd;
            int space = label.indexOf(' ');
            if (space != -1) {
                label = label.substring(0, space);
            }

            for (String commands : SupremeChat.getInstance().getConfig().getConfigurationSection("custom-commands").getKeys(false)) {
                if (commands == null || !label.equalsIgnoreCase(commands)) {
                    continue;
                }

                String str = SupremeChat.getInstance().getConfig().getString("custom-commands." + commands + ".string");
                if (str == null) {
                    continue;
                }

                // Optional per-command permission. When absent/empty the command is public.
                String permission = SupremeChat.getInstance().getConfig().getString("custom-commands." + commands + ".permission");
                if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
                    e.setCancelled(true);
                    msgPlayer(player, getMsg("no-permission"));
                    return;
                }

                e.setCancelled(true);
                msgPlayer(player, str);
                return;
            }
        }
    }
}