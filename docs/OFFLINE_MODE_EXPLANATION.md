# Understanding Offline Mode and UUID Issues

## 🤔 The Problem

When a Minecraft server runs in **offline mode** (`online-mode=false` in `server.properties`), it doesn't authenticate players with Mojang's servers. This creates a critical issue for skin retrieval:

### How UUIDs Work

#### Online Mode (Normal)
```
Player "Notch" connects
↓
Server asks Mojang: "Is this really Notch?"
↓
Mojang responds: "Yes, here's his UUID: 069a79f4-44e9-4726-a5be-fca90e38aaf5"
↓
Server can use this UUID to fetch skin from Mojang
```

#### Offline Mode (Cracked)
```
Player "Notch" connects
↓
Server generates UUID locally: UUID.nameUUIDFromBytes(...)
↓
Result: aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee (fake UUID)
↓
❌ Mojang has NO RECORD of this UUID
↓
❌ Cannot fetch skin using this UUID
```

### The Core Issue

```java
// In offline mode:
OfflinePlayer player = Bukkit.getOfflinePlayer("Notch");
UUID uuid = player.getUniqueId(); // Returns: fake generated UUID

// Try to fetch skin from Mojang:
String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
// ❌ FAILS: Mojang doesn't recognize this UUID
```

## ✅ The Solution

Instead of using UUID, use the **player's NAME** directly with services that support name-based retrieval:

```java
// ❌ WRONG for offline mode (uses fake UUID)
BaseComponent[] head = api.getHead(player.getUniqueId());

// ✅ CORRECT for offline mode (uses actual name)
BaseComponent[] head = api.getHead(player.getName());
```

## 🌐 Skin Services Comparison

### Services That Work in Offline Mode

#### 1. Minotar (RECOMMENDED)
```
URL Format: https://minotar.net/avatar/Notch/8.png
Support: ✅ Name-based retrieval
Perfect for offline mode!
```

#### 2. MC-Heads
```
URL Format: https://mc-heads.net/avatar/Notch/8
Support: ✅ Name-based retrieval
Alternative to Minotar
```

### Services That DON'T Work in Offline Mode

#### 1. Mojang
```
URL Format: https://sessionserver.mojang.com/session/minecraft/profile/{UUID}
Support: ❌ UUID only
Requires valid Mojang UUID
```

#### 2. Crafatar
```
URL Format: https://crafatar.com/avatars/{UUID}?size=8
Support: ❌ UUID only
Cannot handle fake UUIDs
```

## 🔧 How Our Solution Works

### 1. Automatic Mode Detection

```java
// ChatHeadAPI detects server mode on initialization
boolean isOnlineMode = Bukkit.getServer().getOnlineMode();

if (isOnlineMode) {
    // Use Mojang (UUID-based)
    defaultSource = new MojangSource();
} else {
    // Use Minotar (name-based)
    defaultSource = new MinotarSource(false); // false = use name
}
```

### 2. Smart Method Selection

```java
public BaseComponent[] getHeadSmart(OfflinePlayer player) {
    if (isOnlineMode) {
        // Online: use UUID (reliable)
        return headCache.getCachedHead(player, overlay, defaultSource);
    } else {
        // Offline: use player name (bypasses UUID issue)
        String playerName = player.getName();
        return headCache.getCachedHeadByName(playerName, overlay, defaultSource);
    }
}
```

### 3. Name-Based Retrieval

```java
// MinotarSource implementation for offline mode
@Override
public BaseComponent[] getHeadByName(String playerName, boolean overlay) {
    String baseUrl = "https://minotar.net/";
    String endpoint = overlay ? "helm" : "avatar";

    // Use NAME directly, not UUID!
    String imageUrl = baseUrl + endpoint + "/" + playerName + "/8.png";

    BufferedImage skinImage = ImageIO.read(new URL(imageUrl));
    // Process image...
    return toBaseComponent(colors);
}
```

## 📊 Comparison Table

| Aspect | Online Mode | Offline Mode |
|--------|-------------|--------------|
| **UUID** | Real Mojang UUID | Fake local UUID |
| **Authentication** | Via Mojang servers | None |
| **Best API method** | `getHead(UUID)` | `getHead(String name)` |
| **Best skin source** | Mojang | Minotar |
| **UUID reliability** | ✅ 100% reliable | ❌ Unreliable |
| **Name reliability** | ✅ 100% reliable | ✅ 100% reliable |

## 🎯 Best Practices

### DO ✅

```java
// Use smart method (auto-detects mode)
api.getHeadSmart(player);

// Or explicitly use name in offline mode
if (!api.isOnlineMode()) {
    api.getHead(player.getName());
}

// Configure AUTO mode
config.set("chathead.skin-source", "AUTO");
```

### DON'T ❌

```java
// Don't force UUID-based sources in offline mode
api.getHead(player.getUniqueId()); // Fails in offline mode!

// Don't use Mojang/Crafatar for offline servers
defaultSource = new MojangSource(); // Won't work!

// Don't ignore server mode
// Always check: api.isOnlineMode()
```

## 🔍 Debugging

### Check Server Mode

```java
boolean online = Bukkit.getServer().getOnlineMode();
player.sendMessage("Server is in " + (online ? "ONLINE" : "OFFLINE") + " mode");
```

### Test UUID vs Name

```java
Player player = ...;
UUID uuid = player.getUniqueId();
String name = player.getName();

player.sendMessage("UUID: " + uuid); // In offline: fake UUID
player.sendMessage("Name: " + name); // Always correct
```

### Verify Skin Source

```java
ChatHeadAPI api = ChatHeadAPI.getInstance();
SkinSource source = api.getDefaultSource();

player.sendMessage("Using: " + source.getSkinSource());
player.sendMessage("Supports names: " + source.hasUsernameSupport());
player.sendMessage("Offline compatible: " +
    source.getSkinSource().isOfflineModeCompatible());
```

## 🚀 Migration Guide

### From External ChatHeadFont to Integrated API

#### Old Way (External Plugin)
```java
// Required external ChatHeadFont plugin
ChatHeadAPI externalAPI = ChatHeadAPI.getInstance();
BaseComponent[] head = externalAPI.getHead(player.getUniqueId());
// ❌ Failed in offline mode
```

#### New Way (Integrated)
```java
// Built-in to SupremeChat, auto-detects mode
import net.devscape.project.supremechat.chathead.ChatHeadAPI;

ChatHeadAPI api = ChatHeadAPI.getInstance();
BaseComponent[] head = api.getHeadSmart(player);
// ✅ Works in both online and offline mode!
```

## 💡 Key Takeaways

1. **Offline mode UUIDs are fake** - don't rely on them for skin retrieval
2. **Player names are always reliable** - use them in offline mode
3. **Minotar supports name-based retrieval** - best choice for offline servers
4. **Use `getHeadSmart()`** - automatically handles both modes
5. **Configure `skin-source: AUTO`** - lets the API choose the best source

## 📞 Still Confused?

Remember this simple rule:

> **In offline mode, always use player.getName() instead of player.getUniqueId()**

The ChatHeadAPI handles this automatically when you use `getHeadSmart()` or `getHead(String name)`!

---

**Problem Solved!** 🎉
