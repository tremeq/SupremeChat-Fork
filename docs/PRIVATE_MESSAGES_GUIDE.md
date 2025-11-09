# SupremeChat - Private Messages System Guide

## Overview

SupremeChat now fully **takes over** and formats private messages, replacing default Minecraft/Essentials commands with advanced formatting, hover events, click events, and full PlaceholderAPI support.

## Supported Commands

### Sending Messages
- `/msg <player> <message>` - Send a private message
- `/tell <player> <message>` - Alias for /msg
- `/whisper <player> <message>` or `/w` - Alias for /msg

### Replying
- `/reply <message>` or `/r <message>` - Reply to the last person who messaged you

## Full Configuration

```yaml
# ==================================================
# PRIVATE MESSAGES (PM):
#
# SupremeChat now handles /msg, /tell, /whisper (/w), /reply (/r)
# With full formatting support including hover, click events, and placeholders!
# ==================================================
private-messages:
  enable: true # Enable/disable SupremeChat's private message system

  # Message formats
  format:
    # What the sender sees when they send a message
    sender: '&d&l[You &8➟ &d&l%receiver_name%&d&l] &f%message%'
    # What the receiver sees when they get a message
    receiver: '&d&l[%sender_name% &8➟ &d&lYou] &f%message%'

  # Hover event when you hover over a private message
  hover:
    enable: true
    string:
      - '&8&m---------------'
      - '&7Player: &d%player_name%'
      - '&7World: &b%world%'
      - '&7Click to reply'
      - '&8&m---------------'

  # Click event when you click on a private message
  # Uses the extended click system!
  click:
    enable: true
    # Type of click action:
    #   suggest_command - Pre-fills the chat input (player can edit before sending)
    #   run_command - Executes command immediately
    #   open_url - Opens URL in player's browser
    type: 'suggest_command'
    # Value supports all PlaceholderAPI placeholders
    value: '/msg %player_name% '
    # Examples:
    #   type: 'suggest_command', value: '/msg %player_name% ' - Pre-fill reply
    #   type: 'run_command', value: '/tpa %player_name%' - Send teleport request
    #   type: 'open_url', value: 'https://yourserver.com/report?player=%player_name%' - Open report page

  # Social Spy - allows staff to see all private messages
  social-spy:
    enable: false
    permission: 'supremechat.socialspy'
    format: '&8[&7SPY&8] &d%sender_name% &7→ &d%receiver_name%&7: &f%message%'

  # Error messages
  disabled-message: '&cPrivate messages are currently disabled.'
  player-not-found: '&cPlayer &e%player% &cis not online.'
  cannot-message-self: '&cYou cannot send a message to yourself.'
  no-reply-target: '&cYou have no one to reply to.'
  reply-target-offline: '&cThat player is no longer online.'
```

## Available Placeholders

### Built-in Placeholders
- `%sender_name%` - Sender's username
- `%sender_displayname%` - Sender's display name
- `%receiver_name%` - Receiver's username
- `%receiver_displayname%` - Receiver's display name
- `%message%` - Message content
- `%player_name%` - Player name (in hover/click context)
- `%player_displayname%` - Player's display name
- `%world%` - Player's world name
- `%player_uuid%` - Player's UUID

### PlaceholderAPI
All PlaceholderAPI placeholders are supported! Examples:
- `%player_health%`
- `%player_level%`
- `%vault_prefix%`
- `%vault_suffix%`
- `%luckperms_prefix%`
- And all others...

## How It Works - Step by Step

### Example 1: Player sends a message

**Situation:**
- Steve types: `/msg Alex Hey, how are you?`

**What happens:**

1. **Validation:**
   - Checks if PM is enabled (`enable: true`)
   - Checks if Alex is online
   - Checks if Alex is not vanished (if enabled)
   - Checks if Steve is not messaging himself

2. **Formatting for sender (Steve sees):**
   ```
   Config: '&d&l[You &8➟ &d&l%receiver_name%&d&l] &f%message%'
   Result: [You ➟ Alex] Hey, how are you?
   ```
   - Replaces `%receiver_name%` → "Alex"
   - Replaces `%message%` → "Hey, how are you?"
   - Applies colors

3. **Formatting for receiver (Alex sees):**
   ```
   Config: '&d&l[%sender_name% &8➟ &d&lYou] &f%message%'
   Result: [Steve ➟ You] Hey, how are you?
   ```
   - Replaces `%sender_name%` → "Steve"
   - Replaces `%message%` → "Hey, how are you?"
   - Applies colors

4. **Adding Hover Event:**
   - When Steve hovers over his message: sees info about Alex
   - When Alex hovers over the message: sees info about Steve

   ```
   ---------------
   Player: Steve
   World: world
   Click to reply
   ---------------
   ```

5. **Adding Click Event:**
   ```yaml
   type: 'suggest_command'
   value: '/msg %player_name% '
   ```
   - When Alex clicks on the message: chat fills with `/msg Steve `
   - Alex can immediately type a response

6. **Tracking for /reply:**
   - System saves: Steve last messaged → Alex
   - System saves: Alex last received message from → Steve

7. **Social Spy (if enabled):**
   - Admins with permission `supremechat.socialspy` see:
     ```
     [SPY] Steve → Alex: Hey, how are you?
     ```

---

### Example 2: Using /reply

**Situation:**
- Alex types: `/reply Great, thanks!`

**What happens:**

1. **Checking last messenger:**
   - System checks: who last messaged Alex?
   - Answer: Steve

2. **Redirecting to /msg:**
   - System automatically executes: `/msg Steve Great, thanks!`

3. **Then works like normal /msg:**
   - Formatting, hover, click, social spy...

---

## Extended Click System in PM

The PM system uses the **same extended click system** as public chat!

### Example: Suggest Command (default)
```yaml
private-messages:
  click:
    type: 'suggest_command'
    value: '/msg %player_name% '
```
**Effect:** Click → fills chat: `/msg PlayerName `

### Example: Run Command
```yaml
private-messages:
  click:
    type: 'run_command'
    value: '/tpa %player_name%'
```
**Effect:** Click → immediately sends teleport request

### Example: Open URL
```yaml
private-messages:
  click:
    type: 'open_url'
    value: 'https://myserver.com/report?player=%player_name%'
```
**Effect:** Click → opens player report form

---

## Social Spy - Monitoring PM

Administrators can monitor all private messages.

### Configuration
```yaml
private-messages:
  social-spy:
    enable: true
    permission: 'supremechat.socialspy'
    format: '&8[&7SPY&8] &d%sender_name% &7→ &d%receiver_name%&7: &f%message%'
```

### How it works?
1. Admin has permission `supremechat.socialspy`
2. When someone sends a PM, admin sees:
   ```
   [SPY] Steve → Alex: Hey, how are you?
   ```
3. Admin is NOT in the sender/receiver list so doesn't receive normal PM

---

## Permissions

### Basic
- `supremechat.msg` - Allows using /msg, /tell, /whisper
- `supremechat.reply` - Allows using /reply, /r

### Additional
- `supremechat.socialspy` - View all PMs (if social-spy is enabled)
- `supremechat.see.vanished` - Send PMs to vanished players

---

## Integrations

### Vanish Support
```yaml
vanish-support: true
```
- Vanished players are "invisible" for PM (unless sender has `supremechat.see.vanished`)
- When trying to message a vanished player: "Player not found"

### PlaceholderAPI
Automatic support for all placeholders:
```yaml
format:
  sender: '%vault_prefix% You → %receiver_name%: %message%'
  receiver: '%vault_prefix% %sender_name% → You: %message%'
```

### Vault Integration
Works seamlessly with Vault prefixes and suffixes:
```yaml
format:
  sender: '&7[%vault_prefix%&7You &8→ %vault_prefix%&7%receiver_name%&7] %message%'
  receiver: '&7[%vault_prefix%&7%sender_name% &8→ &7You&7] %message%'
```

---

## Example Configurations

### 1. Minimalist Format
```yaml
private-messages:
  format:
    sender: '&7To %receiver_name%: &f%message%'
    receiver: '&7From %sender_name%: &f%message%'
  hover:
    enable: false
  click:
    enable: false
```

### 2. Format with Vault Prefixes
```yaml
private-messages:
  format:
    sender: '%vault_prefix% &7You &8→ %receiver_name%&7: &f%message%'
    receiver: '%vault_prefix% &7%sender_name% &8→ &7You: &f%message%'
  hover:
    enable: true
    string:
      - '&7Player: &e%player_name%'
      - '&7Rank: %vault_prefix%'
      - '&7Click to reply!'
  click:
    enable: true
    type: 'suggest_command'
    value: '/msg %player_name% '
```

### 3. Discord-Style Format
```yaml
private-messages:
  format:
    sender: '&9@You &7→ &9@%receiver_name% &8| &f%message%'
    receiver: '&9@%sender_name% &7→ &9@You &8| &f%message%'
  hover:
    enable: true
    string:
      - '&9@%player_name%'
      - '&7Level: &e%player_level%'
      - '&7World: &b%world%'
      - ''
      - '&7Click to send a message!'
  click:
    enable: true
    type: 'suggest_command'
    value: '/msg %player_name% '
  social-spy:
    enable: true
    format: '&8[&9SPY&8] &9@%sender_name% &7→ &9@%receiver_name% &8| &f%message%'
```

### 4. Advanced Hover with Stats
```yaml
private-messages:
  hover:
    enable: true
    string:
      - '&8&m─────────────────'
      - '&7Player: &d%player_name%'
      - '&7Rank: %vault_prefix%'
      - '&7World: &b%world%'
      - '&7Health: &c%player_health%'
      - '&7Level: &e%player_level%'
      - '&7Money: &a$%vault_eco_balance%'
      - ''
      - '&7Click to send a message!'
      - '&8&m─────────────────'
```

### 5. Click to View Profile
```yaml
private-messages:
  click:
    enable: true
    type: 'run_command'
    value: '/profile %player_name%'
```

### 6. Click to Open Player Stats Page
```yaml
private-messages:
  click:
    enable: true
    type: 'open_url'
    value: 'https://stats.yourserver.com/player/%player_uuid%'
```

---

## Debug Mode

Enable debug mode to see detailed PM information:

```yaml
debug-mode: true
```

**Example logs:**
```
[SupremeChat] [DEBUG] PM from Steve to Alex
[SupremeChat] [DEBUG] Sender sees: [You ➟ Alex] Hey!
[SupremeChat] [DEBUG] Receiver sees: [Steve ➟ You] Hey!
[SupremeChat] [DEBUG] PM Click event - Type: suggest_command, Action: SUGGEST_COMMAND, Value: /msg Alex
```

---

## Disabling PM System

If you want to return to Essentials/default PM:

```yaml
private-messages:
  enable: false
```

Then SupremeChat won't take over /msg, /tell, /whisper, /reply commands.

---

## Comparison with Essentials

| Feature | Essentials | SupremeChat PM |
|---------|-----------|----------------|
| Basic PM | ✅ | ✅ |
| /reply | ✅ | ✅ |
| Formatting | ❌ Limited | ✅ Full |
| Hover Events | ❌ | ✅ |
| Click Events | ❌ | ✅ (3 types!) |
| PlaceholderAPI | ❌ | ✅ |
| Social Spy | ✅ | ✅ |
| Extended Click | ❌ | ✅ |
| Custom Error Messages | ❌ | ✅ |
| Vanish Support | ✅ | ✅ |

---

## Frequently Asked Questions

### Q: Do I need to remove Essentials?
**A:** No! Just enable `enable: true` in `private-messages` and SupremeChat will take over the commands.

### Q: Does it work with vanish plugins?
**A:** Yes! Enable `vanish-support: true` in the main config.

### Q: Can I have different formats for different ranks?
**A:** Not directly, but you can use PlaceholderAPI placeholders like `%vault_prefix%` in formats.

### Q: Does Social Spy work?
**A:** Yes! Admins with `supremechat.socialspy` see all PMs.

### Q: Does /reply work across worlds?
**A:** Yes, /reply works globally (unless the player logs out).

### Q: Can I customize error messages?
**A:** Yes! All error messages are configurable in the config.

### Q: What happens if two plugins have /msg?
**A:** The plugin that loads first usually takes priority. Check your server startup logs to see which plugin registered the command.

---

## Troubleshooting

### Problem: PM commands don't work
**Solution:**
1. Check `private-messages.enable: true`
2. Check plugin.yml - commands are registered
3. Check logs on server startup
4. Check for conflicts with other plugins (Essentials might take commands first - check load order)

### Problem: Click events don't work
**Solution:**
1. Check `private-messages.click.enable: true`
2. Ensure `type` is valid (suggest_command/run_command/open_url)
3. Enable debug mode and check logs
4. Make sure the value is properly formatted (commands need `/` prefix)

### Problem: Hover shows wrong player
**Solution:**
- This is normal! Hover always shows info about the "other person":
  - Sender sees info about receiver
  - Receiver sees info about sender

### Problem: PlaceholderAPI placeholders don't work
**Solution:**
1. Ensure PlaceholderAPI is installed
2. Ensure required expansions are installed (e.g., Player, Vault)
3. Check placeholder syntax with `/papi parse <player> <placeholder>`
4. Enable debug mode to see the processed messages

### Problem: Social Spy not working
**Solution:**
1. Check `social-spy.enable: true`
2. Ensure staff has `supremechat.socialspy` permission
3. Check that staff is not the sender or receiver (social spy doesn't show your own messages)

---

## Advanced Usage

### Using Relational Placeholders
PlaceholderAPI supports relational placeholders between two players:

```yaml
private-messages:
  format:
    sender: '&7You → %receiver_name% &8[&7Relation: %rel_player_name%&8] %message%'
```

### Conditional Formatting with DeluxeChat
While SupremeChat doesn't have built-in conditional formatting, you can use PlaceholderAPI conditionals:

```yaml
format:
  sender: '%player_online% &7You → %receiver_name%: %message%'
```

### Integration with Economy Plugins
Add cost to sending messages:

```yaml
format:
  sender: '&7[Cost: &a$0.10&7] You → %receiver_name%: %message%'
```

Then use a command cost plugin to charge for `/msg`.

---

## Source Code

Implementation is located in:
- `MessageCommand.java` - Handles /msg, /tell, /whisper
- `ReplyCommand.java` - Handles /reply, /r
- `SupremeChat.java` - Tracks last messengers
- `config.yml` - Configuration

---

## Performance Notes

- **Memory:** Uses HashMap for tracking (O(1) lookup)
- **CPU:** Minimal overhead - only processes when PM is sent
- **Network:** No additional network traffic
- **Async:** PM processing is async-safe

---

## Migration Guide

### From Essentials
1. Ensure SupremeChat loads after Essentials
2. Enable `private-messages.enable: true`
3. Copy your Essentials PM format to SupremeChat config
4. Test thoroughly before going live

### From Other Chat Plugins
1. Disable PM features in other plugins
2. Configure SupremeChat PM system
3. Test all commands
4. Adjust formats to match your server style

---

**The system is fully functional and ready to use!** 🎉

For more information about the extended click system, see `CLICK_SYSTEM_EXAMPLES.md`.
