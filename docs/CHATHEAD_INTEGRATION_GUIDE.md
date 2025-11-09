# ChatHeadAPI Integration Guide - Offline Mode Support

## 🎯 Overview

This guide explains the integrated ChatHeadAPI with full offline mode support. The API is now **built directly into SupremeChat** and automatically handles both online and offline mode servers.

## ✨ Key Features

- ✅ **Automatic offline mode detection** - no manual configuration needed!
- ✅ **Name-based skin retrieval** - works with cracked UUIDs
- ✅ **Multiple skin sources** - Mojang, Minotar, Crafatar, MC-Heads
- ✅ **Smart caching system** - 5-minute cache by default
- ✅ **Zero external dependencies** - no separate plugin installation required

## 🔧 Configuration

Add to your `config.yml`:

```yaml
chathead:
  # AUTO = automatically select best source based on server mode
  # Options: AUTO, MOJANG, MINOTAR, CRAFATAR, MCHEADS
  skin-source: AUTO

  # Cache duration in minutes
  cache-time-minutes: 5

  # Enable/disable the ChatHead system
  enabled: true
```

### Skin Source Recommendations

| Server Mode | Recommended Source | Why |
|-------------|-------------------|-----|
| **Offline** | `AUTO` or `MINOTAR` | Supports name-based retrieval |
| **Online** | `AUTO` or `MOJANG` | Most reliable for premium accounts |

## 📝 Usage Examples

### 1. Simple Usage (RECOMMENDED)

The `getHeadSmart()` method automatically uses the best approach:

```java
import net.devscape.project.supremechat.chathead.ChatHeadAPI;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

// Get the API instance
ChatHeadAPI api = ChatHeadAPI.getInstance();

// Smart method - auto-detects online/offline mode
BaseComponent[] head = api.getHeadSmart(player);

// Create message with head
TextComponent message = new TextComponent("Welcome ");
message.addExtra(head);
message.addExtra(new TextComponent(" " + player.getName() + "!"));

// Send to player
player.spigot().sendMessage(message);
```

### 2. Offline Mode - Using Player Name

For offline mode servers, use the player's **name** instead of UUID:

```java
// This is the KEY method for offline mode!
String playerName = "Notch";
BaseComponent[] head = api.getHead(playerName);

TextComponent message = new TextComponent("");
message.addExtra(head);
message.addExtra(" " + playerName);

player.spigot().sendMessage(message);
```

### 3. Multiple Heads in One Message

```java
BaseComponent[] head1 = api.getHead("Steve");
BaseComponent[] head2 = api.getHead("Alex");

TextComponent message = new TextComponent("Chat between ");
message.addExtra(head1);
message.addExtra(" Steve and ");
message.addExtra(head2);
message.addExtra(" Alex");

player.spigot().sendMessage(message);
```

### 4. Custom Skin Source

```java
import net.devscape.project.supremechat.chathead.sources.MinotarSource;

// Force MinotarSource for offline mode compatibility
SkinSource minotarSource = new MinotarSource(false); // false = use name, not UUID
BaseComponent[] head = api.getHead("PlayerName", true, minotarSource);
```

### 5. Without Overlay (No Helmet Layer)

```java
// false = no overlay
BaseComponent[] head = api.getHead("PlayerName", false);
```

## 🛠️ API Methods Reference

### For Online Mode (UUID-based)

```java
// Using UUID
api.getHead(uuid)
api.getHead(uuid, overlay)
api.getHead(uuid, overlay, skinSource)

// Using OfflinePlayer
api.getHead(player)
api.getHead(player, overlay)
api.getHead(player, overlay, skinSource)
```

### For Offline Mode (Name-based) ⭐ NEW

```java
// Using player name (works in offline mode!)
api.getHead(playerName)
api.getHead(playerName, overlay)
api.getHead(playerName, overlay, skinSource)
```

### Smart Method (Auto-detects mode) ⭐ RECOMMENDED

```java
// Automatically uses UUID (online) or name (offline)
api.getHeadSmart(player)
api.getHeadSmart(player, overlay)
```

### String Conversion

```java
// Convert to legacy text format
String headString = api.getHeadAsString(playerName);
String headString = api.getHeadAsString(player);
```

### Utility Methods

```java
// Check if server is in online mode
boolean isOnline = api.isOnlineMode();

// Get current default skin source
SkinSource source = api.getDefaultSource();
```

## 🔍 How It Works

### Online Mode
1. Server has `online-mode=true`
2. UUIDs are valid Mojang UUIDs
3. API uses UUID-based retrieval from Mojang or other sources

### Offline Mode
1. Server has `online-mode=false`
2. UUIDs are locally generated (cracked)
3. API uses **player name** to fetch skins from external services
4. Minotar and MC-Heads support name-based retrieval

## 🚨 Troubleshooting

### Problem: Heads not loading in offline mode

**Solution**: Ensure you're using `AUTO` or `MINOTAR` as skin source:
```yaml
chathead:
  skin-source: AUTO  # or MINOTAR
```

### Problem: "SkinSource does not support username-based retrieval"

**Cause**: You're using Crafatar or Mojang source in offline mode.

**Solution**: Use MinotarSource:
```java
SkinSource source = new MinotarSource(false); // Use name, not UUID
BaseComponent[] head = api.getHead(playerName, true, source);
```

### Problem: Heads showing as Steve/Alex in offline mode

**Cause**: Trying to use UUID retrieval in offline mode.

**Solution**: Use name-based methods:
```java
// ❌ Wrong for offline mode
api.getHead(player.getUniqueId())

// ✅ Correct for offline mode
api.getHead(player.getName())
```

## 📦 Skin Source Comparison

| Source | Offline Support | URL Format | Notes |
|--------|----------------|------------|-------|
| **Mojang** | ❌ No | UUID only | Best for online mode |
| **Crafatar** | ❌ No | UUID only | Fast, reliable for online mode |
| **Minotar** | ✅ Yes | Name or UUID | **Best for offline mode** |
| **MC-Heads** | ✅ Yes | Name or UUID | Alternative for offline mode |

## 🎨 Example: Custom Join Message with Head

```java
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    ChatHeadAPI api = ChatHeadAPI.getInstance();

    // Get player head (works in both online and offline mode)
    BaseComponent[] head = api.getHeadSmart(player);

    // Create fancy join message
    TextComponent message = new TextComponent("§a§l+ §r");
    message.addExtra(head);
    message.addExtra(new TextComponent(" §7" + player.getName() + " §ajoined the game!"));

    // Broadcast to all players
    for (Player online : Bukkit.getOnlinePlayers()) {
        online.spigot().sendMessage(message);
    }

    // Hide default join message
    event.setJoinMessage(null);
}
```

## 🔗 Integration with Existing Features

### Use in Chat Formatting

```java
// In your chat listener
String format = config.getString("chat-format");
// Format: "&7{HEAD} &a{PLAYER} &8» &f{MESSAGE}"

BaseComponent[] head = ChatHeadAPI.getInstance().getHeadSmart(player);
String headString = TextComponent.toLegacyText(head);

// Replace placeholders
format = format.replace("{HEAD}", headString);
format = format.replace("{PLAYER}", player.getName());
```

## 🎯 Best Practices

1. **Always use `getHeadSmart()`** for automatic mode detection
2. **Cache results** when displaying multiple times
3. **Test in both modes** if you support both online and offline
4. **Use async methods** for bulk operations
5. **Set `skin-source: AUTO`** in config for automatic optimization

## 📊 Performance Tips

- Heads are cached for 5 minutes by default
- Requests are asynchronous - won't block the main thread
- First request may be slow (fetching from external service)
- Subsequent requests use cache (very fast)

## 🔐 Security Notes

- All external requests use HTTPS
- No authentication credentials stored
- Rate limiting handled by external services
- Cached data cleared on plugin disable

## 📞 Support

If heads are not displaying:
1. Check your config.yml settings
2. Verify server is using Java 8 or higher
3. Check console for errors
4. Ensure network allows HTTPS requests to skin services

---

**Created for SupremeChat** | Offline mode support integrated
