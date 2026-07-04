
<img width="1003" height="165" alt="supremechat_title" src="https://github.com/user-attachments/assets/8f3c5588-92f2-4b1f-801d-efabba1c0521" />

## 📢 About SupremeChat

**There's an issue/bug where can I report it?**
You can get instant support from my discord server.

**What makes this chat plugin different, from other chat plugins?**
SupremeChat is designed to incorporate dedicated type chat systems into one system, no need to have 10 different plugins for chat.

## 📦 Dependencies

**Optional (all soft dependencies — the plugin runs fine without any of them):**
- Vault - required only for group/rank-based chat formatting
- PlaceholderAPI - for placeholders in formats
- DiscordSRV - for Discord integration
- Floodgate - auto-disables ChatHeads for Bedrock Edition players

---

<img width="870" height="80" alt="supremechatbannerfeatures" src="https://github.com/user-attachments/assets/26ab5041-b8d3-4dbb-b2c0-7aadb28c727b" />

### Core Features
- **Anti Bot Preventions** - Protects against bot spam attacks
- **Mute Chat** - Global chat muting for maintenance or events
- **✨ NEW: Clear Chat** - `/clearchat` wipes the chat for everyone (configurable, with bypass permission)
- **Advanced Chat Formatting (Hover & Click)**
  - Extended Click System (suggest_command, run_command, open_url)
  - Click to execute commands, pre-fill chat, or open URLs
  - Full PlaceholderAPI support in click actions
- **ChatHead Integration** - 8x8 Player heads in chat
  - **Works out of the box** - Zero configuration needed!
  - Automatic resource pack distribution
  - Full offline mode support (cracked servers)
  - **Bedrock Edition detection** - Auto-disables heads for Bedrock players
  - Smart caching and multiple skin sources
  - Embedded ChatHeadFont API with enhancements
- **Private Messages System** (/msg, /tell, /whisper, /reply)
  - Complete takeover of PM commands with advanced formatting
  - Full hover & click event support in private messages
  - PlaceholderAPI integration in PM formats
  - Social Spy for staff monitoring
  - Extended click system support (3 types of actions)
  - Customizable error messages
  - Vanish plugin support
  - **✨ NEW:** `/msgtoggle` - players can enable/disable receiving private messages
  - **✨ NEW:** `/ignore <player>` - ignore a player's chat, private messages and mentions (persists across restarts)
- **✨ NEW: Admin/Staff Chat** - `/ac <message>` with a configurable permission & format
- **Group Formatting** - Different chat formats per permission group
- **Per World Formatting** - Separate chat for different worlds
- **Channels System** - Multiple chat channels with permissions
  - **✨ NEW:** master on/off switch for the whole channel system
  - **✨ NEW:** all channel command messages are now configurable
- **Join/Leave/MOTD Actions** - Customizable join/leave messages with titles
- **Custom Commands** - Create custom chat commands
- **Mentioning** - @player mentions with sound notifications
  - **✨ NEW:** working @everyone mention with its own permission
- **Advanced Chat Filters**
  - Blocked words detection with staff alerts
  - Spam prevention
  - Repeat message detection
  - Caps filter with auto-lowercase
- **Custom Death Messages** - Personalized death messages
- **Chat Games System** - Interactive mini-games in chat
  - Math challenges
  - Trivia questions
  - Word unscrambler
  - Configurable rewards
- **Item In Chat System** - Show items in chat messages
- **Chat Emojis** - 31 ready-to-use emojis with emoticon shortcuts
- **✨ NEW: Full Logging System** - optionally log every chat message and command to file
- **✨ NEW: Configurable Prefix** - set your own plugin prefix at the top of the config
- **✨ NEW: Fully Configurable Messages** - every message sent to players lives in the config (incl. a toggle to hide the help menu)
- **Vault Debug Mode** - Detailed logging for Vault integration
- **DiscordSRV Integration** - Send chat messages to Discord
  - Full integration with DiscordSRV plugin
  - Configurable channel routing
  - Message filtering options
  - Debug mode for troubleshooting

---

<img width="870" height="80" alt="supremechatbannercommands" src="https://github.com/user-attachments/assets/cc58a75b-6e2c-4c23-bca1-ff97f1aab4d4" />

### Commands
- `/schat <help/reload/mutechat>` - Main plugin commands
- **✨ NEW:** `/schat clearchat` - Clear the chat for everyone
- `/channels <help/join/leave> [channel]` - Channel management
- `/emojis` - List all available emojis
- `/msg, /tell, /whisper (or /w) <player> <message>` - Send private messages
- `/reply (or /r) <message>` - Reply to last messenger
- **✨ NEW:** `/msgtoggle` - Toggle receiving private messages (available to everyone)
- **✨ NEW:** `/ignore <player>` - Ignore or unignore a player (available to everyone)
- **✨ NEW:** `/ac <message>` - Admin/staff chat

---

## 🔧 Recent Updates & Bug Fixes

### v1.15.2 (Latest)

**New Features:**
- ✅ `/clearchat` - clear the chat for everyone (configurable + bypass permission)
- ✅ `/msgtoggle` - players can toggle receiving private messages
- ✅ `/ignore <player>` - ignore a player's chat, private messages and mentions (persistent)
- ✅ `/ac` - admin/staff chat with a configurable permission & format
- ✅ Configurable plugin prefix (top of config)
- ✅ Full logging system (chat + commands, each toggleable)
- ✅ Master on/off switch for the whole channel system
- ✅ Working, permissioned `@everyone` mention
- ✅ Every player-facing message is now configurable (incl. a help-menu toggle)

**Bug Fixes:**
- ✅ Fixed 1.21.5 crash (Vault `NoClassDefFoundError` on `AsyncPlayerChatEvent`)
- ✅ **Vault is now fully optional** - the plugin auto-detects it and runs cleanly without it
- ✅ Fixed channel-reload `NullPointerException`
- ✅ Fixed the mute-chat bypass logic

### v1.15-dev

**NEW: ChatHead Integration 🎨**
- ✅ Full ChatHeadFont API integration with enhancements
- ✅ **Works out of the box** - Zero configuration required!
- ✅ Automatic resource pack distribution to players
- ✅ Offline mode support (name-based skin retrieval for cracked servers)
- ✅ Smart server mode detection (online/offline)
- ✅ Multiple skin sources (Mojang, Minotar, Crafatar, MC-Heads)
- ✅ Configurable caching system (5 min default)
- ✅ Pre-configured with working resource pack URL
- ✅ Optional custom pack hosting support

### v1.14-dev-1.3

**New Features:**
- ✅ Extended Click System for chat messages (3 action types)
- ✅ Complete Private Messages system with advanced formatting
- ✅ Social Spy for staff PM monitoring
- ✅ Vault debug mode for troubleshooting
- ✅ DiscordSRV integration (Beta - requires DiscordSRV plugin)

**Bug Fixes:**
- ✅ Fixed DiscordSRV NoClassDefFoundError when plugin not installed
- ✅ Fixed Chat Games not responding to config changes after reload
- ✅ Fixed Chat Games scheduler not stopping on plugin disable
- ✅ Improved error handling for missing dependencies
- ✅ Fixed memory leaks in chat games system

**Improvements:**
- ✅ Chat Games now properly reloads with `/schat reload`
- ✅ Dynamic config reading for chat games (enable/disable without restart)
- ✅ Better compatibility with vanish plugins
- ✅ Enhanced debug logging throughout the plugin
- ✅ Graceful degradation when optional dependencies are missing

---

## 📚 Documentation

### Core Features
- **[🔑 Permissions List](PERMISSIONS.md)** - Every permission with a short description
- **[🔄 Config Auto-Update](CONFIG_AUTO_UPDATE.md)** - How config updates work without losing your settings
- **[Private Messages Guide](docs/PRIVATE_MESSAGES_GUIDE.md)** - Complete PM system documentation
- **[Extended Click System](docs/CLICK_SYSTEM_EXAMPLES.md)** - Click action examples and configuration

### ChatHead Integration
- **[ChatHead Quick Start](docs/RESOURCEPACK_QUICKSTART.md)** - ⚡ 5-minute setup (works out of box!)
- **[ChatHead Complete Guide](docs/CHATHEAD_README.md)** - Full feature overview and documentation
- **[API Integration Guide](docs/CHATHEAD_INTEGRATION_GUIDE.md)** - How to use ChatHead API in code
- **[Configuration Guide](docs/CHATHEAD_CONFIG_GUIDE.md)** - All configuration options explained
- **[Resource Pack Setup](docs/RESOURCEPACK_SETUP_GUIDE.md)** - Hosting your own custom pack
- **[Offline Mode Explanation](docs/OFFLINE_MODE_EXPLANATION.md)** - Technical details about cracked servers
- **[Rendering Technical Details](docs/CHATHEAD_RENDERING_EXPLAINED.md)** - How Unicode rendering works
- **[Implementation Example](docs/CHATHEAD_IMPLEMENTATION_EXAMPLE.md)** - Add heads to chat messages

### Configuration Examples
- **[ChatHead Config Example](docs/config-chathead-example.yml)** - Ready-to-copy configuration
- **[server.properties Example](docs/server.properties.resourcepack-example)** - Alternative pack distribution method

---

## 🔮 Integrations

- **Vault** - *Optional.* Group/rank chat formatting (the plugin works fine without it)
- **PlaceholderAPI** - Extensive placeholder support in all messages
- **DiscordSRV** - Send chat messages to Discord channels (Optional)
- **Floodgate** - Auto-disables ChatHeads for Bedrock Edition players (Optional)
- **Vanish Plugins** - Hide vanished players from PM and mentions
