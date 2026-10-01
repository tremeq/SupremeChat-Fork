package net.devscape.project.supremechat.commands;

import net.devscape.project.supremechat.SupremeChat;
import net.devscape.project.supremechat.hooks.DiscordSRVHook;
import net.devscape.project.supremechat.utils.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.devscape.project.supremechat.utils.Message.getMsg;
import static net.devscape.project.supremechat.utils.Message.msgPlayer;

public class SCCommand implements CommandExecutor, TabCompleter {

    // Sub-commands offered by /supremechat, in the order they should be suggested.
    private static final List<String> SUBCOMMANDS =
            Arrays.asList("reload", "clearchat", "mutechat", "discordsrv");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        } else {

            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("supremechat")) {
                if (player.hasPermission("supremechat.admin") || player.isOp()) {
                    if (args.length == 0) {
                        FormatUtil.sendHelp(player);
                    } else if (args.length == 1) {
                        if (args[0].equalsIgnoreCase("reload")) {
                            SupremeChat.getInstance().reload();
                            msgPlayer(player, getMsg("reload"));
                        } else if (args[0].equalsIgnoreCase("clearchat") || args[0].equalsIgnoreCase("cc")) {
                            clearChat(player);
                        } else if (args[0].equalsIgnoreCase("discordsrv") || args[0].equalsIgnoreCase("discord")) {
                            msgPlayer(player, "&e&m-------------------------------------------");
                            msgPlayer(player, "&6&lDiscordSRV Integration Debug Report");
                            msgPlayer(player, "&e&m-------------------------------------------");
                            msgPlayer(player, "&7Generating debug report...");
                            msgPlayer(player, "&7Check console for detailed information.");
                            msgPlayer(player, "&e&m-------------------------------------------");

                            // Print to console
                            try {
                                if (Bukkit.getPluginManager().getPlugin("DiscordSRV") != null) {
                                    DiscordSRVHook.printDebugReport();
                                    msgPlayer(player, "&a✓ &7Debug report printed to console!");
                                    msgPlayer(player, "&7Please send the console log to support.");
                                } else {
                                    msgPlayer(player, "&c✗ &7DiscordSRV is not installed!");
                                }
                            } catch (NoClassDefFoundError e) {
                                msgPlayer(player, "&c✗ &7DiscordSRV integration not available!");
                            }
                        } else if (args[0].equalsIgnoreCase("mutechat")) {
                            if (SupremeChat.getInstance().getConfig().getBoolean("mute-chat")) {
                                SupremeChat.getInstance().getConfig().set("mute-chat", false);
                                SupremeChat.getInstance().saveConfig();

                                SupremeChat.getInstance().reload();
                                for (Player all : Bukkit.getOnlinePlayers()) {
                                    msgPlayer(all, getMsg("mutechat.disabled"));
                                }
                            } else {
                                SupremeChat.getInstance().getConfig().set("mute-chat", true);
                                SupremeChat.getInstance().saveConfig();

                                SupremeChat.getInstance().reload();
                                for (Player all : Bukkit.getOnlinePlayers()) {
                                    msgPlayer(all, getMsg("mutechat.enabled"));
                                }
                            }
                        }
                    }
                } else {
                    msgPlayer(player, getMsg("no-permission"));
                }
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                                @NotNull String label, @NotNull String[] args) {
        // Only staff can see the sub-commands - mirrors the permission gate in onCommand().
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        Player player = (Player) sender;
        if (!player.hasPermission("supremechat.admin") && !player.isOp()) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(prefix)) {
                    suggestions.add(sub);
                }
            }
            return suggestions;
        }

        return Collections.emptyList();
    }

    /**
     * Clears the chat for every online player by sending a configurable number of
     * blank lines. Players with the configured bypass permission keep their chat.
     * A configurable broadcast is then shown announcing who cleared the chat.
     *
     * @param clearer the player who executed the clear command
     */
    private void clearChat(Player clearer) {
        int lines = SupremeChat.getInstance().getConfig().getInt("messages.clearchat.lines", 100);
        String bypassPermission = SupremeChat.getInstance().getConfig()
                .getString("messages.clearchat.bypass-permission", "supremechat.bypass.clearchat");

        for (Player online : Bukkit.getOnlinePlayers()) {
            // Players with the bypass permission keep their chat history untouched.
            if (bypassPermission != null && !bypassPermission.isEmpty() && online.hasPermission(bypassPermission)) {
                continue;
            }
            for (int i = 0; i < lines; i++) {
                online.sendMessage(" ");
            }
        }

        if (SupremeChat.getInstance().getConfig().getBoolean("messages.clearchat.broadcast-enabled", true)) {
            String broadcast = getMsg("clearchat.broadcast").replace("%player%", clearer.getName());
            for (Player online : Bukkit.getOnlinePlayers()) {
                msgPlayer(online, broadcast);
            }
        }
    }
}