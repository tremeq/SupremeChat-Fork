# 🎯 Jak Wyświetlić Głowę Gracza w Chacie - Praktyczny Przewodnik

## 📍 Gdzie Dodać Kod

W pliku: `src/main/java/net/devscape/project/supremechat/listeners/Formatting.java`

**Linia 302** - tutaj tworzymy TextComponent:
```java
TextComponent msg = new TextComponent(TextComponent.fromLegacyText(format(formattedMessage)));
```

## 🔧 Implementacja Krok po Kroku

### **Krok 1: Zaimportuj ChatHeadAPI**

Na górze pliku `Formatting.java`, dodaj import:

```java
import net.devscape.project.supremechat.chathead.ChatHeadAPI;
import net.md_5.bungee.api.chat.BaseComponent;
```

### **Krok 2: Dodaj Opcję w Config**

W `config.yml` dodaj nową opcję (już jest tam sekcja chathead):

```yaml
# W istniejącej sekcji chathead:
chathead:
  enabled: true
  skin-source: AUTO
  cache-time-minutes: 5
  use-overlay-by-default: true

  # NOWA OPCJA - pokazuj głowy w chacie
  show-in-chat: true  # ⭐ DODAJ TO
```

### **Krok 3: Zmodyfikuj metodę handleChatFormat()**

Znajdź linię **302** i zastąp ten fragment:

#### ❌ **STARY KOD** (linia 302):
```java
// Build chat component for players
TextComponent msg = new TextComponent(TextComponent.fromLegacyText(format(formattedMessage)));
```

#### ✅ **NOWY KOD** (z głową gracza):
```java
// Build chat component for players
TextComponent msg = new TextComponent();

// Dodaj głowę gracza (jeśli włączone)
if (plugin.getConfig().getBoolean("chathead.show-in-chat", false)) {
    try {
        ChatHeadAPI headAPI = ChatHeadAPI.getInstance();
        if (headAPI != null && headAPI.isEnabled()) {
            // Użyj Smart metody - automatycznie obsługuje online/offline mode!
            BaseComponent[] playerHead = headAPI.getHeadSmart(player);

            // Dodaj głowę na początku wiadomości
            if (playerHead != null && playerHead.length > 0) {
                msg.addExtra(playerHead);
                msg.addExtra(" "); // Spacja po głowie
            }
        }
    } catch (Exception e) {
        // Jeśli coś pójdzie nie tak, po prostu ignoruj głowę
        plugin.getLogger().warning("Failed to add player head: " + e.getMessage());
    }
}

// Dodaj sformatowaną wiadomość
msg.addExtra(TextComponent.fromLegacyText(format(formattedMessage)));
```

## 🎨 Kompletny Przykład Implementacji

Cała zmodyfikowana metoda `handleChatFormat()` (fragment od linii 300):

```java
// Build chat component for players
TextComponent msg = new TextComponent();

// ============ CHATHEAD INTEGRATION ============
// Add player head if enabled in config
boolean showHeadInChat = plugin.getConfig().getBoolean("chathead.show-in-chat", false);
if (showHeadInChat) {
    try {
        ChatHeadAPI headAPI = ChatHeadAPI.getInstance();
        if (headAPI != null && headAPI.isEnabled()) {
            // Smart method automatically handles online/offline mode
            BaseComponent[] playerHead = headAPI.getHeadSmart(player);

            if (playerHead != null && playerHead.length > 0) {
                msg.addExtra(playerHead);
                msg.addExtra(" "); // Space after head
            }
        }
    } catch (IllegalArgumentException e) {
        // ChatHeadAPI not initialized (disabled in config)
        // This is normal, just skip adding the head
    } catch (Exception e) {
        plugin.getLogger().warning("Failed to add player head to chat: " + e.getMessage());
    }
}
// ============================================

// Add formatted message
msg.addExtra(TextComponent.fromLegacyText(format(formattedMessage)));

// Existing hover and click events (lines 304-317)
if (plugin.getConfig().getBoolean("hover.enable")) {
    ComponentBuilder hoverBuilder = new ComponentBuilder();
    for (String hoverLine : plugin.getConfig().getStringList("hover.string")) {
        hoverBuilder.append(new TextComponent(format(addOtherPlaceholders(hoverLine, player)))).append("\n");
    }
    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverBuilder.create()));
}

if (plugin.getConfig().getBoolean("click.enable")) {
    ClickEvent clickEvent = createClickEvent(player);
    if (clickEvent != null) {
        msg.setClickEvent(clickEvent);
    }
}

// Send to players (lines 320-324)
for (Player online : Bukkit.getOnlinePlayers()) {
    if (!perWorldChat || online.getWorld().equals(player.getWorld())) {
        online.spigot().sendMessage(ChatMessageType.CHAT, msg);
    }
}
```

## 🎯 Dla Channel Chat

Podobnie w metodzie `handleChannelFormat()` (linia ~415):

```java
// Create and send the chat message to the recipient
TextComponent msg = new TextComponent();

// Add player head if enabled
if (SupremeChat.getInstance().getConfig().getBoolean("chathead.show-in-chat", false)) {
    try {
        ChatHeadAPI headAPI = ChatHeadAPI.getInstance();
        if (headAPI != null && headAPI.isEnabled()) {
            BaseComponent[] playerHead = headAPI.getHeadSmart(player);
            if (playerHead != null && playerHead.length > 0) {
                msg.addExtra(playerHead);
                msg.addExtra(" ");
            }
        }
    } catch (Exception ignored) {
        // Skip if fails
    }
}

msg.addExtra(TextComponent.fromLegacyText(format(formattedMessage)));
```

## ⚙️ Konfiguracja

Dodaj do `configValidator()` w `SupremeChat.java`:

```java
// W metodzie configValidator(), w sekcji chathead:
if (!config.isSet("chathead.show-in-chat")) {
    config.set("chathead.show-in-chat", true); // Domyślnie włączone
    plugin.saveConfig();
}
```

## 📋 Kompletny Config YAML

```yaml
chathead:
  # Enable/disable entire ChatHead system
  enabled: true

  # Skin source (AUTO recommended)
  skin-source: AUTO

  # Cache time in minutes
  cache-time-minutes: 5

  # Use overlay (second skin layer) by default
  use-overlay-by-default: true

  # Show player heads in chat messages
  show-in-chat: true
```

## 🎨 Jak To Będzie Wyglądać

### **Z głową włączoną** (`show-in-chat: true`):
```
[🧑] Player123: Hello world!
```
*(gdzie 🧑 to prawdziwa głowa gracza w 8x8 pikselach)*

### **Bez głowy** (`show-in-chat: false`):
```
Player123: Hello world!
```

## 🔧 Zaawansowane Opcje

### **Opcja 1: Głowa tylko dla określonej rangi**

```java
// Dodaj głowę tylko jeśli gracz ma określoną rangę
String rank = FormatUtil.getRank(player);
boolean showHead = plugin.getConfig().getBoolean("chathead.show-in-chat", false);
boolean isVIP = rank != null && rank.equalsIgnoreCase("VIP");

if (showHead && isVIP) {
    BaseComponent[] playerHead = ChatHeadAPI.getInstance().getHeadSmart(player);
    msg.addExtra(playerHead);
    msg.addExtra(" ");
}
```

### **Opcja 2: Głowa z różnymi źródłami dla różnych rang**

```java
if (showHead) {
    ChatHeadAPI headAPI = ChatHeadAPI.getInstance();

    // VIP używa Mojang (najlepsza jakość)
    if (player.hasPermission("supremechat.vip")) {
        MojangSource mojang = new MojangSource();
        BaseComponent[] head = headAPI.getHead(player, true, mojang);
        msg.addExtra(head);
    } else {
        // Zwykli gracze używają Minotar (szybsze)
        BaseComponent[] head = headAPI.getHeadSmart(player);
        msg.addExtra(head);
    }
    msg.addExtra(" ");
}
```

### **Opcja 3: Bez overlay dla wydajności**

```java
if (showHead) {
    ChatHeadAPI headAPI = ChatHeadAPI.getInstance();
    // false = bez overlay (szybsze)
    BaseComponent[] playerHead = headAPI.getHeadSmart(player, false);
    msg.addExtra(playerHead);
    msg.addExtra(" ");
}
```

### **Opcja 4: Różne głowy dla różnych kanałów**

```java
// W handleChannelFormat()
Channel channel = SupremeChat.getInstance().getChannelManager().getChannel(player);
boolean showHead = plugin.getConfig().getBoolean("channels." + channel.getName() + ".show-head", true);

if (showHead) {
    BaseComponent[] playerHead = ChatHeadAPI.getInstance().getHeadSmart(player);
    msg.addExtra(playerHead);
    msg.addExtra(" ");
}
```

## 🐛 Troubleshooting

### Problem: Głowy się nie pokazują

**Sprawdź**:
```java
// Dodaj debug logging
if (plugin.getConfig().getBoolean("debug-mode", false)) {
    plugin.getLogger().info("ChatHead show-in-chat: " +
        plugin.getConfig().getBoolean("chathead.show-in-chat"));
    plugin.getLogger().info("ChatHead enabled: " +
        ChatHeadAPI.getInstance().isEnabled());
    plugin.getLogger().info("Online mode: " +
        ChatHeadAPI.getInstance().isOnlineMode());
}
```

### Problem: Błąd "ChatHeadAPI has not been initialized"

**Przyczyna**: `chathead.enabled: false` w config

**Rozwiązanie**:
```yaml
chathead:
  enabled: true  # Upewnij się, że to jest true!
```

### Problem: W offline mode pokazuje błędne głowy

**Przyczyna**: Używasz `getHead(UUID)` zamiast `getHeadSmart()`

**Rozwiązanie**:
```java
// ❌ ZŁE - nie działa w offline mode
BaseComponent[] head = api.getHead(player.getUniqueId());

// ✅ DOBRE - automatycznie obsługuje offline mode
BaseComponent[] head = api.getHeadSmart(player);
```

## 📊 Performance Impact

| Opcja | Impact | Rekomendacja |
|-------|--------|--------------|
| `show-in-chat: true` | Minimalny (cache!) | ✅ Zalecane |
| `cache-time-minutes: 5` | Optymalny | ✅ Default |
| `use-overlay: true` | Prawie niewidoczny | ✅ Wygląda lepiej |
| `skin-source: AUTO` | Automatyczna optymalizacja | ✅ Najlepszy wybór |

Cache sprawia, że po pierwszym pobraniu głowy jest **błyskawiczne**!

## ✅ Podsumowanie

### Co musisz zrobić:

1. ✅ **Zaimportuj** ChatHeadAPI na górze `Formatting.java`
2. ✅ **Dodaj** opcję `show-in-chat: true` do config.yml
3. ✅ **Zmodyfikuj** linię 302 w `handleChatFormat()`
4. ✅ **Opcjonalnie** zmodyfikuj `handleChannelFormat()`
5. ✅ **Przetestuj** na swoim serwerze!

### Przykład użycia w jednej linii:

```java
// To wszystko co potrzebujesz!
msg.addExtra(ChatHeadAPI.getInstance().getHeadSmart(player));
msg.addExtra(" ");
msg.addExtra(TextComponent.fromLegacyText(format(formattedMessage)));
```

**Gotowe!** Teraz każda wiadomość w chacie będzie miała głowę gracza! 🎉

---

**Potrzebujesz pomocy?** Sprawdź:
- CHATHEAD_INTEGRATION_GUIDE.md - Więcej przykładów API
- CHATHEAD_CONFIG_GUIDE.md - Pełna dokumentacja konfiguracji
- OFFLINE_MODE_EXPLANATION.md - Jak działa offline mode
