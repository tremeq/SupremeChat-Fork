package net.devscape.project.supremechat.commands;

import net.devscape.project.supremechat.SupremeChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.devscape.project.supremechat.utils.Message.getMsg;
import static net.devscape.project.supremechat.utils.Message.msgPlayer;

/**
 * /msgtoggle - lets any player enable/disable receiving private messages.
 * Available to everyone (no permission required).
 */
public class MsgToggleCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            msgPlayer(sender, SupremeChat.getInstance().getConfig().getString("private-messages.only-players", "&cThis command can only be used by players."));
            return true;
        }

        Player player = (Player) sender;
        boolean nowDisabled = SupremeChat.getInstance().getChatDataManager().toggleMessages(player.getUniqueId());

        if (nowDisabled) {
            msgPlayer(player, getMsg("msgtoggle.now-blocking"));
        } else {
            msgPlayer(player, getMsg("msgtoggle.now-receiving"));
        }
        return true;
    }
}
