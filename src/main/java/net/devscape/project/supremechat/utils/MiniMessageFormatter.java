package net.devscape.project.supremechat.utils;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MiniMessage support, controlled by the 'text-format' section of config.yml.
 * <p>
 * Every reference to the Adventure/MiniMessage API lives in this class. The library is
 * shaded and relocated into the plugin jar, so it behaves the same on Spigot and Paper.
 * <p>
 * MiniMessage is only used to turn a text into a legacy (§) string, which the rest of the
 * plugin already knows how to send. Because of that, interactive tags (hover, click, insert)
 * cannot survive the conversion and are dropped - hover/click have their own config sections.
 * <p>
 * Security: text typed by players is never parsed with full MiniMessage. Before it is put
 * into a format, every tag the player is not allowed to use is escaped, so it shows up as
 * plain text instead of being formatted.
 */
public final class MiniMessageFormatter {

    /**
     * Tag groups that players can be allowed to use in their own messages
     * (text-format.player-tags.groups.&lt;key&gt; in config.yml).
     */
    public enum TagGroup {
        COLOR("color", true, StandardTags.color()),
        DECORATION("decoration", true, TagResolver.resolver(
                StandardTags.decorations(TextDecoration.BOLD),
                StandardTags.decorations(TextDecoration.ITALIC),
                StandardTags.decorations(TextDecoration.UNDERLINED),
                StandardTags.decorations(TextDecoration.STRIKETHROUGH))),
        OBFUSCATED("obfuscated", true, StandardTags.decorations(TextDecoration.OBFUSCATED)),
        GRADIENT("gradient", true, StandardTags.gradient()),
        RAINBOW("rainbow", true, StandardTags.rainbow()),
        TRANSITION("transition", true, StandardTags.transition()),
        PRIDE("pride", true, StandardTags.pride()),
        // Off by default: <reset> can break the look of the chat format,
        // <newline> can be abused to fake messages from other players.
        RESET("reset", false, StandardTags.reset()),
        NEWLINE("newline", false, StandardTags.newline());

        private final String key;
        private final boolean enabledByDefault;
        private final TagResolver tags;

        TagGroup(String key, boolean enabledByDefault, TagResolver tags) {
            this.key = key;
            this.enabledByDefault = enabledByDefault;
            this.tags = tags;
        }

        public String getKey() {
            return key;
        }

        public boolean isEnabledByDefault() {
            return enabledByDefault;
        }

        public String getDefaultPermission() {
            return "supremechat.tags." + key;
        }
    }

    private static final MiniMessage PARSER = MiniMessage.miniMessage();

    // A tag the parser can only read one way: no quotes, no backslashes, no whitespace and
    // no nested '<' or '>' in its arguments. Only tags of this shape are ever left unescaped
    // in player text - everything else (including hover, click, insert, font, key, lang,
    // selector, score, nbt and shadow, which belong to no group) is shown as plain text.
    private static final Pattern SIMPLE_TAG = Pattern.compile("<(/?)(!?[a-zA-Z0-9_#-]+)((?::[^<>'\"\\\\\\s]*)*)>");

    private static final LegacyComponentSerializer SERIALIZER_HEX = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    // Pre-1.16 servers cannot show hex colors - they get the nearest named color instead.
    private static final LegacyComponentSerializer SERIALIZER_NO_HEX = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .build();

    // ---- Legacy code -> MiniMessage conversion ----
    // In legacy formatting a color code also clears bold/italic/etc., so every converted
    // color first switches all decorations off. Negations are used instead of <reset>,
    // because <reset> would also close tags from the config (e.g. </gradient> would then
    // show up as text).
    private static final String RESET_DECORATIONS = "<!b><!i><!u><!st><!obf>";

    private static final Pattern SECTION_HEX = Pattern.compile("§[xX]((?:§[0-9a-fA-F]){6})");
    private static final Pattern SECTION_CODE = Pattern.compile("§([0-9a-fA-Fk-oK-OrR])");
    private static final Pattern AMP_REPEATED_HEX = Pattern.compile("&[xX]((?:&[0-9a-fA-F]){6})");
    private static final Pattern BRACE_HEX = Pattern.compile("\\{#([0-9a-fA-F]{6})}");
    private static final Pattern ANGLE_AMP_HEX = Pattern.compile("<#&([0-9a-fA-F]{6})>");
    private static final Pattern AMP_HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    // A lone '#RRGGBB' (matched the same way as in LEGACY mode) - but not inside MiniMessage
    // tags like <#ff0000>, </#ff0000>, <color:#ff0000> or <gradient:#ff0000:#0000ff>.
    private static final Pattern BARE_HEX = Pattern.compile("(?<![</:])#([0-9a-fA-F]{6})");
    private static final Pattern AMP_CODE = Pattern.compile("&([0-9a-fA-Fk-oK-OrR])");

    // ---- Settings, re-read on every reload ----
    private static volatile JavaPlugin plugin;
    private static volatile boolean miniMessageMode = false;
    private static volatile boolean convertLegacyCodes = true;
    private static volatile boolean playerTagsEnabled = true;
    private static volatile boolean[] groupEnabled = defaultGroupEnabled();
    private static volatile String[] groupPermission = defaultGroupPermissions();
    private static volatile boolean hexSupported = true;
    private static volatile boolean parseErrorLogged = false;

    private MiniMessageFormatter() {
    }

    /**
     * (Re)loads the 'text-format' settings from config.yml.
     */
    public static void reload(JavaPlugin owner) {
        plugin = owner;
        FileConfiguration config = owner.getConfig();

        String mode = config.getString("text-format.mode", "LEGACY").trim();
        if (mode.equalsIgnoreCase("MINIMESSAGE")) {
            miniMessageMode = true;
        } else {
            if (!mode.equalsIgnoreCase("LEGACY")) {
                owner.getLogger().warning("Unknown text-format.mode '" + mode + "' - using LEGACY. Valid values: LEGACY, MINIMESSAGE");
            }
            miniMessageMode = false;
        }

        convertLegacyCodes = config.getBoolean("text-format.convert-legacy-codes", true);
        playerTagsEnabled = config.getBoolean("text-format.player-tags.enabled", true);

        TagGroup[] groups = TagGroup.values();
        boolean[] enabled = new boolean[groups.length];
        String[] permissions = new String[groups.length];
        for (TagGroup group : groups) {
            String path = "text-format.player-tags.groups." + group.getKey();
            enabled[group.ordinal()] = config.getBoolean(path + ".enabled", group.isEnabledByDefault());

            String permission = config.getString(path + ".permission", group.getDefaultPermission());
            if (permission == null || permission.trim().isEmpty()) {
                // An empty value must never mean "everyone" - fall back to the default node.
                permission = group.getDefaultPermission();
            }
            permissions[group.ordinal()] = permission.trim();
        }
        groupEnabled = enabled;
        groupPermission = permissions;

        hexSupported = !Message.isVersionLessThan("1.16");
        parseErrorLogged = false;

        owner.getLogger().info("Text format: " + (miniMessageMode ? "MINIMESSAGE" : "LEGACY"));
    }

    /**
     * @return true if texts are formatted with MiniMessage (text-format.mode: MINIMESSAGE).
     */
    public static boolean isMiniMessageMode() {
        return miniMessageMode;
    }

    /**
     * Formats a text with MiniMessage and returns it as a legacy (§) string.
     *
     * @return the formatted text, or null if it could not be parsed (the caller then
     *         falls back to LEGACY formatting so the message is never lost)
     */
    public static String toLegacy(String text) {
        if (text == null) {
            return null;
        }
        try {
            String input = convertLegacyCodes(text);
            LegacyComponentSerializer serializer = hexSupported ? SERIALIZER_HEX : SERIALIZER_NO_HEX;
            return serializer.serialize(PARSER.deserialize(input));
        } catch (Exception e) {
            logParseError(text, e);
            return null;
        }
    }

    /**
     * Prepares a text typed by a player (chat, channels, private messages, admin chat) to be
     * inserted into a format. Tags the sender has no permission for are escaped and show up
     * as plain text. Does nothing in LEGACY mode.
     */
    public static String escapePlayerText(CommandSender sender, String text) {
        if (!miniMessageMode || text == null || text.isEmpty()) {
            return text;
        }
        return escape(text, allowedGroups(sender));
    }

    /**
     * Prepares untrusted text that must never be formatted as tags (typed player names,
     * commands in command spy, item names). Does nothing in LEGACY mode.
     */
    public static String escapeUntrustedText(String text) {
        if (!miniMessageMode || text == null || text.isEmpty()) {
            return text;
        }
        return escape(text, 0);
    }

    // ==================================================
    // Internals
    // ==================================================

    /**
     * Escapes every '&lt;' of the text, except the ones that start a simple tag from an allowed
     * group. MiniMessage's own escapeTags() is not used on purpose: with unclosed quotes it
     * reads a text differently than the parser does, which let blocked tags through.
     */
    private static String escape(String text, int allowedMask) {
        // Double every backslash first. Otherwise a message ending with '\' would escape the
        // next tag of the format (e.g. "</gray>") and break it.
        String input = text.replace("\\", "\\\\");
        StringBuilder out = new StringBuilder(input.length() + 16);
        Deque<String> openTags = new ArrayDeque<>();
        Matcher tag = SIMPLE_TAG.matcher(input);

        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c != '<') {
                out.append(c);
                i++;
                continue;
            }

            // '<#&RRGGBB>' is a legacy code, not a MiniMessage tag. Like '&' codes it is
            // controlled by chat-color-permission, so it is left for the legacy conversion.
            if (convertLegacyCodes && input.startsWith("<#&", i)) {
                Matcher legacyHex = ANGLE_AMP_HEX.matcher(input);
                legacyHex.region(i, input.length());
                if (legacyHex.lookingAt()) {
                    out.append(legacyHex.group());
                    i = legacyHex.end();
                    continue;
                }
            }

            tag.region(i, input.length());
            if (tag.lookingAt()) {
                boolean closing = !tag.group(1).isEmpty();
                String name = tag.group(2).toLowerCase(Locale.ROOT);
                if (isAllowed(name, allowedMask)) {
                    if (!closing) {
                        openTags.push(name);
                        out.append(tag.group());
                        i = tag.end();
                        continue;
                    }
                    // A player may only close tags he opened himself - never a tag of the
                    // format around his message (e.g. the "<gray>" before %message%).
                    if (openTags.contains(name)) {
                        while (!openTags.pop().equals(name)) {
                            // drop tags opened after it - MiniMessage closes them too
                        }
                        out.append(tag.group());
                        i = tag.end();
                        continue;
                    }
                }
            }

            out.append("\\<");
            i++;
        }
        return out.toString();
    }

    private static boolean isAllowed(String tagName, int allowedMask) {
        if (allowedMask == 0) {
            return false;
        }
        for (TagGroup group : TagGroup.values()) {
            if ((allowedMask & (1 << group.ordinal())) != 0 && group.tags.has(tagName)) {
                return true;
            }
        }
        return false;
    }

    private static int allowedGroups(CommandSender sender) {
        if (!playerTagsEnabled || sender == null) {
            return 0;
        }
        boolean[] enabled = groupEnabled;
        String[] permissions = groupPermission;
        int mask = 0;
        for (TagGroup group : TagGroup.values()) {
            int i = group.ordinal();
            if (!enabled[i]) {
                continue;
            }
            String permission = permissions[i];
            if (permission.equalsIgnoreCase("None") || sender.hasPermission(permission)) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    /**
     * Turns legacy codes into MiniMessage tags. MiniMessage refuses texts containing '§'
     * codes, so those (e.g. prefixes coming from other plugins) are always converted.
     * '&' based codes are converted only if text-format.convert-legacy-codes is true.
     */
    static String convertLegacyCodes(String text) {
        String out = replace(SECTION_HEX, text, m -> hexTag(m.group(1)));
        out = replace(SECTION_CODE, out, m -> codeTag(m.group(1).charAt(0)));

        if (convertLegacyCodes) {
            out = replace(AMP_REPEATED_HEX, out, m -> hexTag(m.group(1)));
            out = replace(BRACE_HEX, out, m -> hexTag(m.group(1)));
            out = replace(ANGLE_AMP_HEX, out, m -> hexTag(m.group(1)));
            out = replace(AMP_HEX, out, m -> hexTag(m.group(1)));
            out = replace(BARE_HEX, out, m -> hexTag(m.group(1)));
            out = replace(AMP_CODE, out, m -> codeTag(m.group(1).charAt(0)));
        }
        return out;
    }

    private static String hexTag(String digits) {
        return RESET_DECORATIONS + "<#" + digits.replaceAll("[§&]", "").toLowerCase() + ">";
    }

    private static String codeTag(char code) {
        switch (Character.toLowerCase(code)) {
            case '0': return RESET_DECORATIONS + "<black>";
            case '1': return RESET_DECORATIONS + "<dark_blue>";
            case '2': return RESET_DECORATIONS + "<dark_green>";
            case '3': return RESET_DECORATIONS + "<dark_aqua>";
            case '4': return RESET_DECORATIONS + "<dark_red>";
            case '5': return RESET_DECORATIONS + "<dark_purple>";
            case '6': return RESET_DECORATIONS + "<gold>";
            case '7': return RESET_DECORATIONS + "<gray>";
            case '8': return RESET_DECORATIONS + "<dark_gray>";
            case '9': return RESET_DECORATIONS + "<blue>";
            case 'a': return RESET_DECORATIONS + "<green>";
            case 'b': return RESET_DECORATIONS + "<aqua>";
            case 'c': return RESET_DECORATIONS + "<red>";
            case 'd': return RESET_DECORATIONS + "<light_purple>";
            case 'e': return RESET_DECORATIONS + "<yellow>";
            case 'f': return RESET_DECORATIONS + "<white>";
            case 'k': return "<obf>";
            case 'l': return "<b>";
            case 'm': return "<st>";
            case 'n': return "<u>";
            case 'o': return "<i>";
            // Legacy reset: back to the default look without closing the config's own tags.
            case 'r': return RESET_DECORATIONS + "<white>";
            default: return "";
        }
    }

    private static String replace(Pattern pattern, String input, Function<Matcher, String> replacement) {
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        StringBuffer result = new StringBuffer();
        do {
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.apply(matcher)));
        } while (matcher.find());
        matcher.appendTail(result);
        return result.toString();
    }

    private static void logParseError(String text, Exception e) {
        JavaPlugin owner = plugin;
        if (owner == null) {
            return;
        }
        String reason = String.valueOf(e.getMessage()).split("\n")[0];
        if (!parseErrorLogged) {
            parseErrorLogged = true;
            owner.getLogger().warning("Could not format a text with MiniMessage, LEGACY formatting was used instead: " + reason);
            owner.getLogger().warning("Text: " + text);
            owner.getLogger().warning("Further errors of this kind are only shown with debug-mode: true");
        } else if (owner.getConfig().getBoolean("debug-mode", false)) {
            owner.getLogger().warning("[DEBUG] MiniMessage error: " + reason + " | Text: " + text);
        }
    }

    private static boolean[] defaultGroupEnabled() {
        TagGroup[] groups = TagGroup.values();
        boolean[] enabled = new boolean[groups.length];
        for (TagGroup group : groups) {
            enabled[group.ordinal()] = group.isEnabledByDefault();
        }
        return enabled;
    }

    private static String[] defaultGroupPermissions() {
        TagGroup[] groups = TagGroup.values();
        String[] permissions = new String[groups.length];
        for (TagGroup group : groups) {
            permissions[group.ordinal()] = group.getDefaultPermission();
        }
        return permissions;
    }
}
