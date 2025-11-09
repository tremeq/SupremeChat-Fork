# SupremeChat - Extended Click System Documentation

## Overview
The click system has been extended to support three different click actions in chat messages:
- `suggest_command` - Pre-fills the chat input (default, backward compatible)
- `run_command` - Executes a command immediately
- `open_url` - Opens a URL in the player's browser

## Configuration

### Basic Structure
```yaml
click:
  enable: true
  type: "suggest_command"  # or "run_command" or "open_url"
  value: "/msg %player_name% "
```

**Note:** The old `click.string` configuration key is still supported for backward compatibility, but `click.value` is recommended.

## Examples

### Example 1: Suggest Private Message (Default Behavior)
Pre-fills the chat with a private message command - player can type their message before sending.

```yaml
click:
  enable: true
  type: "suggest_command"
  value: "/msg %player_name% "
```

**Result:** Clicking on a player's name fills chat with `/msg PlayerName ` (cursor ready for typing)

---

### Example 2: Quick Profile Command
Immediately executes a profile command when clicking a player's name.

```yaml
click:
  enable: true
  type: "run_command"
  value: "/profile %player_name%"
```

**Result:** Clicking instantly runs `/profile PlayerName`

---

### Example 3: View Player Statistics (Web)
Opens a web page with player statistics.

```yaml
click:
  enable: true
  type: "open_url"
  value: "https://stats.yourserver.com/player/%player_uuid%"
```

**Result:** Clicking opens `https://stats.yourserver.com/player/[UUID]` in browser

---

### Example 4: Discord Invite
Open your Discord server invite link.

```yaml
click:
  enable: true
  type: "open_url"
  value: "https://discord.gg/yourserver"
```

**Result:** Clicking opens Discord invite in browser

---

### Example 5: Trade Request
Quickly send a trade request to a player.

```yaml
click:
  enable: true
  type: "run_command"
  value: "/trade %player_name%"
```

**Result:** Clicking instantly runs `/trade PlayerName`

---

### Example 6: Report Player
Pre-fill a report command.

```yaml
click:
  enable: true
  type: "suggest_command"
  value: "/report %player_name% "
```

**Result:** Clicking fills chat with `/report PlayerName ` for additional context

---

## Supported Placeholders

All PlaceholderAPI placeholders are supported, plus built-in placeholders:

- `%player_name%` - Player's username
- `%player_displayname%` - Player's display name
- `%player_uuid%` - Player's UUID
- `%player_world%` - Player's current world
- `%vault_prefix%` - Player's Vault prefix
- `%vault_suffix%` - Player's Vault suffix
- And all PlaceholderAPI placeholders...

## Backward Compatibility

Old configurations using `click.string` will continue to work:

```yaml
# OLD (still works)
click:
  enable: true
  string: "/msg %player_name% "

# Automatically treated as type: suggest_command
```

A deprecation notice will be logged once on startup if using old format.

## Validation & Error Handling

- If `click.type` is invalid, defaults to `suggest_command` with a warning
- If no value/string is configured but click is enabled, a warning is logged
- Debug mode (`debug-mode: true`) logs detailed click event information

## Debug Mode

Enable debug logging for click events:

```yaml
debug-mode: true
```

Logs will show:
```
[DEBUG] Click event created - Type: run_command, Action: RUN_COMMAND, Value: /profile PlayerName
```

## Migration Guide

### From Old Config
```yaml
click:
  enable: true
  string: "/msg %player_name% "
```

### To New Config
```yaml
click:
  enable: true
  type: "suggest_command"  # Explicitly specify type
  value: "/msg %player_name% "  # Use 'value' instead of 'string'
```

## Use Cases

| Use Case | Type | Example Value |
|----------|------|---------------|
| Private messaging | `suggest_command` | `/msg %player_name% ` |
| View profile | `run_command` | `/profile %player_name%` |
| Teleport request | `run_command` | `/tpa %player_name%` |
| Trade request | `run_command` | `/trade %player_name%` |
| Player statistics | `open_url` | `https://stats.server.com/player/%player_uuid%` |
| Discord link | `open_url` | `https://discord.gg/invite` |
| Server website | `open_url` | `https://yourserver.com` |
| Player reports | `suggest_command` | `/report %player_name% ` |

## Technical Notes

- Click events only work in `handleChatFormat()` (normal chat messages)
- All placeholders are replaced before creating the click event
- URL validation is handled by Minecraft client
- Commands must include `/` prefix for `run_command` and `suggest_command`
