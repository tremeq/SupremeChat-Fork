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

/**
 * /ignore <player> - toggles ignoring another player's chat and private messages.
 * Available to everyone (no permission required). Target must be online (so the
 * name can be resolved without a blocking lookup); the ignore persists by UUID.
 */
public class IgnoreCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            msgPlayer(sender, SupremeChat.getInstance().getConfig().getString("private-messages.only-players", "&cThis command can only be used by players."));
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            msgPlayer(player, getMsg("ignore.usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            String notFound = SupremeChat.getInstance().getConfig().getString("private-messages.player-not-found", "&cPlayer &e%player% &cis not online.");
            msgPlayer(player, notFound.replace("%player%", args[0]));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            msgPlayer(player, getMsg("ignore.cannot-ignore-self"));
            return true;
        }

        boolean nowIgnoring = SupremeChat.getInstance().getChatDataManager()
                .toggleIgnore(player.getUniqueId(), target.getUniqueId());

        if (nowIgnoring) {
            msgPlayer(player, getMsg("ignore.now-ignoring").replace("%player%", target.getName()));
        } else {
            msgPlayer(player, getMsg("ignore.no-longer-ignoring").replace("%player%", target.getName()));
        }
        return true;
    }
}
