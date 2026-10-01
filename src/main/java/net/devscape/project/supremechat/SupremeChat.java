package net.devscape.project.supremechat;

import net.devscape.project.supremechat.chatgames.GameManager;
import net.devscape.project.supremechat.chathead.ChatHeadAPI;
import net.devscape.project.supremechat.chathead.ResourcePackManager;
import net.devscape.project.supremechat.commands.AdminChatCommand;
import net.devscape.project.supremechat.commands.ChannelCommand;
import net.devscape.project.supremechat.commands.EmojisCommands;
import net.devscape.project.supremechat.commands.IgnoreCommand;
import net.devscape.project.supremechat.commands.MessageCommand;
import net.devscape.project.supremechat.commands.MsgToggleCommand;
import net.devscape.project.supremechat.commands.ReplyCommand;
import net.devscape.project.supremechat.commands.SCCommand;
import net.devscape.project.supremechat.hooks.DiscordSRVHook;
import net.devscape.project.supremechat.hooks.FloodgateHook;
import net.devscape.project.supremechat.hooks.Metrics;
import net.devscape.project.supremechat.hooks.VaultHook;
import net.devscape.project.supremechat.listeners.*;
import net.devscape.project.supremechat.managers.ChannelManager;
import net.devscape.project.supremechat.managers.ChatDataManager;
import net.devscape.project.supremechat.utils.FormatUtil;
import net.devscape.project.supremechat.utils.MiniMessageFormatter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SupremeChat extends JavaPlugin {

    /**
     * Default resource pack URL for ChatHead rendering.
     * This pack is hosted on GitHub and works out of the box.
     * Users can override this in config.yml with their own pack.
     */
    public static final String DEFAULT_RESOURCE_PACK = "https://github.com/OGminso/ChatHeadFont/raw/main/pack.zip";

    private static SupremeChat instance;
    private ChannelManager channelManager;
    private ChatDataManager chatDataManager;
    private GameManager gameManager;
    private ResourcePackManager resourcePackManager;

    // Whether Vault was found AND both its Chat/Permission providers were hooked.
    // Kept as a plain boolean so callers can guard Vault usage WITHOUT referencing
    // any net.milkbowl.vault.* class (which would crash when Vault isn't installed).
    private static boolean vaultEnabled = false;

    // MEMORY LEAK FIX: Use UUID instead of Player objects to prevent memory leaks
    // Player objects are recreated on each join, old references prevent garbage collection
    private final List<Player> chatDelayList = new ArrayList<>();
    // prevention list for preventing bot attacks
    private final List<Player> prevention = new ArrayList<>();
    private final List<Player> commandDelayList = new ArrayList<>();
    private final Map<UUID, String> lastMessage = new HashMap<>();
    // tracking for /reply command - stores last person each player messaged
    // Changed from Map<Player, Player> to Map<UUID, UUID> to prevent memory leaks
    private final Map<UUID, UUID> lastMessenger = new HashMap<>();
    private FormatUtil formattingUtils;

    public static SupremeChat getInstance() {
        return instance;
    }

    /**
     * @return true if Vault is installed and its Chat provider was hooked.
     * Callers should check this BEFORE using any Vault-backed feature.
     */
    public static boolean isVaultEnabled() {
        return vaultEnabled;
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

        // Persist player chat data (msgtoggle / ignore lists) before shutdown.
        if (chatDataManager != null) {
            chatDataManager.save();
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
        MiniMessageFormatter.reload(this);

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
        chatDataManager = new ChatDataManager(this);
        gameManager = new GameManager(this);
        gameManager.startScheduler();

        SCCommand scCommand = new SCCommand();
        getCommand("supremechat").setExecutor(scCommand);
        getCommand("supremechat").setTabCompleter(scCommand);

        ChannelCommand channelCommand = new ChannelCommand();
        getCommand("channel").setExecutor(channelCommand);
        getCommand("channel").setTabCompleter(channelCommand);

        getCommand("emojis").setExecutor(new EmojisCommands());

        // Register private message commands
        MessageCommand msgCommand = new MessageCommand();
        getCommand("msg").setExecutor(msgCommand);
        getCommand("tell").setExecutor(msgCommand);
        getCommand("whisper").setExecutor(msgCommand);

        ReplyCommand replyCommand = new ReplyCommand();
        getCommand("reply").setExecutor(replyCommand);
        getCommand("r").setExecutor(replyCommand);

        getCommand("msgtoggle").setExecutor(new MsgToggleCommand());
        getCommand("ignore").setExecutor(new IgnoreCommand());
        getCommand("adminchat").setExecutor(new AdminChatCommand());

        getServer().getPluginManager().registerEvents(new Formatting(), this);
        getServer().getPluginManager().registerEvents(new JoinLeave(), this);
        getServer().getPluginManager().registerEvents(new CommandFilter(), this);
        getServer().getPluginManager().registerEvents(new CustomCommands(), this);
        getServer().getPluginManager().registerEvents(new Mention(), this);
        getServer().getPluginManager().registerEvents(new CommandSpy(), this);
        getServer().getPluginManager().registerEvents(new DeathMessages(), this);
        getServer().getPluginManager().registerEvents(new AdvancementMessages(), this);
        AdvancementMessages.applyGamerules();

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

        // Only touch Vault classes if the Vault plugin is actually installed.
        // This guard MUST come before any reference to net.milkbowl.vault.* so the
        // plugin runs cleanly on servers without Vault (rank formatting is skipped).
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().info("Vault not found - rank/group based chat formatting is disabled.");
            if (debugMode) {
                getLogger().info("[DEBUG] Vault status: NOT FOUND");
            }
            vaultEnabled = false;
            return false;
        }

        if (debugMode) {
            getLogger().info("[DEBUG] Vault status: FOUND");
        }

        try {
            vaultEnabled = VaultHook.setup(this, debugMode);
        } catch (NoClassDefFoundError | Exception e) {
            getLogger().warning("Failed to hook into Vault: " + e.getMessage());
            vaultEnabled = false;
        }

        if (vaultEnabled) {
            getLogger().info("Vault hooked successfully!");
        } else {
            getLogger().info("Vault present but no Chat provider - rank/group formatting disabled.");
        }

        return vaultEnabled;
    }


    public List<Player> getChatDelayList() {
        return chatDelayList;
    }

    public void reload() {
        super.reloadConfig();
        configValidator();
        MiniMessageFormatter.reload(this);
        AdvancementMessages.applyGamerules();
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

    public Map<UUID, String> getLastMessage() {
        return lastMessage;
    }

    public List<Player> getCommandDelayList() {
        return commandDelayList;
    }

    public ChannelManager getChannelManager() { return channelManager; }

    public ChatDataManager getChatDataManager() { return chatDataManager; }

    public GameManager getGameManager() {
        return gameManager;
    }

    /**
     * Sets the last messenger for reply tracking.
     * MEMORY LEAK FIX: Now uses UUID instead of Player objects.
     *
     * @param player The player who sent the message
     * @param target The player who received the message
     */
    public void setLastMessenger(Player player, Player target) {
        lastMessenger.put(player.getUniqueId(), target.getUniqueId());
    }

    /**
     * Gets the last messenger for a player (for /reply command).
     * MEMORY LEAK FIX: Now uses UUID and returns Player by looking up online player.
     *
     * @param player The player to check
     * @return The last messenger, or null if not found or not online
     */
    public Player getLastMessenger(Player player) {
        UUID targetUUID = lastMessenger.get(player.getUniqueId());
        if (targetUUID == null) {
            return null;
        }
        return Bukkit.getPlayer(targetUUID);
    }

    /**
     * Gets the last messenger map.
     * MEMORY LEAK FIX: Now returns Map<UUID, UUID> instead of Map<Player, Player>.
     *
     * @return The last messenger map
     */
    public Map<UUID, UUID> getLastMessengerMap() {
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
            config.set("death.mode", "CUSTOM");
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

        // Death message options added in v1.15.3. 'death.enable' is replaced by 'death.mode':
        // enable: true -> CUSTOM, enable: false -> VANILLA (same behaviour as before).
        if (!config.isSet("death.mode")) {
            config.set("death.mode", config.getBoolean("death.enable", true) ? "CUSTOM" : "VANILLA");
            config.set("death.enable", null);
            getLogger().info("Config: 'death.enable' was replaced by 'death.mode' (" + config.getString("death.mode") + ")");
            configChanged = true;
        }
        if (!config.isSet("death.show-to")) {
            config.set("death.show-to", "ALL");
            config.set("death.disabled-worlds", new ArrayList<String>());
            config.set("death.ignore-gamerule", true);
            setCommentsSafely(config, "death.mode", Arrays.asList(
                    "CUSTOM  - messages from 'messages' below",
                    "VANILLA - Minecraft's own death messages",
                    "HIDDEN  - no death messages in chat at all"));
            setCommentsSafely(config, "death.show-to", Arrays.asList(
                    "Who sees a death message: ALL (whole server) or WORLD (only the world where the player died)"));
            setCommentsSafely(config, "death.disabled-worlds", Arrays.asList(
                    "Worlds where death messages are never shown"));
            setCommentsSafely(config, "death.ignore-gamerule", Arrays.asList(
                    "true = also show death messages in worlds with the gamerule showDeathMessages: false"));
            getLogger().info("Added new death message options: show-to, disabled-worlds, ignore-gamerule");
            configChanged = true;
        }
        if (!config.isSet("death.messages.default")) {
            config.set("death.messages.default", DeathMessages.DEFAULT_MESSAGE);
            setCommentsSafely(config, "death.messages.default", Arrays.asList(
                    "Used for every cause of death without its own message. %vanilla% = Minecraft's message"));
            configChanged = true;
        }

        // Advancement messages (added in v1.15.3) - VANILLA keeps the old behaviour
        if (!config.isSet("advancements.mode")) {
            config.set("advancements.mode", "VANILLA");
            config.set("advancements.show-to", "ALL");
            config.set("advancements.disabled-worlds", new ArrayList<String>());
            config.set("advancements.message", AdvancementMessages.DEFAULT_MESSAGE);
            setCommentsSafely(config, "advancements", Arrays.asList(
                    "==================================================",
                    "ADVANCEMENT MESSAGES (\"Steve has made the advancement [Stone Age]\")",
                    "=================================================="));
            setCommentsSafely(config, "advancements.mode", Arrays.asList(
                    "VANILLA - Minecraft's own messages",
                    "HIDDEN  - no advancement messages in chat",
                    "CUSTOM  - the 'message' below"));
            setCommentsSafely(config, "advancements.show-to", Arrays.asList(
                    "CUSTOM mode: who sees the message - ALL or WORLD"));
            setCommentsSafely(config, "advancements.disabled-worlds", Arrays.asList(
                    "Worlds where advancement messages are never shown (any mode)"));
            setCommentsSafely(config, "advancements.message", Arrays.asList(
                    "CUSTOM mode. Placeholders: %name%, %advancement%, %world% + PlaceholderAPI"));
            getLogger().info("Added new config section: advancements (mode: VANILLA)");
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
        // MEMORY LEAK FIX: Add max-cache-size option (v1.15.1+)
        if (!config.isSet("chathead.max-cache-size")) {
            config.set("chathead.max-cache-size", 5000);
            getLogger().info("Added new config option: chathead.max-cache-size (default: 5000)");
            getLogger().info("  This prevents unbounded memory growth from player head caching");
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

        // Validate text format / MiniMessage configuration (added in v1.15.3)
        if (!config.isSet("text-format.mode")) {
            setDefaultTextFormat(config);
            getLogger().info("Added new config section: text-format (mode: LEGACY - nothing changes until you set it to MINIMESSAGE)");
            configChanged = true;
        }

        // Save config only once if any changes were made
        if (configChanged) {
            plugin.saveConfig();
            getLogger().info("Config updated with new options. Your custom settings have been preserved.");
        }
    }

    // Config comments need Spigot 1.18.1+, older servers just skip them
    private static void setCommentsSafely(FileConfiguration config, String path, List<String> comments) {
        try {
            config.setComments(path, comments);
        } catch (NoSuchMethodError ignored) {
            // Server older than 1.18.1 - the value is saved without a comment
        }
    }

    // Method to set the default text-format (MiniMessage) section for configs from older versions
    private void setDefaultTextFormat(FileConfiguration config) {
        config.set("text-format.mode", "LEGACY");
        config.set("text-format.convert-legacy-codes", true);
        config.set("text-format.player-tags.enabled", true);
        for (MiniMessageFormatter.TagGroup group : MiniMessageFormatter.TagGroup.values()) {
            String path = "text-format.player-tags.groups." + group.getKey();
            config.set(path + ".enabled", group.isEnabledByDefault());
            config.set(path + ".permission", group.getDefaultPermission());
        }

        // Short explanations. Config comments need Spigot 1.18.1+, older servers just skip them.
        try {
            config.setComments("text-format", Arrays.asList(
                    "==================================================",
                    "TEXT FORMAT (LEGACY / MINIMESSAGE)",
                    "How every text of this config is formatted (chat formats, messages, join/leave,",
                    "private messages, hover lines, custom commands...).",
                    "  LEGACY      - classic codes: &c, &l, &#RRGGBB, {#RRGGBB}, <#RRGGBB> and simple tags like <red>",
                    "  MINIMESSAGE - MiniMessage tags: <red>text</red>, <b>, <gradient:#ff0000:#0000ff>text</gradient>, <rainbow>",
                    "                Guide: https://docs.advntr.dev/minimessage/format.html",
                    "<hover> and <click> tags are not supported inside texts - use the 'hover' / 'click' sections.",
                    "=================================================="));
            config.setComments("text-format.mode", Collections.singletonList("LEGACY or MINIMESSAGE"));
            config.setComments("text-format.convert-legacy-codes", Arrays.asList(
                    "[MINIMESSAGE only] true = &-codes, &#RRGGBB and {#RRGGBB} keep working and can be mixed with tags.",
                    "'§' codes (e.g. prefixes from other plugins) are always converted."));
            config.setComments("text-format.player-tags", Arrays.asList(
                    "[MINIMESSAGE only] Tags players may use in THEIR OWN messages (chat, channels, /msg, /reply, /ac).",
                    "A tag the player is not allowed to use is shown as plain text.",
                    "  enabled    - false = nobody can use this group",
                    "  permission - needed to use the group, 'None' = everyone",
                    "Always blocked for players: hover, click, insert, font, key, lang, selector, score, nbt, shadow."));
        } catch (NoSuchMethodError ignored) {
            // Server older than 1.18.1 - values are still saved, just without comments.
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