# 🔄 Auto-Update Config System

SupremeChat automatycznie aktualizuje config.yml bez tracenia Twoich customowych ustawień!

## 📋 Jak to działa?

### System Auto-Update:

1. **Plugin startuje** → Czyta config.yml
2. **Sprawdza brakujące opcje** → Porównuje z wymaganymi kluczami
3. **Dodaje nowe opcje** → Tylko te których nie ma
4. **Zachowuje Twoje ustawienia** → Wszystkie custom wartości pozostają nietknięte
5. **Zapisuje raz** → Optymalizacja wydajności

### Przykład:

#### Przed aktualizacją (stary config):
```yaml
chat-delay: 3  # Twoja własna wartość
chat-warn: '&4Powoli! Nie spamuj!'  # Twoja własna wiadomość
# Brak chat-warn-enabled (nowa opcja)
```

#### Po aktualizacji (nowy config):
```yaml
chat-delay: 3  # ✅ ZACHOWANE - Twoja wartość
chat-warn: '&4Powoli! Nie spamuj!'  # ✅ ZACHOWANE - Twoja wiadomość
chat-warn-enabled: true  # ✅ DODANE - Nowa opcja z domyślną wartością
```

## 🎯 Co jest automatycznie aktualizowane?

### ✅ Wszystkie nowe opcje dodane w aktualizacjach:

| Opcja | Domyślna wartość | Kiedy dodano |
|-------|-----------------|--------------|
| `chat-warn-enabled` | `true` | v1.15 |
| `command-warn-enabled` | `true` | v1.15 |
| `chathead.disable-for-bedrock` | `true` | v1.15 |
| `chathead.use-overlay-by-default` | `true` | v1.14 |
| `chathead.resourcepack.*` | (pełna config) | v1.14 |
| `chatgames.strings.game-win` | (domyślna) | v1.13 |
| `per-world-chat` | `false` | v1.12 |
| `death.*` | (pełna sekcja) | v1.11 |
| `emojis.*` | (domyślne emoji) | v1.10 |
| `mention.*` | (domyślna config) | v1.10 |

### ✅ Naprawy istniejących opcji:

- **Channels** - dodaje brakujące `permission` i `chat-color`
- **Groups** - sprawdza czy wszystkie pola są ustawione

## 📝 Logi podczas aktualizacji

### Start pluginu z nowym configiem:
```
[SupremeChat] Loading config.yml...
[SupremeChat] Added new config option: chat-warn-enabled (default: true)
[SupremeChat] Added new config option: command-warn-enabled (default: true)
[SupremeChat] Config updated with new options. Your custom settings have been preserved.
[SupremeChat] SupremeChat enabled!
```

### Start pluginu ze starym configiem (wszystko OK):
```
[SupremeChat] Loading config.yml...
[SupremeChat] SupremeChat enabled!
```
(Brak dodatkowych logów - config jest już aktualny)

## 🔧 Kiedy config się aktualizuje?

1. **Start serwera** - zawsze przy `onEnable()`
2. **Reload pluginu** - przy `/schat reload`
3. **Ręczne reload** - przy `/reload` (nie zalecane)

## 💡 Best Practices

### ✅ DO:
- Edytuj config.yml normalnie
- Dodawaj własne wartości
- Zmieniaj domyślne wiadomości
- Aktualizuj plugin - config się sam zaktualizuje

### ❌ DON'T:
- Nie kasuj całego config.yml (chyba że chcesz reset)
- Nie używaj `/reload` - używaj `/schat reload`
- Nie edytuj podczas działania serwera (zmiany zostaną nadpisane)

## 🛠️ Ręczne zarządzanie

### Jeśli chcesz reset całej sekcji:

1. **Zatrzymaj serwer**
2. **Skasuj sekcję z config.yml** (np. całą sekcję `chathead:`)
3. **Wystartuj serwer** - plugin doda domyślną sekcję
4. **Edytuj wartości** jak chcesz

### Jeśli chcesz total reset:

1. **Zatrzymaj serwer**
2. **Skasuj** `plugins/SupremeChat/config.yml`
3. **Wystartuj serwer** - plugin stworzy nowy config
4. **Edytuj wartości** jak chcesz

## 🔍 Jak sprawdzić czy config jest aktualny?

Po starcie serwera:
- **Brak logów o nowych opcjach** → Config jest aktualny ✅
- **Widzisz logi "Added new config option"** → Config został zaktualizowany ✅
- **Widzisz "Config updated with new options"** → Wszystko OK ✅

## 📊 Przykład migracji

### Migracja z v1.14 → v1.15:

**Przed (v1.14):**
```yaml
chat-delay: 1
chat-warn: '&cPlease slow down, you''re chatting too fast...'
command-delay: 1
command-warn: '&cPlease slow down, you''re executing commands too fast...'

chathead:
  enabled: true
  skin-source: AUTO
  cache-time-minutes: 5
  use-overlay-by-default: true
  resourcepack:
    auto-send: true
    url: "https://..."
    sha1: ""
    prompt: "..."
    force: false
```

**Po (v1.15) - automatycznie dodane:**
```yaml
chat-delay: 1
chat-warn: '&cPlease slow down, you''re chatting too fast...'
chat-warn-enabled: true  # ← NOWE!
command-delay: 1
command-warn: '&cPlease slow down, you''re executing commands too fast...'
command-warn-enabled: true  # ← NOWE!

chathead:
  enabled: true
  disable-for-bedrock: true  # ← NOWE!
  skin-source: AUTO
  cache-time-minutes: 5
  use-overlay-by-default: true
  resourcepack:
    auto-send: true
    url: "https://..."
    sha1: ""
    prompt: "..."
    force: false
```

## 🎓 FAQ

### Q: Czy muszę usuwać stary config przy aktualizacji?
**A:** NIE! Plugin automatycznie doda nowe opcje zachowując Twoje ustawienia.

### Q: Co jeśli mam custom wiadomości?
**A:** Wszystkie Twoje custom wiadomości zostaną zachowane. Nowe opcje będą dodane z domyślnymi wartościami.

### Q: Czy mogę wyłączyć auto-update?
**A:** Nie, ale nie jest to potrzebne - system tylko dodaje brakujące opcje, nie zmienia istniejących.

### Q: Co jeśli plugin dodał coś czego nie chcę?
**A:** Możesz normalnie edytować config.yml i zmienić wartości na swoje. Plugin nie nadpisze Twoich zmian.

### Q: Jak sprawdzić jakie opcje zostały dodane?
**A:** Sprawdź logi konsoli po starcie serwera. Każda nowa opcja jest logowana.

### Q: Czy config się nadpisuje przy reload?
**A:** NIE! `/schat reload` tylko sprawdza brakujące opcje i je dodaje. Twoje ustawienia są bezpieczne.

## 🔐 Bezpieczeństwo

System auto-update jest **bezpieczny**:
- ✅ Nie usuwa żadnych opcji
- ✅ Nie zmienia istniejących wartości
- ✅ Tylko dodaje brakujące klucze
- ✅ Zapisuje config raz (nie spamuje I/O)
- ✅ Loguje wszystkie zmiany

## 📞 Wsparcie

Jeśli masz problemy z auto-update:
1. Sprawdź logi konsoli
2. Zweryfikuj czy config.yml ma poprawną składnię (YAML validator)
3. Zgłoś issue na GitHub z logami

---

**System auto-update zapewnia że Twój config jest zawsze aktualny bez tracenia customowych ustawień!** 🎉
