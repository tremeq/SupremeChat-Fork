package net.devscape.project.supremechat.listeners;

import net.devscape.project.supremechat.SupremeChat;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static net.devscape.project.supremechat.utils.Message.escapeUntrustedInput;
import static net.devscape.project.supremechat.utils.Message.format;
import static net.devscape.project.supremechat.utils.Message.replacePlaceholders;

/**
 * Advancement messages in chat (config section 'advancements'): VANILLA, HIDDEN or CUSTOM.
 * <p>
 * Hiding the vanilla message:
 * <ul>
 *   <li>Paper: per event (PlayerAdvancementDoneEvent#message(null)) - nothing is changed in the worlds.</li>
 *   <li>Spigot: has no such API, so the gamerule 'announceAdvancements' is set to false.
 *       Worlds changed by the plugin are remembered in advancement-gamerules.yml and set back to
 *       true as soon as they don't need to be hidden anymore.</li>
 * </ul>
 * Paper and Spigot use different advancement display types, so they are read with reflection.
 */
public class AdvancementMessages implements Listener {

    public static final String DEFAULT_MESSAGE = "&a%name% &7has made the advancement &e[%advancement%]";
    private static final String GAMERULE_FILE = "advancement-gamerules.yml";

    // Paper only: PlayerAdvancementDoneEvent#message(Component)
    private static final Method PAPER_SET_MESSAGE = findPaperSetMessage();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        FileConfiguration config = SupremeChat.getInstance().getConfig();
        Player player = event.getPlayer();
        String mode = getMode(config);
        boolean disabledWorld = config.getStringList("advancements.disabled-worlds").contains(player.getWorld().getName());

        if (mode.equals("VANILLA") && !disabledWorld) {
            return;
        }

        // Hide the vanilla message (on Spigot the gamerule already does it)
        if (PAPER_SET_MESSAGE != null) {
            try {
                PAPER_SET_MESSAGE.invoke(event, new Object[]{null});
            } catch (Exception ignored) {
                // cannot happen - the method exists
            }
        }

        if (!mode.equals("CUSTOM") || disabledWorld) {
            return;
        }

        // Only advancements Minecraft itself would announce (no recipes / hidden advancements)
        Object display = invoke(event.getAdvancement(), "getDisplay");
        if (display == null || !announcesToChat(display)) {
            return;
        }

        String template = config.getString("advancements.message", DEFAULT_MESSAGE);
        template = replacePlaceholders(player, template);
        String message = format(template
                .replace("%name%", player.getName())
                .replace("%world%", player.getWorld().getName())
                .replace("%advancement%", escapeUntrustedInput(getTitle(display, event.getAdvancement()))));

        boolean onlyThisWorld = "WORLD".equalsIgnoreCase(config.getString("advancements.show-to", "ALL"));
        Collection<? extends Player> recipients = onlyThisWorld ? player.getWorld().getPlayers() : Bukkit.getOnlinePlayers();
        for (Player recipient : recipients) {
            recipient.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        applyGamerules();
    }

    /**
     * @return VANILLA, HIDDEN or CUSTOM
     */
    private static String getMode(FileConfiguration config) {
        String mode = config.getString("advancements.mode", "VANILLA").trim().toUpperCase(Locale.ROOT);
        if (mode.equals("HIDDEN") || mode.equals("CUSTOM")) {
            return mode;
        }
        return "VANILLA";
    }

    /**
     * Spigot only: sets 'announceAdvancements' to false in every world where advancement
     * messages must be hidden, and back to true in worlds the plugin changed earlier but
     * which don't need it anymore. On Paper it only undoes old changes (Paper hides per event).
     * Called on enable, on /sc reload and when a world loads.
     */
    public static void applyGamerules() {
        SupremeChat plugin = SupremeChat.getInstance();
        FileConfiguration config = plugin.getConfig();
        String mode = getMode(config);
        List<String> disabledWorlds = config.getStringList("advancements.disabled-worlds");

        File file = new File(plugin.getDataFolder(), GAMERULE_FILE);
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        List<String> changedWorlds = new ArrayList<>(data.getStringList("changed-worlds"));
        boolean dirty = false;

        for (World world : Bukkit.getWorlds()) {
            boolean hide = PAPER_SET_MESSAGE == null
                    && (!mode.equals("VANILLA") || disabledWorlds.contains(world.getName()));
            Boolean announce = world.getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS);

            if (hide && (announce == null || announce)) {
                world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                if (!changedWorlds.contains(world.getName())) {
                    changedWorlds.add(world.getName());
                }
                dirty = true;
            } else if (!hide && changedWorlds.contains(world.getName())) {
                world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
                changedWorlds.remove(world.getName());
                dirty = true;
            }
        }

        if (dirty) {
            data.set("changed-worlds", changedWorlds);
            try {
                data.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not save " + GAMERULE_FILE + ": " + e.getMessage());
            }
        }
    }

    // ==================================================
    // Reflection helpers (Paper and Spigot have different display types)
    // ==================================================

    private static boolean announcesToChat(Object display) {
        Object announce = invoke(display, "shouldAnnounceChat");  // Spigot
        if (announce == null) {
            announce = invoke(display, "doesAnnounceToChat");     // Paper
        }
        return Boolean.TRUE.equals(announce);
    }

    private static String getTitle(Object display, Advancement advancement) {
        Object title = invoke(display, "getTitle");                // Spigot: String
        if (title instanceof String && !((String) title).isEmpty()) {
            return (String) title;
        }
        Object component = invoke(display, "title");               // Paper: Component
        if (component != null) {
            try {
                Class<?> paperComponents = Class.forName("io.papermc.paper.text.PaperComponents");
                Object serializer = paperComponents.getMethod("legacySectionSerializer").invoke(null);
                for (Method method : serializer.getClass().getMethods()) {
                    if (method.getName().equals("serialize") && method.getParameterCount() == 1) {
                        method.setAccessible(true);
                        Object text = method.invoke(serializer, component);
                        if (text instanceof String && !((String) text).isEmpty()) {
                            return (String) text;
                        }
                    }
                }
            } catch (Exception ignored) {
                // fall back to the advancement key below
            }
        }
        // e.g. "story/mine_stone" -> "Mine Stone"
        String key = advancement.getKey().getKey();
        key = key.substring(key.lastIndexOf('/') + 1).replace('_', ' ');
        StringBuilder pretty = new StringBuilder();
        for (String word : key.split(" ")) {
            if (!word.isEmpty()) {
                pretty.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
            }
        }
        return pretty.toString().trim();
    }

    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                try {
                    method.setAccessible(true);
                    return method.invoke(target);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static Method findPaperSetMessage() {
        for (Method method : PlayerAdvancementDoneEvent.class.getMethods()) {
            if (method.getName().equals("message") && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }
}
