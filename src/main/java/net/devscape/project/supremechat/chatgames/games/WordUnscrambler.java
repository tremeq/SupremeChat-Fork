package net.devscape.project.supremechat.chatgames.games;

import net.devscape.project.supremechat.SupremeChat;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static net.devscape.project.supremechat.utils.Message.msgPlayer;

public class WordUnscrambler implements Runnable {

    private final SupremeChat plugin;
    private final Runnable onEnd;

    public WordUnscrambler(SupremeChat plugin, Runnable onEnd) {
        this.plugin = plugin;
        this.onEnd = onEnd;
    }

    @Override
    public void run() {
        List<String> words = plugin.getConfig().getStringList("chatgames.word-unscrambler.words");
        if (words.isEmpty()) return;

        String word = words.get(new Random().nextInt(words.size()));
        String scrambled = shuffle(word);

        String gameName = plugin.getConfig().getString("chatgames.word-unscrambler.name", "Word Unscrambler");
        List<String> announce = plugin.getConfig().getStringList("chatgames.strings.game-announce");
        for (String line : announce) {
            String parsed = line.replace("%game_name%", gameName)
                            .replace("%game_question%", scrambled);
            Bukkit.getOnlinePlayers().forEach(p -> msgPlayer(p, parsed));
        }

        AtomicBoolean solved = new AtomicBoolean(false);

        Listener listener = new Listener() {
            @EventHandler
            public void onChat(AsyncPlayerChatEvent event) {
                if (solved.get()) return;

                if (event.getMessage().equalsIgnoreCase(word)) {
                    solved.set(true);
                    String winMsg = plugin.getConfig().getString("chatgames.strings.game-win", "&c&lC&6&lH&e&lA&a&lT&b&l &9&lG&d&lA&5&lM&c&lE&6&lS &8&l➟ &a%player% &7won the game!");
                    if (winMsg != null) {
                        winMsg = winMsg.replace("%player%", event.getPlayer().getName());
                        String finalWinMsg = winMsg;
                        Bukkit.getOnlinePlayers().forEach(p -> msgPlayer(p, finalWinMsg));
                    }
                    SupremeChat.getInstance().getGameManager().executeRewardCommands(event.getPlayer(), "word-unscrambler");
                    HandlerList.unregisterAll(this);
                    onEnd.run();
                }
            }
        };

        Bukkit.getPluginManager().registerEvents(listener, plugin);

        long expiryTicks = plugin.getConfig().getInt("chatgames.expiry-time", 30) * 20L;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!solved.get()) {
                HandlerList.unregisterAll(listener);
                String expiredMsg = plugin.getConfig().getString("chatgames.strings.game-expired", "&c&lCHAT GAMES &8&l➟ &7Time is up! The word was: " + word);
                Bukkit.getOnlinePlayers().forEach(p -> msgPlayer(p, expiredMsg.replace("%answer%", word)));
                onEnd.run();
            }
        }, expiryTicks);
    }

    private String shuffle(String input) {
        List<Character> chars = input.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        Collections.shuffle(chars);
        StringBuilder sb = new StringBuilder();
        chars.forEach(sb::append);
        return sb.toString();
    }
}
