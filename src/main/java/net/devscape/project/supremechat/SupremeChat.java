package net.devscape.project.supremechat;

import net.devscape.project.supremechat.chatgames.GameManager;
import net.devscape.project.supremechat.chathead.ChatHeadAPI;
import net.devscape.project.supremechat.chathead.ResourcePackManager;
import net.devscape.project.supremechat.commands.ChannelCommand;
import net.devscape.project.supremechat.commands.EmojisCommands;
import net.devscape.project.supremechat.commands.MessageCommand;
import net.devscape.project.supremechat.commands.ReplyCommand;
import net.devscape.project.supremechat.commands.SCCommand;
import net.devscape.project.supremechat.hooks.DiscordSRVHook;
import net.devscape.project.supremechat.hooks.FloodgateHook;
import net.devscape.project.supremechat.hooks.Metrics;
import net.devscape.project.supremechat.listeners.*;
import net.devscape.project.supremechat.managers.ChannelManager;
import net.devscape.project.supremechat.utils.FormatUtil;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SupremeChat extends JavaPlugin {

    /**
     * Default resource pack URL for ChatHead rendering.
     * This pack is hosted on GitHub and works out of the box.
     * Users can override this in config.yml with their own pack.
     */
    public static final String DEFAULT_RESOURCE_PACK = "https://github.com/OGminso/ChatHeadFont/raw/main/pack.zip";

    private static SupremeChat instance;
    private ChannelManager channelManager;
    private GameManager gameManager;
    private ResourcePackManager resourcePackManager;

    private static Permission perms = null;
    private static Chat chat;

    private final List<Player> chatDelayList = new ArrayList<>();
    // prevention list for preventing bot attacks
    private final List<Player> prevention = new ArrayList<>();
    private final List<Player> commandDelayList = new ArrayList<>();
    private final Map<Player, String> lastMessage = new HashMap<>();
    // tracking for /reply command - stores last person each player messaged
    private final Map<Player, Player> lastMessenger = new HashMap<>();
    private FormatUtil formattingUtils;

    public static SupremeChat getInstance() {
        return instance;
    }

    public static Chat getChat() {
        return chat;
    }

    @Override
    public void onEnable() {
        init();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        // Shutdown ChatHead API
        try {
            if (ChatHeadAPI.getInstance() != null) {
                ChatHeadAPI.getInstance().shutdown();
                getLogger().info("ChatHeadAPI shutdown successfully");
            }
        } catch (Exception e) {
            // API not initialized, ignore
        }

        // Stop chat games scheduler
        if (gameManager != null) {
            gameManager.stopScheduler();
        }

        // Disable Floodgate integration
        try {
            FloodgateHook.disable();
        } catch (Exception e) {
            // Ignore if not initialized
        }

        chatDelayList.clear();
        lastMessage.clear();
        commandDelayList.clear();
        lastMessenger.clear();
    }

    private void init() {
        instance = this;

        saveDefaultConfig();
        configValidator();

        setupVault();

        // Initialize ChatHead API with offline mode support
        try {
            ChatHeadAPI.initialize(this);
            getLogger().info("ChatHeadAPI initialized successfully!");
        } catch (Exception e) {
            getLogger().warning("Failed to initialize ChatHeadAPI: " + e.getMessage());
            e.printStackTrace();
        }

        // Initialize Resource Pack Manager for automatic distribution
        try {
            resourcePackManager = new ResourcePackManager(this);
            if (resourcePackManager.isEnabled()) {
                getServer().getPluginManager().registerEvents(resourcePackManager, this);
                getLogger().info("ResourcePackManager initialized and registered");
            } else {
                getLogger().info("ResourcePackManager disabled in config");
            }
        } catch (Exception e) {
            getLogger().warning("Failed to initialize ResourcePackManager: " + e.getMessage());
            e.printStackTrace();
        }

        channelManager = new ChannelManager();
        gameManager = new GameManager(this);
        gameManager.startScheduler();

        getCommand("supremechat").setExecutor(new SCCommand());
        getCommand("channel").setExecutor(new ChannelCommand());
        getCommand("emojis").setExecutor(new EmojisCommands());

        // Register private message commands
        MessageCommand msgCommand = new MessageCommand();
        getCommand("msg").setExecutor(msgCommand);
        getCommand("tell").setExecutor(msgCommand);
        getCommand("whisper").setExecutor(msgCommand);

        ReplyCommand replyCommand = new ReplyCommand();
        getCommand("reply").setExecutor(replyCommand);
        getCommand("r").setExecutor(replyCommand);

        getServer().getPluginManager().registerEvents(new Formatting(), this);
        getServer().getPluginManager().registerEvents(new JoinLeave(), this);
        getServer().getPluginManager().registerEvents(new CommandFilter(), this);
        getServer().getPluginManager().registerEvents(new CustomCommands(), this);
        getServer().getPluginManager().registerEvents(new Mention(), this);
        getServer().getPluginManager().registerEvents(new CommandSpy(), this);
        getServer().getPluginManager().registerEvents(new DeathMessages(), this);

        // Initialize DiscordSRV integration only if the plugin is available
        if (Bukkit.getPluginManager().getPlugin("DiscordSRV") != null) {
            try {
                DiscordSRVHook.initialize();
                getLogger().info("DiscordSRV integration enabled!");
            } catch (NoClassDefFoundError e) {
                getLogger().warning("Failed to load DiscordSRV classes: " + e.getMessage());
                getLogger().warning("DiscordSRV integration disabled. Make sure DiscordSRV is properly installed.");
            } catch (Exception e) {
                getLogger().warning("Failed to initialize DiscordSRV integration: " + e.getMessage());
            }
        } else {
            getLogger().info("DiscordSRV not found - integration disabled.");
        }

        // Initialize Floodgate integration only if the plugin is available
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
            try {
                FloodgateHook.initialize();
                getLogger().info("Floodgate integration enabled! Bedrock players will not see ChatHeads.");
            } catch (NoClassDefFoundError e) {
                getLogger().warning("Failed to load Floodgate classes: " + e.getMessage());
                getLogger().warning("Floodgate integration disabled. Make sure Floodgate is properly installed.");
            } catch (Exception e) {
                getLogger().warning("Failed to initialize Floodgate integration: " + e.getMessage());
            }
        } else {
            getLogger().info("Floodgate not found - Bedrock player detection disabled.");
        }

        callMetrics();
    }

    private boolean setupVault() {
        boolean debugMode = getConfig().getBoolean("debug-mode", false);

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault plugin not found! Some features may not work correctly.");
            if (debugMode) {
                getLogger().info("[DEBUG] Vault status: NOT FOUND");
            }
            return false;
        }

        if (debugMode) {
            getLogger().info("[DEBUG] Vault status: FOUND");
        }

        RegisteredServiceProvider<Permission> permProvider = getServer().getServicesManager()
                .getRegistration(Permission.class);
        if (permProvider == null) {
            getLogger().warning("Vault Permission provider not found!");
            if (debugMode) {
                getLogger().info("[DEBUG] Permission provider: NOT AVAILABLE");
            }
            return false;
        }

        perms = permProvider.getProvider();
        if (debugMode) {
            getLogger().info("[DEBUG] Permission provider: " + perms.getName());
        }

        RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager()
                .getRegistration(Chat.class);
        if (chatProvider == null) {
            getLogger().warning("Vault Chat provider not found!");
            if (debugMode) {
                getLogger().info("[DEBUG] Chat provider: NOT AVAILABLE");
            }
            return false;
        }
        chat = chatProvider.getProvider();

        if (debugMode) {
            getLogger().info("[DEBUG] Chat provider: " + chat.getName());
            getLogger().info("[DEBUG] Vault setup: SUCCESSFUL");
        } else {
            getLogger().info("Vault hooked successfully!");
        }

        return true;
    }

    public static Permission getPermissions() {
        return perms;
    }


    public List<Player> getChatDelayList() {
        return chatDelayList;
    }

    public void reload() {
        super.reloadConfig();
        configValidator();
        channelManager.reloadChannels();

        // Reload ChatHeadAPI
        try {
            if (ChatHeadAPI.getInstance() != null) {
                ChatHeadAPI.getInstance().shutdown();
            }
            ChatHeadAPI.initialize(this);
            getLogger().info("ChatHeadAPI reloaded successfully!");
        } catch (Exception e) {
            getLogger().warning("Failed to reload ChatHeadAPI: " + e.getMessage());
        }

        // Reload chat games system
        if (gameManager != null) {
            gameManager.reload();
        }

        // Reload DiscordSRV integration if available
        if (Bukkit.getPluginManager().getPlugin("DiscordSRV") != null) {
            try {
                DiscordSRVHook.reload();
            } catch (NoClassDefFoundError e) {
                getLogger().warning("Failed to reload DiscordSRV integration: " + e.getMessage());
            } catch (Exception e) {
                getLogger().warning("Error reloading DiscordSRV: " + e.getMessage());
            }
        }

        // Reload Floodgate integration if available
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
            try {
                FloodgateHook.disable();
                FloodgateHook.initialize();
                getLogger().info("Floodgate integration reloaded!");
            } catch (NoClassDefFoundError e) {
                getLogger().warning("Failed to reload Floodgate integration: " + e.getMessage());
            } catch (Exception e) {
                getLogger().warning("Error reloading Floodgate: " + e.getMessage());
            }
        }
    }

    public Map<Player, String> getLastMessage() {
        return lastMessage;
    }

    public List<Player> getCommandDelayList() {
        return commandDelayList;
    }

    public ChannelManager getChannelManager() { return channelManager; }

    public GameManager getGameManager() {
        return gameManager;
    }

    public void setLastMessenger(Player player, Player target) {
        lastMessenger.put(player, target);
    }

    public Player getLastMessenger(Player player) {
        return lastMessenger.get(player);
    }

    public Map<Player, Player> getLastMessengerMap() {
        return lastMessenger;
    }

    private void callMetrics() {
        int pluginId = 18329;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new Metrics.SimplePie("used_language", () -> getConfig().getString("language", "en")));

        metrics.addCustomChart(new Metrics.DrilldownPie("java_version", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            String javaVersion = System.getProperty("java.version");
            Map<String, Integer> entry = new HashMap<>();
            entry.put(javaVersion, 1);
            if (javaVersion.startsWith("1.7")) {
                map.put("Java 1.7", entry);
            } else if (javaVersion.startsWith("1.8")) {
                map.put("Java 1.8", entry);
            } else if (javaVersion.startsWith("1.9")) {
                map.put("Java 1.9", entry);
            } else {
                map.put("Other", entry);
            }
            return map;
        }));
    }

    public List<Player> getPrevention() {
        return prevention;
    }

    private void configValidator() {
        SupremeChat plugin = SupremeChat.getInstance();
        FileConfiguration config = plugin.getConfig();
        boolean configChanged = false; // Track if config needs saving

        // Validate channels
        if (!config.isSet("channels")) {
            setDefaultChannels(config);
            configChanged = true;
        } else {
            // Ensure permissions and colors are set for existing channels
            for (String key : config.getConfigurationSection("channels").getKeys(false)) {
                if (config.getString("channels." + key + ".permission") == null) {
                    config.set("channels." + key + ".permission", "None");
                    config.set("channels." + key + ".chat-color", "&7");
                    configChanged = true;
                }
            }
        }

        // Validate death messages.
        if (!config.isConfigurationSection("death")) {
            config.set("death.enable", true);
            config.set("death.messages.contact", "&c%name% was slain!");
            config.set("death.messages.entity_attack", "&e%name% was killed by a mob.");
            config.set("death.messages.fall", "&b%name% fell from a high place.");
            config.set("death.messages.drowning", "&3%name% drowned.");
            config.set("death.messages.fire", "&c%name% burned to death.");
            config.set("death.messages.projectile", "&d%name% was shot.");
            config.set("death.messages.magic", "&5%name% was killed by magic.");
            config.set("death.messages.suicide", "&7%name% took their own life.");
            config.set("death.messages.unknown", "&7%name% died mysteriously.");
            configChanged = true;
        }

        if (!config.isSet("per-world-chat")) {
            config.set("per-world-chat", false);
            configChanged = true;
        }

        // Validate emojis
        if (!config.isSet("emojis")) {
            setDefaultEmojis(config);
            configChanged = true;
        }

        // Validate mentions
        if (!config.isSet("mention")) {
            setDefaultMentions(config);
            configChanged = true;
        }

        // Validate chat games win message
        if (!config.isSet("chatgames.strings.game-win")) {
            config.set("chatgames.strings.game-win", "&c&lC&6&lH&e&lA&a&lT&b&l &9&lG&d&lA&5&lM&c&lE&6&lS &8&l➟ &a%player% &7won the game!");
            configChanged = true;
        }

        // Validate chat and command cooldown warning toggles (v1.15+)
        if (!config.isSet("chat-warn-enabled")) {
            config.set("chat-warn-enabled", true);
            getLogger().info("Added new config option: chat-warn-enabled (default: true)");
            configChanged = true;
        }
        if (!config.isSet("command-warn-enabled")) {
            config.set("command-warn-enabled", true);
            getLogger().info("Added new config option: command-warn-enabled (default: true)");
            configChanged = true;
        }

        // Validate ChatHead API configuration
        if (!config.isSet("chathead.enabled")) {
            // Full chathead configuration with all options
            config.set("chathead.enabled", true);
            config.set("chathead.skin-source", "AUTO");
            config.set("chathead.cache-time-minutes", 5);
            config.set("chathead.use-overlay-by-default", true);

            // Add helpful comments
            getLogger().info("ChatHead configuration created with default values");
            getLogger().info("  - enabled: true");
            getLogger().info("  - skin-source: AUTO (auto-detects online/offline mode)");
            getLogger().info("  - cache-time-minutes: 5");
            getLogger().info("  - use-overlay-by-default: true");

            configChanged = true;
        }

        // Ensure all chathead sub-options exist (for upgrades from older versions)
        if (!config.isSet("chathead.use-overlay-by-default")) {
            config.set("chathead.use-overlay-by-default", true);
            configChanged = true;
        }
        if (!config.isSet("chathead.disable-for-bedrock")) {
            config.set("chathead.disable-for-bedrock", true);
            configChanged = true;
        }

        // Validate ChatHead ResourcePack configuration
        if (!config.isSet("chathead.resourcepack.auto-send")) {
            config.set("chathead.resourcepack.auto-send", true);
            config.set("chathead.resourcepack.url", DEFAULT_RESOURCE_PACK);
            config.set("chathead.resourcepack.sha1", "");
            config.set("chathead.resourcepack.prompt", "§6§lSupremeChat §aChatHead Pack\n§7Required for displaying player heads in chat\n§e§lHighly Recommended!");
            config.set("chathead.resourcepack.force", false);

            getLogger().info("ChatHead resource pack configuration created with default URL");
            getLogger().info("  Default pack: " + DEFAULT_RESOURCE_PACK);
            getLogger().info("  You can change the URL in config.yml to use your own pack");

            configChanged = true;
        }

        // Save config only once if any changes were made
        if (configChanged) {
            plugin.saveConfig();
            getLogger().info("Config updated with new options. Your custom settings have been preserved.");
        }
    }

    // Method to set default channels
    private void setDefaultChannels(FileConfiguration config) {
        config.set("channels.english.enable", true);
        config.set("channels.english.permission", "None");
        config.set("channels.english.chat-color", "&7");
        config.set("channels.english.format", "&e[ENGLISH] &7%name% &8➟ &7%message%");

        config.set("channels.spanish.enable", true);
        config.set("channels.spanish.permission", "None");
        config.set("channels.spanish.chat-color", "&7");
        config.set("channels.spanish.format", "&e[SPANISH] &7%name% &8➟ &7%message%");

        config.set("channels.french.enable", true);
        config.set("channels.french.permission", "None");
        config.set("channels.french.chat-color", "&7");
        config.set("channels.french.format", "&e[FRENCH] &7%name% &8➟ &7%message%");
    }

    // Method to set default emojis
    private void setDefaultEmojis(FileConfiguration config) {
        config.set("emojis.smile.emoticon", ":)");
        config.set("emojis.smile.emoji", "&e😊");
        config.set("emojis.sad.emoticon", ":(");
        config.set("emojis.sad.emoji", "&9😢");
        config.set("emojis.wink.emoticon", ";)");
        config.set("emojis.wink.emoji", "&6😉");
        config.set("emojis.thumbs_up.emoticon", ":+1:");
        config.set("emojis.thumbs_up.emoji", "&a👍");
        config.set("emojis.thumbs_down.emoticon", ":-1:");
        config.set("emojis.thumbs_down.emoji", "&c👎");
        config.set("emojis.heart.emoticon", "<3");
        config.set("emojis.heart.emoji", "&c❤");
        config.set("emojis.fire.emoticon", ":fire:");
        config.set("emojis.fire.emoji", "&c🔥");
        config.set("emojis.laugh.emoticon", ":D");
        config.set("emojis.laugh.emoji", "&a😄");
        config.set("emojis.cool.emoticon", "B)");
        config.set("emojis.cool.emoji", "&b😎");
        config.set("emojis.surprised.emoticon", ":o");
        config.set("emojis.surprised.emoji", "&e😲");
        config.set("emojis.angry.emoticon", ">:(");
        config.set("emojis.angry.emoji", "&4😠");
        config.set("emojis.party.emoticon", ":party:");
        config.set("emojis.party.emoji", "&d🎉");
        config.set("emojis.clap.emoticon", ":clap:");
        config.set("emojis.clap.emoji", "&a👏");
    }

    // Method to set default mentions
    private void setDefaultMentions(FileConfiguration config) {
        config.set("mention.player.permission", "supremechat.mention.player");
        config.set("mention.player.target", "@");
        config.set("mention.player.replacement", "&e%target%");
        config.set("mention.player.spaces", true);
        config.set("mention.player.sound.enable", true);
        config.set("mention.player.sound.sound", "ENTITY_LEVELUP");

        config.set("mention.everyone.permission", "supremechat.mention.everyone");
        config.set("mention.everyone.target", "@everyone");
        config.set("mention.everyone.replacement", "&e@everyone&f");
        config.set("mention.everyone.spaces", true);
        config.set("mention.everyone.sound.enable", true);
        config.set("mention.everyone.sound.sound", "ENTITY_LEVELUP");
    }


}