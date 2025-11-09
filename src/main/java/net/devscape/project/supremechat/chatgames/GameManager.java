package net.devscape.project.supremechat.chatgames;

import net.devscape.project.supremechat.SupremeChat;
import net.devscape.project.supremechat.chatgames.games.MathGame;
import net.devscape.project.supremechat.chatgames.games.TriviaGame;
import net.devscape.project.supremechat.chatgames.games.WordUnscrambler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameManager {

    private final SupremeChat plugin;
    private boolean gameRunning = false;
    private final Random random = new Random();
    private BukkitTask schedulerTask = null;

    public GameManager(SupremeChat plugin) {
        this.plugin = plugin;
    }

    public void startScheduler() {
        // Cancel existing scheduler if running
        if (schedulerTask != null && !schedulerTask.isCancelled()) {
            schedulerTask.cancel();
            plugin.getLogger().info("[ChatGames] Cancelled previous scheduler.");
        }

        int interval = plugin.getConfig().getInt("chatgames.interval-seconds", 180) * 20;

        schedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (gameRunning) return;

                // Read enabled status from config each time (don't cache it)
                boolean enabled = plugin.getConfig().getBoolean("chatgames.enable", true);
                if (!enabled) return;

                if (Bukkit.getOnlinePlayers().size() < plugin.getConfig().getInt("chatgames.min-players")) return;

                List<Runnable> availableGames = new ArrayList<>();

                if (plugin.getConfig().getBoolean("chatgames.word-unscrambler.enabled"))
                    availableGames.add(new WordUnscrambler(plugin, () -> gameRunning = false));

                if (plugin.getConfig().getBoolean("chatgames.trivia.enabled"))
                    availableGames.add(new TriviaGame(plugin, () -> gameRunning = false));

                if (plugin.getConfig().getBoolean("chatgames.math.enabled"))
                    availableGames.add(new MathGame(plugin, () -> gameRunning = false));

                if (availableGames.isEmpty()) return;

                Runnable game = availableGames.get(random.nextInt(availableGames.size()));
                gameRunning = true;
                Bukkit.getScheduler().runTask(plugin, game);
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    public List<String> rewardCommands(String game) {
        return SupremeChat.getInstance().getConfig().getStringList("chatgames." + game + ".reward-commands");
    }

    public void executeRewardCommands(Player player, String game) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String cmds : rewardCommands(game)) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmds.replace("%player%", player.getName()));
            }
        });
    }

    /**
     * Stops the scheduler (cancels the task)
     */
    public void stopScheduler() {
        if (schedulerTask != null && !schedulerTask.isCancelled()) {
            schedulerTask.cancel();
            plugin.getLogger().info("[ChatGames] Scheduler stopped.");
        }
    }

    /**
     * Reloads the chat games system
     * Cancels existing scheduler and starts a new one with updated config values
     */
    public void reload() {
        plugin.getLogger().info("[ChatGames] Reloading chat games system...");
        stopScheduler();
        gameRunning = false; // Reset game state
        startScheduler();
        plugin.getLogger().info("[ChatGames] Chat games reloaded with new config values.");
    }
}
