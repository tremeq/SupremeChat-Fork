package net.devscape.project.supremechat.managers;

import net.devscape.project.supremechat.SupremeChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stores per-player chat preferences that must survive restarts:
 * <ul>
 *     <li>msgtoggle - players who disabled receiving private messages</li>
 *     <li>ignore - which players each player is ignoring</li>
 * </ul>
 * Data is persisted to plugins/SupremeChat/data.yml.
 */
public class ChatDataManager {

    private final SupremeChat plugin;
    private final File file;
    private FileConfiguration data;

    // players who have disabled receiving private messages
    private final Set<UUID> msgToggled = new HashSet<>();
    // player -> set of players they ignore
    private final Map<UUID, Set<UUID>> ignores = new HashMap<>();

    public ChatDataManager(SupremeChat plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create data.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(file);

        msgToggled.clear();
        ignores.clear();

        for (String id : data.getStringList("msgtoggle")) {
            try {
                msgToggled.add(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (data.isConfigurationSection("ignores")) {
            for (String id : data.getConfigurationSection("ignores").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(id);
                    Set<UUID> set = new HashSet<>();
                    for (String ignoredId : data.getStringList("ignores." + id)) {
                        try {
                            set.add(UUID.fromString(ignoredId));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    if (!set.isEmpty()) {
                        ignores.put(uuid, set);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void save() {
        data.set("msgtoggle", msgToggled.stream().map(UUID::toString).collect(Collectors.toList()));

        // Rewrite the ignores section from scratch to drop emptied entries.
        data.set("ignores", null);
        for (Map.Entry<UUID, Set<UUID>> entry : ignores.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            data.set("ignores." + entry.getKey(),
                    entry.getValue().stream().map(UUID::toString).collect(Collectors.toList()));
        }

        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save data.yml: " + e.getMessage());
        }
    }

    // ---------- msgtoggle ----------

    public boolean hasMessagesDisabled(UUID uuid) {
        return msgToggled.contains(uuid);
    }

    /**
     * Toggles private-message receiving for a player.
     *
     * @return true if messages are now DISABLED, false if now enabled.
     */
    public boolean toggleMessages(UUID uuid) {
        boolean nowDisabled;
        if (msgToggled.contains(uuid)) {
            msgToggled.remove(uuid);
            nowDisabled = false;
        } else {
            msgToggled.add(uuid);
            nowDisabled = true;
        }
        save();
        return nowDisabled;
    }

    // ---------- ignore ----------

    public boolean isIgnoring(UUID player, UUID target) {
        Set<UUID> set = ignores.get(player);
        return set != null && set.contains(target);
    }

    /**
     * Toggles whether {@code player} ignores {@code target}.
     *
     * @return true if now IGNORING, false if no longer ignoring.
     */
    public boolean toggleIgnore(UUID player, UUID target) {
        Set<UUID> set = ignores.computeIfAbsent(player, k -> new HashSet<>());
        boolean nowIgnoring;
        if (set.contains(target)) {
            set.remove(target);
            nowIgnoring = false;
        } else {
            set.add(target);
            nowIgnoring = true;
        }
        if (set.isEmpty()) {
            ignores.remove(player);
        }
        save();
        return nowIgnoring;
    }
}
