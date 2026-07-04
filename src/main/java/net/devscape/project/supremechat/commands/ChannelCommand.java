package net.devscape.project.supremechat.commands;

import net.devscape.project.supremechat.SupremeChat;
import net.devscape.project.supremechat.object.Channel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.devscape.project.supremechat.utils.Message.getMsg;
import static net.devscape.project.supremechat.utils.Message.getMsgList;
import static net.devscape.project.supremechat.utils.Message.msgPlayer;

public class ChannelCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        } else {

            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("channel")) {
                // Master switch for the whole channel system.
                if (!SupremeChat.getInstance().getConfig().getBoolean("channels-enabled", true)) {
                    msgPlayer(player, getMsg("channels.disabled"));
                    return true;
                }

                if (player.hasPermission("supremechat.channel") || player.isOp()) {
                    if (args.length == 0) {
                        whatChannel(player);
                    } else if (args.length == 1) {
                        if (args[0].equalsIgnoreCase("leave")) {
                            if (!SupremeChat.getInstance().getChannelManager().isInChannel(player)) {
                                msgPlayer(player, getMsg("channels.not-in-channel"));
                                return true;
                            }

                            String channel = SupremeChat.getInstance().getChannelManager().getChannel(player).getName();

                            SupremeChat.getInstance().getChannelManager().getPlayerChannel().remove(player.getUniqueId());
                            msgPlayer(player, getMsg("channels.left").replace("%channel%", channel));
                        } else if (args[0].equalsIgnoreCase("help")) {
                            whatChannel(player);
                        } else if (args[0].equalsIgnoreCase("list")) {
                            List<String> channels = new ArrayList<>();

                            for (Channel c : SupremeChat.getInstance().getChannelManager().channels) {
                                if (player.hasPermission(c.getPermission()) || c.getPermission().equalsIgnoreCase("None")) {
                                    channels.add(c.getName());
                                }
                            }

                            String formatted = channels.toString().replace("[", "").replace("]", "");

                            if (channels.isEmpty()) {
                                formatted = getMsg("channels.list-empty");
                            }

                            msgPlayer(player, getMsg("channels.list").replace("%channels%", formatted));
                        }
                    } else if (args.length == 2) {
                        if (args[0].equalsIgnoreCase("join")) {
                            String channel = args[1];

                            if (!SupremeChat.getInstance().getChannelManager().isChannel(channel)) {
                                msgPlayer(player, getMsg("channels.not-exist"));
                                return true;
                            }

                            Channel c = SupremeChat.getInstance().getChannelManager().getChannel(channel);

                            if (!c.getPermission().equalsIgnoreCase("None") && !player.hasPermission(c.getPermission())) {
                                msgPlayer(player, getMsg("channels.no-permission-join"));
                                return true;
                            }

                            if (SupremeChat.getInstance().getChannelManager().getChannel(player) != null && SupremeChat.getInstance().getChannelManager().getChannel(player).getName().equalsIgnoreCase(channel)) {
                                msgPlayer(player, getMsg("channels.already-in"));
                                return true;
                            }

                            SupremeChat.getInstance().getChannelManager().addToChannel(player, channel);
                            msgPlayer(player, getMsg("channels.joined").replace("%channel%", channel));
                        }
                    }
                } else {
                    msgPlayer(player, getMsg("no-permission"));
                }
            }
        }
        return false;
    }

    public void whatChannel(Player player) {
        String channelName;
        if (SupremeChat.getInstance().getChannelManager().isInChannel(player)) {
            channelName = SupremeChat.getInstance().getChannelManager().getChannel(player).getName();
        } else {
            channelName = getMsg("channels.none");
        }

        for (String line : getMsgList("channels.info")) {
            msgPlayer(player, line.replace("%channel%", channelName));
        }
    }
}