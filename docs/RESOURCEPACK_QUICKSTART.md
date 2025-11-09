# 🚀 ChatHead Resource Pack - Quick Start

## ⚡ TL;DR - Works OUT OF THE BOX! ✅

### **GOOD NEWS: It works immediately with zero configuration!**

The plugin is pre-configured with a working resource pack URL from ChatHeadFont.
**Players will automatically receive the pack when joining - no setup required!**

### What You Need to Know

**Without this resource pack, player heads won't display in chat!**

The resource pack defines custom Unicode characters that render as pixels. Without it, players will see garbled text like `󰀁󰀂󰀃` instead of heads.

**The good news:** The plugin sends this pack automatically by default!

---

## 📦 Files in This Repository

```
chathead-resourcepack/          ← Unpacked (for development)
chathead-resourcepack.zip       ← Packed (3.1 KB, for distribution)
RESOURCEPACK_SETUP_GUIDE.md     ← Full setup guide
server.properties.resourcepack-example  ← Configuration example
```

**SHA1 Hash**: `401b402cefdb05776cb1bb06db0afc0ed566e20d`

---

## 🎯 Quick Setup - AUTOMATIC Distribution (Recommended)

**SupremeChat now automatically sends the resource pack to players!**

### Step 1: Host the Pack

**GitHub Releases (Easiest)**:
1. Go to your repo → Releases → Create new release
2. Upload `chathead-resourcepack.zip`
3. Copy the download URL

**Example URL**:
```
https://github.com/YOUR_USERNAME/YOUR_REPO/releases/download/v1.0/chathead-resourcepack.zip
```

### Step 2: Configure config.yml

Edit your `plugins/SupremeChat/config.yml`:

```yaml
chathead:
  enabled: true
  # ... other chathead settings ...

  resourcepack:
    auto-send: true  # ✅ Enable automatic sending
    url: "https://github.com/YOUR_USERNAME/YOUR_REPO/releases/download/v1.0/chathead-resourcepack.zip"
    sha1: "401b402cefdb05776cb1bb06db0afc0ed566e20d"
    prompt: "§6§lSupremeChat §aChatHead Pack\n§7Required for player heads in chat\n§e§lHighly Recommended!"
    force: false  # false = optional, true = required
```

### Step 3: Reload Plugin

```bash
/supremechat reload
# Or restart the server
```

**Done!** Players will be prompted to download when joining.

---

## 🎯 Alternative: server.properties Method

If you prefer using `server.properties` instead:

```properties
resource-pack=https://github.com/YOUR_USERNAME/YOUR_REPO/releases/download/v1.0/chathead-resourcepack.zip
resource-pack-sha1=401b402cefdb05776cb1bb06db0afc0ed566e20d
resource-pack-prompt=§6§lSupremeChat §aChatHead Pack\n§7Required for player heads in chat
require-resource-pack=false
```

**Note**: server.properties takes priority. If set there, config.yml won't override it.

---

## 🧪 Quick Test (Manual Installation)

For testing without hosting:

1. Copy `chathead-resourcepack.zip` to your Minecraft folder:
   - Windows: `%APPDATA%\.minecraft\resourcepacks\`
   - Mac: `~/Library/Application Support/minecraft/resourcepacks/`
   - Linux: `~/.minecraft/resourcepacks/`

2. In Minecraft: Options → Resource Packs → Move to "Selected"

3. Join server and test chat

---

## ❓ Troubleshooting

| Problem | Quick Fix |
|---------|-----------|
| See Unicode chars instead of heads | Pack not loaded - check it's in "Selected" |
| "Failed to download" error | URL must be HTTPS and publicly accessible |
| "SHA1 verification failed" | Copy the exact hash: `401b402cefdb05776cb1bb06db0afc0ed566e20d` |
| Pack loads but no heads show | Code not implemented yet - see `CHATHEAD_IMPLEMENTATION_EXAMPLE.md` |

---

## 📚 Full Documentation

- **Full Setup Guide**: `RESOURCEPACK_SETUP_GUIDE.md` (all hosting options, troubleshooting)
- **API Integration**: `CHATHEAD_INTEGRATION_GUIDE.md` (how to use ChatHeadAPI)
- **Implementation Example**: `CHATHEAD_IMPLEMENTATION_EXAMPLE.md` (add heads to chat)
- **Config Example**: `server.properties.resourcepack-example` (ready to copy)

---

## 🔍 What's Inside the Pack?

```
chathead-resourcepack/
├── pack.mcmeta                  # Pack info (format 22)
└── assets/
    ├── chathead/
    │   ├── font/playerhead.json # Defines \uF001-\uF008, \uF101-\uF102
    │   └── textures/
    │       └── pixel1-8.png     # 1x1 white pixels
    └── minecraft/font/
        └── default.json         # Links to playerhead
```

**How it works**:
- Characters \uF001-\uF008 = 8 columns of pixels
- Character \uF101 = newline (move to next row)
- Character \uF102 = negative space (move back)
- Each char is colored by Minecraft chat codes
- 8 columns × 8 rows = 64 pixels = player head

---

## 🎨 Example: What Players See

**Without pack**:
```
󰀁󰀂󰀃󰀄󰀅󰀆󰀇󰀈 PlayerName: Hello!
```

**With pack**:
```
[Player Head Image] PlayerName: Hello!
```

---

## ⚠️ Important Notes

1. **HTTPS Required**: Resource pack URL must use HTTPS, not HTTP
2. **SHA1 Must Match**: If you modify the pack, regenerate the hash:
   ```bash
   sha1sum chathead-resourcepack.zip
   ```
3. **Pack Format**: Current pack uses format 22 (MC 1.20.2-1.20.4)
   - For older versions, edit `pack.mcmeta` and change `pack_format`
4. **File Size**: Only 3.1 KB - won't impact performance

---

## 🔗 Hosting Options Comparison

| Method | Cost | Speed | Ease | Recommended |
|--------|------|-------|------|-------------|
| **GitHub Releases** | Free | Fast | Easy | ✅ Yes |
| Own Web Server | Varies | Fast | Medium | If you have one |
| Dropbox | Free | Medium | Easy | Acceptable |
| Google Drive | Free | Slow | Hard | Not recommended |

---

## 💡 Pro Tips

1. **Use GitHub Releases** - Free, reliable, and players trust it
2. **Set `require-resource-pack=false`** - Let players choose (heads are optional)
3. **Test manually first** - Install locally before configuring server-wide
4. **Custom prompt** - Use color codes to make it appealing
5. **Keep `use-overlay-by-default: true`** in config for best-looking heads

---

## 🆘 Need Help?

1. Check `RESOURCEPACK_SETUP_GUIDE.md` for detailed troubleshooting
2. Verify URL is accessible (open in browser - should download immediately)
3. Check SHA1 hash is exactly: `401b402cefdb05776cb1bb06db0afc0ed566e20d`
4. Test with manual installation first
5. Look for errors in server console when player joins

---

## ✅ Checklist

Before asking for help, verify:

- [ ] `chathead-resourcepack.zip` is uploaded and accessible via HTTPS
- [ ] SHA1 hash in `server.properties` is `401b402cefdb05776cb1bb06db0afc0ed566e20d`
- [ ] URL is direct download (not a webpage)
- [ ] Server was restarted after changing `server.properties`
- [ ] Player accepted the resource pack prompt
- [ ] Manual installation works (proves pack is valid)

---

**Quick Start Complete!** 🎉

For more details, see `RESOURCEPACK_SETUP_GUIDE.md`
