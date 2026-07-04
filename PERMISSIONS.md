# SupremeChat-Fork — Permissions

A full list of every permission used by the plugin, grouped by purpose.

**Legend — Default:**
- `op` — only server operators have it by default
- `true` — everyone has it by default
- `false` — nobody has it by default (must be granted)
- `—` — not a fixed node; the permission string is read from the config and can be changed

> Commands **`/msgtoggle`** and **`/ignore`** require **no permission** — they are available to every player.

---

## Command access

| Permission | Default | Description |
|---|---|---|
| `supremechat.admin` | op | Access to `/supremechat` admin subcommands (`reload`, `mutechat`, `clearchat`, `discordsrv`). |
| `supremechat.channel` | op | Use the `/channel` command (join/leave/list channels). |
| `supremechat.emojis` | true | Use the `/emojis` command to list available emojis. |
| `supremechat.msg` | true | Use `/msg`, `/tell`, `/whisper` (`/w`) to send private messages. |
| `supremechat.reply` | true | Use `/reply` (`/r`) to reply to the last messenger. |
| `supremechat.adminchat` | op | Send **and** receive admin/staff chat via `/ac`. Node is configurable in `admin-chat.permission`. |

---

## Chat features

| Permission | Default | Description |
|---|---|---|
| `supremechat.chat.color` | op | Use `&` color codes and hex colors in chat. Node is configurable in `chat-color-permission`. |
| `supremechat.mention.player` | true | Mention a single player in chat (e.g. `@Steve`). |
| `supremechat.mention.everyone` | op | Mention everyone online at once (e.g. `@everyone`). |
| `supremechat.socialspy` | op | See other players' private messages (Social Spy). Node is configurable in `private-messages.social-spy.permission`. |
| `supremechat.see.vanished` | op | Send messages to / see vanished players in the private-message system. |
| `supremechat.emoji.<name>` | true | Use a specific emoji (e.g. `supremechat.emoji.smile`). |
| `supremechat.emoji.*` | true | Use **all** emojis. |

---

## Bypasses

| Permission | Default | Description |
|---|---|---|
| `supremechat.bypass.filter` | op | Bypass chat filters (banned words, caps, repeat messages). |
| `supremechat.bypass.chatcooldown` | op | Bypass the chat cooldown/delay. |
| `supremechat.bypass.commandcooldown` | op | Bypass the command cooldown/delay. |
| `supremechat.bypass.mutechat` | op | Chat while global chat is muted. Node is configurable in `bypass-mute-chat-permission`. |
| `supremechat.bypass.clearchat` | false | Keep your chat history when an admin runs `/clearchat`. Node is configurable in `messages.clearchat.bypass-permission`. |

---

## Staff alerts

| Permission | Default | Description |
|---|---|---|
| `supremechat.commandspy.alert` | op | Receive Command-Spy alerts when players run commands. |
| `sc.alert.staff` | — | Receive banned-word filter alerts. Node is configurable in `detect-alert-staff-permission` (default `sc.alert.staff`). |

---

## Channel access

Each channel defines its own permission via `channels.<name>.permission`.
Use `None` to make a channel open to everyone.

| Permission | Default | Description |
|---|---|---|
| `supremechat.channel.staff` | op | Join the built-in `staff` channel. |
| `supremechat.channel.admin` | op | Join the built-in `admin` channel. |
| *(configurable)* | — | Any custom channel uses whatever node you set in its `permission` field. |

The default `english`, `spanish` and `french` channels use `None` (no permission required).

---

## Integrations

| Permission | Default | Description |
|---|---|---|
| *(configurable)* | — | DiscordSRV send permission, read from `discordsrv.required-permission`. Empty (default) = all players may send chat to Discord. |

---

## No-permission commands

These are intentionally available to **everyone** (no permission node):

| Command | Description |
|---|---|
| `/msgtoggle` (`/pmtoggle`, `/togglemsg`) | Toggle receiving private messages. |
| `/ignore <player>` | Ignore / unignore another player's chat, private messages and mentions. |
