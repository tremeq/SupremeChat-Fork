# ChatHead Configuration Guide

## 📝 Configuration Options

Add this section to your `config.yml`:

```yaml
chathead:
  # Enable or disable the ChatHead system
  # Set to false to completely disable player head rendering
  # Default: true
  enabled: true

  # Skin source to use for fetching player skins
  # Options:
  #   AUTO     - Automatically selects best source (Mojang for online, Minotar for offline) [RECOMMENDED]
  #   MOJANG   - Official Mojang API (online mode only, requires valid UUIDs)
  #   MINOTAR  - Minotar service (supports both online and offline mode)
  #   CRAFATAR - Crafatar service (online mode only, UUID-based)
  #   MCHEADS  - MC-Heads service (supports both online and offline mode)
  # Default: AUTO
  skin-source: AUTO

  # How long to cache player heads (in minutes)
  # Longer cache = better performance, but skins update less frequently
  # Shorter cache = worse performance, but skins update more quickly
  # Recommended: 5-10 minutes for most servers
  # Default: 5
  cache-time-minutes: 5

  # Whether to include the second skin layer (helmet/overlay) by default
  # true = show full head with hat/accessories
  # false = show only base skin layer
  # You can still override this per-call in code
  # Default: true
  use-overlay-by-default: true
```

## 🎯 Choosing the Right Skin Source

### For Offline Mode Servers (`online-mode=false`)

**RECOMMENDED: `AUTO` or `MINOTAR`**

```yaml
chathead:
  enabled: true
  skin-source: AUTO  # Will automatically use Minotar for offline mode
```

Why?
- Minotar supports **name-based** retrieval
- Works with cracked/offline UUIDs
- Reliable and fast

### For Online Mode Servers (`online-mode=true`)

**RECOMMENDED: `AUTO` or `MOJANG`**

```yaml
chathead:
  enabled: true
  skin-source: AUTO  # Will automatically use Mojang for online mode
```

Why?
- Mojang is the official source
- Most accurate for premium accounts
- Direct from Minecraft servers

## ⚙️ Performance Tuning

### High-Traffic Server (100+ players)

```yaml
chathead:
  enabled: true
  skin-source: AUTO
  cache-time-minutes: 10  # Longer cache for better performance
  use-overlay-by-default: true
```

Benefits:
- Reduced API calls to skin services
- Lower bandwidth usage
- Better server performance

### Small Server with Frequent Skin Changes

```yaml
chathead:
  enabled: true
  skin-source: AUTO
  cache-time-minutes: 2  # Shorter cache for fresher skins
  use-overlay-by-default: true
```

Benefits:
- Skins update more quickly
- Players see their new skins faster

## 🔧 Common Configurations

### Minimal Setup (Just Works™)

```yaml
chathead:
  enabled: true
  skin-source: AUTO
  cache-time-minutes: 5
  use-overlay-by-default: true
```

This is the default and works for 99% of servers!

### Offline Server Optimized

```yaml
chathead:
  enabled: true
  skin-source: MINOTAR  # Explicitly use Minotar for offline mode
  cache-time-minutes: 10  # Longer cache since offline skins change less
  use-overlay-by-default: true
```

### Performance-First Configuration

```yaml
chathead:
  enabled: true
  skin-source: MINOTAR  # Minotar is generally faster than Mojang
  cache-time-minutes: 15  # Maximum recommended cache time
  use-overlay-by-default: false  # Slightly faster without overlay
```

### Disabled Configuration

```yaml
chathead:
  enabled: false  # Completely disables ChatHead system
  # Other options are ignored when disabled
```

## 🚨 Troubleshooting

### Heads Not Showing

1. **Check if enabled:**
   ```yaml
   chathead:
     enabled: true  # Make sure this is true!
   ```

2. **Check your server mode:**
   - Offline server? Use `AUTO` or `MINOTAR`
   - Online server? Use `AUTO` or `MOJANG`

3. **Check logs:**
   Look for messages like:
   ```
   [SupremeChat] ChatHeadAPI initialized in OFFLINE mode
   [SupremeChat] ChatHeadAPI: Auto-selected MINOTAR source for offline mode
   ```

### Heads Showing Wrong Skins

**Cause:** Cache is too long, skins haven't refreshed

**Solution:** Reduce cache time:
```yaml
chathead:
  cache-time-minutes: 2  # Shorter cache
```

Or reload the plugin: `/supremechat reload`

### Performance Issues

**Cause:** Cache is too short, too many API calls

**Solution:** Increase cache time:
```yaml
chathead:
  cache-time-minutes: 10  # Longer cache
```

### Offline Mode - Heads Not Working

**Cause:** Using UUID-based source (Mojang/Crafatar)

**Solution:** Use name-based source:
```yaml
chathead:
  skin-source: MINOTAR  # or AUTO
```

## 📊 Configuration Impact

| Option | Performance Impact | Freshness | Best For |
|--------|-------------------|-----------|----------|
| `cache-time: 2` | 🔴 High load | ✅ Fresh | Small servers |
| `cache-time: 5` | 🟡 Moderate | 🟡 Okay | **Default** |
| `cache-time: 10` | 🟢 Low load | 🔴 Stale | Large servers |
| `overlay: true` | 🟡 Slight | N/A | **Recommended** |
| `overlay: false` | 🟢 Faster | N/A | Performance-first |

## 🎨 Example: Full config.yml Section

```yaml
# Supreme Chat Configuration
language: en
debug-mode: false

# ... other options ...

# ChatHead Player Head Rendering
chathead:
  # Enable/disable player heads in chat
  enabled: true

  # Skin source: AUTO, MOJANG, MINOTAR, CRAFATAR, MCHEADS
  # AUTO automatically chooses best source for your server mode
  skin-source: AUTO

  # Cache duration in minutes (recommended: 5-10)
  cache-time-minutes: 5

  # Include second skin layer (helmet/overlay) by default
  use-overlay-by-default: true
```

## 🔄 Reloading Configuration

After changing config.yml, reload the plugin:

```
/supremechat reload
```

This will:
1. Reload all config values
2. Restart ChatHeadAPI with new settings
3. Clear the head cache
4. Re-detect server mode

## ❓ FAQ

**Q: What does AUTO do exactly?**

A: AUTO checks if your server is in online or offline mode:
- Online mode (`online-mode=true`) → Uses Mojang
- Offline mode (`online-mode=false`) → Uses Minotar

**Q: Can I change sources without restarting?**

A: Yes! Just change the config and run `/supremechat reload`

**Q: Does cache-time affect all players or per-player?**

A: Per-player. Each player's head is cached individually for the specified time.

**Q: What happens when cache expires?**

A: The head is fetched again asynchronously. The old cached version is shown until the new one is ready.

**Q: Can I disable just the overlay?**

A: Yes, set `use-overlay-by-default: false`. You can still use overlay in specific API calls in code.

**Q: Does this work with custom skins in offline mode?**

A: Yes! As long as the player has uploaded their skin to a skin service (like SkinsRestorer or similar), Minotar will fetch it by name.

## 🎯 Recommended Settings by Server Type

### Cracked/Offline Server
```yaml
chathead:
  enabled: true
  skin-source: AUTO  # Will use Minotar
  cache-time-minutes: 10
  use-overlay-by-default: true
```

### Premium/Online Server
```yaml
chathead:
  enabled: true
  skin-source: AUTO  # Will use Mojang
  cache-time-minutes: 5
  use-overlay-by-default: true
```

### Testing/Development
```yaml
chathead:
  enabled: true
  skin-source: MINOTAR  # Fast and reliable
  cache-time-minutes: 1  # Quick updates for testing
  use-overlay-by-default: true
```

### Production High-Performance
```yaml
chathead:
  enabled: true
  skin-source: AUTO
  cache-time-minutes: 15
  use-overlay-by-default: true
```

---

**Need more help?** Check `CHATHEAD_INTEGRATION_GUIDE.md` for code examples!
