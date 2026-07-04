package net.devscape.project.supremechat.hooks;

import net.devscape.project.supremechat.SupremeChat;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Isolates ALL references to the Vault API (net.milkbowl.vault.*).
 * <p>
 * This class must only ever be touched AFTER confirming that the Vault plugin
 * is installed (see {@link SupremeChat#isVaultEnabled()}). Because every method
 * here references Vault classes, the JVM will only load those classes when this
 * class is actively used - keeping the rest of the plugin safe to run on servers
 * that don't have Vault installed. Rank/group lookups simply fall back to
 * "default" in that case instead of throwing NoClassDefFoundError.
 */
public final class VaultHook {

    private static Chat chat;
    private static Permission perms;

    private VaultHook() {
    }

    /**
     * Attempts to hook into Vault's Chat and Permission service providers.
     *
     * @return true if both providers were found and stored, false otherwise.
     */
    public static boolean setup(SupremeChat plugin, boolean debugMode) {
        RegisteredServiceProvider<Permission> permProvider = plugin.getServer()
                .getServicesManager().getRegistration(Permission.class);
        if (permProvider == null) {
            plugin.getLogger().warning("Vault Permission provider not found!");
            if (debugMode) {
                plugin.getLogger().info("[DEBUG] Permission provider: NOT AVAILABLE");
            }
            return false;
        }
        perms = permProvider.getProvider();
        if (debugMode) {
            plugin.getLogger().info("[DEBUG] Permission provider: " + perms.getName());
        }

        RegisteredServiceProvider<Chat> chatProvider = plugin.getServer()
                .getServicesManager().getRegistration(Chat.class);
        if (chatProvider == null) {
            plugin.getLogger().warning("Vault Chat provider not found!");
            if (debugMode) {
                plugin.getLogger().info("[DEBUG] Chat provider: NOT AVAILABLE");
            }
            return false;
        }
        chat = chatProvider.getProvider();

        if (debugMode) {
            plugin.getLogger().info("[DEBUG] Chat provider: " + chat.getName());
            plugin.getLogger().info("[DEBUG] Vault setup: SUCCESSFUL");
        }
        return true;
    }

    /**
     * @return the player's primary permission group (rank), or null if unavailable.
     */
    public static String getPrimaryGroup(Player player) {
        if (chat == null) {
            return null;
        }
        return chat.getPrimaryGroup(player);
    }

    public static Chat getChat() {
        return chat;
    }

    public static Permission getPermissions() {
        return perms;
    }
}
