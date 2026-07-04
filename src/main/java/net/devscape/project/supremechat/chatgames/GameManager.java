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

    /**
     * Validates that a player name is safe for command execution.
     * Minecraft player names must be 3-16 characters, alphanumeric + underscore only.
     * If a player has an invalid name, it indicates a modified client or proxy injection.
     *
     * @param playerName The player name to validate
     * @return true if the name is safe, false otherwise
     */
    private boolean isPlayerNameSafe(String playerName) {
        // Minecraft username requirements: 3-16 chars, alphanumeric + underscore
        return playerName != null && playerName.matches("^[a-zA-Z0-9_]{3,16}$");
    }

    /**
     * Executes reward commands for a player who won a chat game.
     * SECURITY: Validates player name to prevent command injection attacks.
     *
     * @param player The player who won
     * @param game The game type (math, trivia, word-unscrambler)
     */
    public void executeRewardCommands(Player player, String game) {
        String playerName = player.getName();

        // SECURITY CHECK: Prevent command injection via malicious player names
        if (!isPlayerNameSafe(playerName)) {
            plugin.getLogger().severe("═══════════════════════════════════════════════════");
            plugin.getLogger().severe("⚠ SECURITY ALERT - COMMAND INJECTION ATTEMPT ⚠");
            plugin.getLogger().severe("Blocked command execution for unsafe player name!");
            plugin.getLogger().severe("Player: " + playerName);
            plugin.getLogger().severe("UUID: " + player.getUniqueId());
            plugin.getLogger().severe("Game: " + game);
            plugin.getLogger().severe("This indicates a modified client or proxy attack.");
            plugin.getLogger().severe("═══════════════════════════════════════════════════");

            // Notify online staff
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("supremechat.admin") || staff.isOp()) {
                    staff.sendMessage("§c§l[SECURITY] §fBlocked command injection attempt from: §e" + playerName);
                }
            }
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String cmds : rewardCommands(game)) {
                String command = cmds.replace("%player%", playerName);

                // Log command execution for audit trail
                if (plugin.getConfig().getBoolean("debug-mode", false)) {
                    plugin.getLogger().info("[ChatGames] Executing reward: " + command);
                }

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
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
