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
 *
 * MEMORY LEAK FIX: Added configurable maximum cache size with LRU eviction.
 */
public class HeadCache {

    private final JavaPlugin plugin;
    private final long cacheExpiration; // Now configurable!
    private final int maxCacheSize; // MEMORY LEAK FIX: Hard limit on cache size

    private final Map<String, CachedHead> cache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> pendingRequests = new ConcurrentHashMap<>();
    private BukkitTask cacheCleanupTask;

    public HeadCache(JavaPlugin plugin) {
        this.plugin = plugin;

        // Read cache time from config (in minutes), default to 5 minutes
        int cacheMinutes = plugin.getConfig().getInt("chathead.cache-time-minutes", 5);
        this.cacheExpiration = cacheMinutes * 60 * 1000L; // Convert to milliseconds

        // MEMORY LEAK FIX: Read max cache size from config, default to 5000
        this.maxCacheSize = plugin.getConfig().getInt("chathead.max-cache-size", 5000);

        plugin.getLogger().info("ChatHead cache expiration set to " + cacheMinutes + " minutes");
        plugin.getLogger().info("ChatHead max cache size set to " + maxCacheSize + " entries");
        startCacheCleanupTask();
    }

    /**
     * MEMORY LEAK FIX: Checks if cache size exceeds limit and evicts oldest entries.
     * This prevents unbounded memory growth from accumulating too many player heads.
     */
    private void enforceCacheSizeLimit() {
        if (cache.size() <= maxCacheSize) {
            return; // Within limit, no action needed
        }

        // Cache exceeded limit - evict oldest entries
        int toRemove = cache.size() - maxCacheSize;
        plugin.getLogger().fine("[HeadCache] Cache size (" + cache.size() + ") exceeded limit (" +
                                 maxCacheSize + "), removing " + toRemove + " oldest entries");

        // Find and remove oldest entries based on timestamp
        cache.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e1.getValue().getTimestamp(), e2.getValue().getTimestamp()))
            .limit(toRemove)
            .map(Map.Entry::getKey)
            .forEach(cache::remove);

        plugin.getLogger().fine("[HeadCache] Eviction complete, new size: " + cache.size());
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
                    // MEMORY LEAK FIX: Enforce cache size limit after adding new entry
                    enforceCacheSizeLimit();
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
                    // MEMORY LEAK FIX: Enforce cache size limit after adding new entry
                    enforceCacheSizeLimit();
                }
                pendingRequests.remove(cacheKey);
            });
        }

        return lastHead;
    }

    private boolean isExpired(CachedHead cachedHead) {
        return System.currentTimeMillis() - cachedHead.getTimestamp() > cacheExpiration;
    }

    /**
     * CACHE REFRESH FIX: Changed behavior to NOT delete expired entries.
     *
     * OLD BEHAVIOR (BAD):
     * - Deleted expired cache entries completely
     * - Caused empty heads on first message after expiration
     * - Second message would show head (after async fetch completed)
     *
     * NEW BEHAVIOR (GOOD):
     * - Keep expired entries as "stale cache"
     * - Always show SOME head (even if outdated) while refreshing
     * - Background async refresh updates to fresh head
     * - LRU eviction (enforceCacheSizeLimit) controls memory, not this task
     *
     * This provides better UX: players always see heads, never empty messages.
     */
    private void startCacheCleanupTask() {
        if (cacheCleanupTask != null) {
            cacheCleanupTask.cancel();
        }

        // Run monitoring task to log cache statistics (for debugging)
        // Does NOT delete expired entries - they serve as stale cache during refresh
        long cleanupInterval = cacheExpiration / 20; // Convert ms to ticks
        cacheCleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Count expired entries for monitoring
            long expiredCount = cache.values().stream()
                .filter(this::isExpired)
                .count();

            if (expiredCount > 0) {
                plugin.getLogger().fine("[HeadCache] Stats: " + cache.size() + " total entries, " +
                                       expiredCount + " expired (serving as stale cache)");
            }

            // NOTE: We do NOT remove expired entries here!
            // They are kept as "stale cache" to prevent empty heads during async refresh.
            // Memory is controlled by LRU eviction in enforceCacheSizeLimit().
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
     * DIAGNOSTIC: Gets current cache size.
     * Useful for monitoring memory usage.
     *
     * @return Number of entries currently in cache
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * DIAGNOSTIC: Gets maximum cache size limit.
     *
     * @return Maximum allowed cache entries
     */
    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    /**
     * DIAGNOSTIC: Clears all cached heads.
     * Should only be used for troubleshooting or after config changes.
     */
    public void clearCache() {
        int sizeBefore = cache.size();
        cache.clear();
        plugin.getLogger().info("[HeadCache] Cache manually cleared. Removed " + sizeBefore + " entries.");
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
