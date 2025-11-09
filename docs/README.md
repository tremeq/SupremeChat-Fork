# 📚 SupremeChat Documentation

This folder contains all documentation, guides, and example configuration files for SupremeChat features.

---

## 🚀 Quick Start Guides

### ChatHead Integration (NEW!)

**Start here if you want player heads in chat:**

1. **[RESOURCEPACK_QUICKSTART.md](RESOURCEPACK_QUICKSTART.md)** - ⚡ **Start here!** Works out of the box!
   - Zero configuration needed
   - Pre-configured resource pack
   - 2-minute setup

2. **[CHATHEAD_README.md](CHATHEAD_README.md)** - Complete overview
   - All features explained
   - Configuration options
   - Quick examples

### Other Features

- **[PRIVATE_MESSAGES_GUIDE.md](PRIVATE_MESSAGES_GUIDE.md)** - Private messaging system
- **[CLICK_SYSTEM_EXAMPLES.md](CLICK_SYSTEM_EXAMPLES.md)** - Advanced click actions

---

## 📖 ChatHead Detailed Documentation

### Setup & Configuration

| Document | Description | When to Use |
|----------|-------------|-------------|
| **[RESOURCEPACK_SETUP_GUIDE.md](RESOURCEPACK_SETUP_GUIDE.md)** | Complete resource pack hosting guide | Want to host your own custom pack |
| **[CHATHEAD_CONFIG_GUIDE.md](CHATHEAD_CONFIG_GUIDE.md)** | All configuration options | Need to customize settings |
| **[config-chathead-example.yml](config-chathead-example.yml)** | Ready-to-copy config | Want example configuration |
| **[server.properties.resourcepack-example](server.properties.resourcepack-example)** | server.properties method | Prefer traditional pack distribution |

### API & Implementation

| Document | Description | When to Use |
|----------|-------------|-------------|
| **[CHATHEAD_INTEGRATION_GUIDE.md](CHATHEAD_INTEGRATION_GUIDE.md)** | API usage and code examples | Adding heads to your custom formats |
| **[CHATHEAD_IMPLEMENTATION_EXAMPLE.md](CHATHEAD_IMPLEMENTATION_EXAMPLE.md)** | Step-by-step implementation | Want to add heads to chat |

### Technical Documentation

| Document | Description | When to Use |
|----------|-------------|-------------|
| **[OFFLINE_MODE_EXPLANATION.md](OFFLINE_MODE_EXPLANATION.md)** | Why offline mode needs special handling | Running cracked/offline server |
| **[CHATHEAD_RENDERING_EXPLAINED.md](CHATHEAD_RENDERING_EXPLAINED.md)** | How Unicode rendering works | Want to understand the technology |

---

## 🗂️ Documentation by Topic

### 🎨 ChatHead System

**What is ChatHead?**
ChatHead displays 8x8 player heads in chat using custom Unicode characters and a resource pack.

**Key Features:**
- ✅ Works out of the box (zero config)
- ✅ Offline mode support (cracked servers)
- ✅ Automatic resource pack distribution
- ✅ Smart caching system

**Documents:**
1. [RESOURCEPACK_QUICKSTART.md](RESOURCEPACK_QUICKSTART.md) - **Start here!**
2. [CHATHEAD_README.md](CHATHEAD_README.md) - Complete guide
3. [CHATHEAD_INTEGRATION_GUIDE.md](CHATHEAD_INTEGRATION_GUIDE.md) - API usage
4. [CHATHEAD_CONFIG_GUIDE.md](CHATHEAD_CONFIG_GUIDE.md) - Configuration
5. [RESOURCEPACK_SETUP_GUIDE.md](RESOURCEPACK_SETUP_GUIDE.md) - Custom pack hosting
6. [OFFLINE_MODE_EXPLANATION.md](OFFLINE_MODE_EXPLANATION.md) - Offline servers
7. [CHATHEAD_RENDERING_EXPLAINED.md](CHATHEAD_RENDERING_EXPLAINED.md) - Technical details
8. [CHATHEAD_IMPLEMENTATION_EXAMPLE.md](CHATHEAD_IMPLEMENTATION_EXAMPLE.md) - Code examples

**Example Files:**
- [config-chathead-example.yml](config-chathead-example.yml)
- [server.properties.resourcepack-example](server.properties.resourcepack-example)

---

### 💬 Private Messages System

**What is Private Messages?**
Complete private messaging system with /msg, /tell, /whisper, /reply commands.

**Key Features:**
- ✅ Advanced formatting with hover/click
- ✅ Social Spy for staff
- ✅ PlaceholderAPI support
- ✅ Vanish plugin compatibility

**Documents:**
- [PRIVATE_MESSAGES_GUIDE.md](PRIVATE_MESSAGES_GUIDE.md) - Complete guide

---

### 🖱️ Click System

**What is Click System?**
Advanced click actions in chat messages (suggest_command, run_command, open_url).

**Key Features:**
- ✅ 3 types of click actions
- ✅ PlaceholderAPI support
- ✅ Works in all chat formats

**Documents:**
- [CLICK_SYSTEM_EXAMPLES.md](CLICK_SYSTEM_EXAMPLES.md) - Examples and setup

---

## 🎯 Common Questions

### "I want player heads in chat, where do I start?"

**Answer:** [RESOURCEPACK_QUICKSTART.md](RESOURCEPACK_QUICKSTART.md)

The plugin is pre-configured with a working resource pack. Just install and it works!

---

### "How do I customize the resource pack?"

**Answer:** [RESOURCEPACK_SETUP_GUIDE.md](RESOURCEPACK_SETUP_GUIDE.md)

Follow the guide to host your own pack on GitHub Releases or your own server.

---

### "My server is offline mode (cracked), will ChatHead work?"

**Answer:** Yes! [OFFLINE_MODE_EXPLANATION.md](OFFLINE_MODE_EXPLANATION.md)

ChatHead has special support for offline servers using name-based skin retrieval.

---

### "How do I add heads to my custom chat format?"

**Answer:** [CHATHEAD_IMPLEMENTATION_EXAMPLE.md](CHATHEAD_IMPLEMENTATION_EXAMPLE.md)

Step-by-step guide with code examples.

---

### "What are all the ChatHead configuration options?"

**Answer:** [CHATHEAD_CONFIG_GUIDE.md](CHATHEAD_CONFIG_GUIDE.md)

Complete reference of all config.yml options.

---

### "How do I use the ChatHead API in my code?"

**Answer:** [CHATHEAD_INTEGRATION_GUIDE.md](CHATHEAD_INTEGRATION_GUIDE.md)

API documentation with 8+ code examples.

---

## 📂 File Index

### Quick Reference

```
docs/
├── README.md (this file)
│
├── ChatHead Documentation:
│   ├── RESOURCEPACK_QUICKSTART.md          ⚡ START HERE
│   ├── CHATHEAD_README.md                  📘 Complete overview
│   ├── CHATHEAD_INTEGRATION_GUIDE.md       💻 API usage
│   ├── CHATHEAD_CONFIG_GUIDE.md            ⚙️ Configuration
│   ├── RESOURCEPACK_SETUP_GUIDE.md         📦 Custom pack
│   ├── OFFLINE_MODE_EXPLANATION.md         🔓 Cracked servers
│   ├── CHATHEAD_RENDERING_EXPLAINED.md     🔬 Technical
│   ├── CHATHEAD_IMPLEMENTATION_EXAMPLE.md  📝 Code examples
│   ├── config-chathead-example.yml         📄 Config example
│   └── server.properties.resourcepack-example
│
├── Other Features:
│   ├── PRIVATE_MESSAGES_GUIDE.md           💬 PM system
│   └── CLICK_SYSTEM_EXAMPLES.md            🖱️ Click actions
```

---

## 🔗 External Links

- **[Main README](../README.md)** - Go back to main page
- **[SupremeChat Config](../src/main/resources/config.yml)** - Main config file
- **[ChatHeadFont Original](https://github.com/OGminso/ChatHeadFont)** - Original plugin we enhanced

---

## 💡 Tips

1. **New to ChatHead?** Start with [RESOURCEPACK_QUICKSTART.md](RESOURCEPACK_QUICKSTART.md)
2. **Want to code?** Check [CHATHEAD_INTEGRATION_GUIDE.md](CHATHEAD_INTEGRATION_GUIDE.md)
3. **Need help?** All documents have troubleshooting sections
4. **Offline server?** Read [OFFLINE_MODE_EXPLANATION.md](OFFLINE_MODE_EXPLANATION.md) first

---

**Last Updated:** 2025-11-05
**SupremeChat Version:** v1.15-dev
