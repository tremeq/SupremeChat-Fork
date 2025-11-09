package net.devscape.project.supremechat.chathead;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced HeadCache with support for both UUID and username-based caching.
 * This is crucial for offline mode servers where UUID is unreliable.
 */
public class HeadCache {

    private final JavaPlugin plugin;
    private final long cacheExpiration; // Now configurable!

    private final Map<String, CachedHead> cache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> pendingRequests = new ConcurrentHashMap<>();
    private BukkitTask cacheCleanupTask;

    public HeadCache(JavaPlugin plugin) {
        this.plugin = plugin;

        // Read cache time from config (in minutes), default to 5 minutes
        int cacheMinutes = plugin.getConfig().getInt("chathead.cache-time-minutes", 5);
        this.cacheExpiration = cacheMinutes * 60 * 1000L; // Convert to milliseconds

        plugin.getLogger().info("ChatHead cache expiration set to " + cacheMinutes + " minutes");
        startCacheCleanupTask();
    }

    /**
     * Retrieves the cached head representation for the player identified by UUID.
     */
    public BaseComponent[] getCachedHead(UUID uuid, boolean overlay, SkinSource skinSource) {
        return getCachedHead(Bukkit.getOfflinePlayer(uuid), overlay, skinSource);
    }

    /**
     * Retrieves the cached head representation for the specified OfflinePlayer.
     */
    public BaseComponent[] getCachedHead(OfflinePlayer player, boolean overlay, SkinSource skinSource) {
        UUID uuid = player.getUniqueId();
        String cacheKey = getCacheKey(uuid, overlay);
        CachedHead cachedHead = cache.get(cacheKey);

        if (cachedHead != null && !isExpired(cachedHead)) {
            return cachedHead.getHead();
        }

        BaseComponent[] lastHead = cachedHead != null ? cachedHead.getHead() : new BaseComponent[]{};

        if (pendingRequests.putIfAbsent(cacheKey, true) == null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                BaseComponent[] head = skinSource.getHead(player, overlay);
                if (head != null && head.length > 0 && plugin.isEnabled()) {
                    cache.put(cacheKey, new CachedHead(head, overlay, System.currentTimeMillis()));
                }
                pendingRequests.remove(cacheKey);
            });
        }

        return lastHead;
    }

    /**
     * NEW: Retrieves the cached head representation using player name.
     * This is the KEY method for offline mode support!
     *
     * @param playerName The player's name (works in offline mode)
     * @param overlay Whether to include the second skin layer
     * @param skinSource The skin source to use (should support username retrieval)
     * @return BaseComponent array representing the player's head
     */
    public BaseComponent[] getCachedHeadByName(String playerName, boolean overlay, SkinSource skinSource) {
        if (!skinSource.hasUsernameSupport()) {
            throw new UnsupportedOperationException(
                "SkinSource " + skinSource.getSkinSource() + " does not support username-based retrieval. " +
                "Use MinotarSource for offline mode."
            );
        }

        String cacheKey = getCacheKeyByName(playerName, overlay);
        CachedHead cachedHead = cache.get(cacheKey);

        if (cachedHead != null && !isExpired(cachedHead)) {
            return cachedHead.getHead();
        }

        BaseComponent[] lastHead = cachedHead != null ? cachedHead.getHead() : new BaseComponent[]{};

        if (pendingRequests.putIfAbsent(cacheKey, true) == null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                BaseComponent[] head = skinSource.getHeadByName(playerName, overlay);
                if (head != null && head.length > 0 && plugin.isEnabled()) {
                    cache.put(cacheKey, new CachedHead(head, overlay, System.currentTimeMillis()));
                }
                pendingRequests.remove(cacheKey);
            });
        }

        return lastHead;
    }

    private boolean isExpired(CachedHead cachedHead) {
        return System.currentTimeMillis() - cachedHead.getTimestamp() > cacheExpiration;
    }

    private void startCacheCleanupTask() {
        if (cacheCleanupTask != null) {
            cacheCleanupTask.cancel();
        }

        // Run cleanup task at intervals equal to cache expiration time
        long cleanupInterval = cacheExpiration / 20; // Convert ms to ticks
        cacheCleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            cache.entrySet().removeIf(entry -> isExpired(entry.getValue()));
        }, cleanupInterval, cleanupInterval);
    }

    private String getCacheKey(UUID uuid, boolean overlay) {
        return uuid.toString() + ":" + overlay;
    }

    /**
     * NEW: Generate cache key based on player name instead of UUID.
     * This allows caching to work properly in offline mode.
     */
    private String getCacheKeyByName(String playerName, boolean overlay) {
        return "name:" + playerName.toLowerCase() + ":" + overlay;
    }

    /**
     * Shutdown the cache and cancel cleanup tasks.
     */
    public void shutdown() {
        if (cacheCleanupTask != null) {
            cacheCleanupTask.cancel();
        }
        cache.clear();
        pendingRequests.clear();
    }

    private static class CachedHead {
        private final BaseComponent[] head;
        private final boolean overlay;
        private final long timestamp;

        CachedHead(BaseComponent[] head, boolean overlay, long timestamp) {
            this.head = head;
            this.overlay = overlay;
            this.timestamp = timestamp;
        }

        public BaseComponent[] getHead() {
            return head;
        }

        public boolean hasOverlay() {
            return overlay;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
