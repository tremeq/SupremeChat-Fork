package net.devscape.project.supremechat.listeners;

import net.devscape.project.supremechat.SupremeChat;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;

import static net.devscape.project.supremechat.utils.Message.escapeUntrustedInput;
import static net.devscape.project.supremechat.utils.Message.format;
import static net.devscape.project.supremechat.utils.Message.replacePlaceholders;

/**
 * Death messages (config section 'death').
 * <p>
 * mode decides the text (CUSTOM / VANILLA) or hides it (HIDDEN), show-to / disabled-worlds /
 * ignore-gamerule decide who gets it. Works in every world, for every cause of death.
 */
public class DeathMessages implements Listener {

    public static final String DEFAULT_MESSAGE = "&c&lDEATH! &7%vanilla%";

    // Paper only: PlayerDeathEvent#setShowDeathMessages(boolean). Lets the server broadcast the
    // message even if the world's gamerule would hide it (keeps DiscordSRV & co. working).
    private static final Method SET_SHOW_DEATH_MESSAGES = findSetShowDeathMessages();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        FileConfiguration config = SupremeChat.getInstance().getConfig();
        Player victim = event.getEntity();
        World world = victim.getWorld();
        String mode = getMode(config);

        // Hidden everywhere, or in this world
        if (mode.equals("HIDDEN") || config.getStringList("death.disabled-worlds").contains(world.getName())) {
            event.setDeathMessage(null);
            return;
        }

        String vanillaMessage = event.getDeathMessage();
        String message = mode.equals("VANILLA") ? vanillaMessage : format(buildCustomMessage(config, victim, vanillaMessage));
        if (message == null || message.isEmpty()) {
            event.setDeathMessage(null);
            return;
        }

        Boolean gamerule = world.getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);
        boolean gameruleShows = gamerule == null || gamerule;
        boolean ignoreGamerule = config.getBoolean("death.ignore-gamerule", true);
        boolean onlyThisWorld = "WORLD".equalsIgnoreCase(config.getString("death.show-to", "ALL"));

        if (!gameruleShows && !ignoreGamerule) {
            // Respect the gamerule: nothing is shown in this world, like in vanilla.
            return;
        }

        if (!onlyThisWorld && (gameruleShows || SET_SHOW_DEATH_MESSAGES != null)) {
            // Normal case: the server broadcasts it to everyone. Other plugins (DiscordSRV...)
            // still see the message this way.
            if (!gameruleShows) {
                setShowDeathMessages(event);
            }
            if (mode.equals("CUSTOM")) {
                event.setDeathMessage(message);
            }
            return;
        }

        // Only this world, or a Spigot server where the gamerule would hide it - send it ourselves.
        event.setDeathMessage(null);
        Collection<? extends Player> recipients = onlyThisWorld ? world.getPlayers() : Bukkit.getOnlinePlayers();
        for (Player player : recipients) {
            player.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    /**
     * @return CUSTOM, VANILLA or HIDDEN. Old configs without 'death.mode' use 'death.enable'.
     */
    private static String getMode(FileConfiguration config) {
        String mode = config.getString("death.mode");
        if (mode == null) {
            return config.getBoolean("death.enable", true) ? "CUSTOM" : "VANILLA";
        }
        mode = mode.trim().toUpperCase(Locale.ROOT);
        if (mode.equals("VANILLA") || mode.equals("HIDDEN")) {
            return mode;
        }
        return "CUSTOM";
    }

    private static String buildCustomMessage(FileConfiguration config, Player victim, String vanillaMessage) {
        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        String cause = lastDamage != null ? lastDamage.getCause().name().toLowerCase(Locale.ROOT) : "unknown";
        Player killer = victim.getKiller();

        // A player kill has its own message, then the message of the cause, then 'default'
        String template = null;
        if (killer != null) {
            template = config.getString("death.messages.entity_player");
        }
        if (template == null) {
            template = config.getString("death.messages." + cause);
        }
        if (template == null) {
            template = config.getString("death.messages.default", DEFAULT_MESSAGE);
        }

        // PlaceholderAPI on the config text first - names inserted below are never parsed
        template = replacePlaceholders(victim, template);

        Entity damager = findDamager(lastDamage);
        String mobName;
        if (damager == null) {
            mobName = "Unknown Mob";
        } else if (damager.getCustomName() != null) {
            mobName = damager.getCustomName();
        } else {
            mobName = damager.getType().name().toLowerCase(Locale.ROOT).replace("_", " ");
        }

        // Plain replace(): replaceAll() would break on '$' or '\' in names.
        // Custom names and the vanilla message (item names) can come from players.
        return template
                .replace("%name%", victim.getName())
                .replace("%killer%", killer != null ? killer.getName() : "Unknown")
                .replace("%mob%", escapeUntrustedInput(mobName))
                .replace("%world%", victim.getWorld().getName())
                .replace("%vanilla%", escapeUntrustedInput(vanillaMessage != null ? vanillaMessage : victim.getName() + " died"));
    }

    /**
     * The entity that caused the death: the shooter of a projectile, whoever lit the TNT,
     * otherwise the attacking entity itself.
     */
    private static Entity findDamager(EntityDamageEvent lastDamage) {
        if (!(lastDamage instanceof EntityDamageByEntityEvent)) {
            return null;
        }
        Entity damager = ((EntityDamageByEntityEvent) lastDamage).getDamager();
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Entity) {
                return (Entity) shooter;
            }
        }
        if (damager instanceof TNTPrimed && ((TNTPrimed) damager).getSource() != null) {
            return ((TNTPrimed) damager).getSource();
        }
        return damager;
    }

    private static Method findSetShowDeathMessages() {
        try {
            return PlayerDeathEvent.class.getMethod("setShowDeathMessages", boolean.class);
        } catch (NoSuchMethodException e) {
            return null; // Spigot
        }
    }

    private static void setShowDeathMessages(PlayerDeathEvent event) {
        try {
            SET_SHOW_DEATH_MESSAGES.invoke(event, true);
        } catch (Exception ignored) {
            // cannot happen - only called when the method exists
        }
    }
}
