# Minecraft Debug Shortcuts

Справочник отладочных сочетаний клавиш Minecraft Java Edition.

Данные ниже извлечены из ванильного клиента **26.2** скриптом [`tools/dump_debug_shortcuts.py`](tools/dump_debug_shortcuts.py) — это не выписка по памяти, а то, что игра сама показывает по `F3 + Q`.

Все комбинации `F3 + X` выполняются так: **зажать F3**, нажать вторую клавишу, отпустить обе. Экран отладки при этом не появится и не исчезнет.

---

## Список из игры (`F3 + Q`)

Ровно те 17 строк, которые клиент печатает в чат (ключи локализации `debug.*.help`).

| Сочетание | Действие |
|---|---|
| `F3 + Q` | Показать этот список |
| `F3 + A` | Перезагрузить чанки |
| `F3 + B` | Показать хитбоксы |
| `F3 + C` | Скопировать координаты как команду `/tp`; **удерживать 10 секунд** — принудительный краш игры |
| `F3 + D` | Очистить чат |
| `F3 + G` | Показать границы чанков |
| `F3 + H` | Расширенные подсказки предметов |
| `F3 + I` | Скопировать данные сущности или блока в буфер обмена |
| `F3 + L` | Запустить/остановить профилирование |
| `F3 + N` | Переключение «предыдущий режим игры ↔ наблюдатель» |
| `F3 + P` | Пауза при потере фокуса окна |
| `F3 + S` | Выгрузить динамические текстуры (dump dynamic textures) |
| `F3 + T` | Перезагрузить ресурспаки |
| `F3 + V` | Информация о версии клиента |
| `F3 + Esc` | Пауза без меню паузы (если пауза вообще возможна) |
| `F3 + F4` | Открыть переключатель режимов игры |
| `F3 + F6` | Открыть экран Debug Options |

## Перепривязываемые debug-клавиши

В 26.2 все отладочные действия — обычные key bindings (`key.debug.*`), их можно переназначить в **Options → Controls**. `F3` здесь не зашит: это отдельный биндинг-модификатор `key.debug.modifier`.

| Ключ биндинга | Название в игре |
|---|---|
| `key.debug.modifier` | Debug Modifier Key (по умолчанию `F3`) |
| `key.debug.overlay` | Toggle Overlay |
| `key.debug.clearChat` | Clear Chat |
| `key.debug.copyLocation` | Copy Location |
| `key.debug.copyRecreateCommand` | Copy Data |
| `key.debug.crash` | Debug Crash |
| `key.debug.debugOptions` | Debug Options |
| `key.debug.dumpDynamicTextures` | Dump Dynamic Textures |
| `key.debug.dumpVersion` | Dump Version Info |
| `key.debug.focusPause` | Toggle Lost Focus Pause |
| `key.debug.fpsCharts` | FPS Charts |
| `key.debug.networkCharts` | Network Charts |
| `key.debug.profilingChart` | Profiling Chart |
| `key.debug.lightmapTexture` | Lightmap Texture |
| `key.debug.profiling` | Start/Stop Profiling |
| `key.debug.reloadChunk` | Reload Chunks |
| `key.debug.reloadResourcePacks` | Reload Resource Packs |
| `key.debug.showAdvancedTooltips` | Show Advanced Tooltips |
| `key.debug.showChunkBorders` | Show Chunk Boundaries |
| `key.debug.showHitboxes` | Show Hitboxes |
| `key.debug.spectate` | Cycle Spectator |
| `key.debug.switchGameMode` | Game Mode Switcher |

Четыре биндинга в список `F3 + Q` не попадают, хотя сочетания у них есть — вот их значения по умолчанию (считаны из `Options.debugKeys` в чистом клиенте 26.2):

| Сочетание | Действие |
|---|---|
| `F3 + 1` | Profiling Chart — график профилировщика |
| `F3 + 2` | FPS Charts — графики FPS |
| `F3 + 3` | Network Charts — сетевые графики |
| `F3 + 4` | Lightmap Texture — текстура карты освещения |

Ещё два биндинга — `key.debug.overlay` (сам `F3`) и `key.debug.modifier` — в `debugKeys` не входят: они не «F3 + что-то», а сам механизм вызова.

---

## Как получить список самому

### 1. В игре

`F3 + Q` — клиент печатает актуальный для вашей версии список в чат.

### 2. Из клиентского jar (без запуска игры)

```bash
python tools/dump_debug_shortcuts.py            # последний релиз
python tools/dump_debug_shortcuts.py 1.21.11    # конкретная версия
python tools/dump_debug_shortcuts.py --format md
```

Скрипт берёт `version_manifest_v2.json` у Mojang, качает `client.jar`, распаковывает
`assets/minecraft/lang/en_us.json` и вытаскивает ключи `debug.*.help` и `key.debug.*`.
Работает для любой версии, jar кэшируется в `tools/.cache/`.

### 3. В рантайме, из мода

Все отладочные биндинги лежат в публичном массиве `Options.debugKeys`:

```java
Minecraft client = Minecraft.getInstance();
for (KeyMapping mapping : client.options.debugKeys) {
    Component name = Component.translatable(mapping.getName());   // "Show Hitboxes"
    Component key  = mapping.getTranslatedKeyMessage();           // текущая клавиша
    Component mod  = client.options.keyDebugModifier.getTranslatedKeyMessage();
}
```

Это единственный способ получить список **с учётом переназначенных игроком клавиш** —
`F3 + Q` и языковой файл показывают дефолтные сочетания.

---

## Общие клавиши, полезные при отладке

| Клавиша | Действие |
|---|---|
| `F1` | Скрыть/показать HUD |
| `F2` | Скриншот (в `.minecraft/screenshots`) |
| `F3` | Показать/скрыть экран отладки |
| `F5` | Смена вида от 3-го лица |
| `F11` | Полноэкранный режим |
| `Ctrl + B` | Вкл/выкл рассказчика (Narrator) |
| `Средняя кнопка мыши` | Pick block — взять блок под прицелом |
| `Ctrl + ПКМ` по блоку в креативе | Копировать блок вместе с NBT |

## Отладочные инструменты и команды

| Инструмент / команда | Назначение |
|---|---|
| `/debug start` \| `stop` \| `function` | Профилирование тиков сервера, результат в `debug/` |
| `/perf start` \| `stop` | Профилирование производительности сервера |
| `/give @s minecraft:debug_stick` | Debug Stick: ПКМ меняет значение свойства блока, ЛКМ выбирает свойство |
| `/tick freeze` \| `step` \| `rate` | Заморозка и пошаговое выполнение тиков |
| `/data get entity @s` | Просмотр NBT игрока или сущности |
| `minecraft:light` | Невидимый источник света для отладки освещения |
| `minecraft:structure_block` | Сохранение/загрузка структур |
| `Debug World` (Shift + «Тип мира» при создании) | Мир со всеми блоками и всеми их состояниями |

---

## Заметки

- Комбинации `F3 + …` — только Java Edition; в Bedrock Edition набор отличается.
- Если `F3` перехватывается ноутбуком, попробуйте `Fn + F3`.
- Устаревшее: `F3 + F` / `F3 + Shift + F` (дальность прорисовки) удалено; `F3 + S` раньше перезагружал звуки, сейчас выгружает динамические текстуры.
- Список зависит от версии — для своей сборки всегда сверяйтесь с `F3 + Q` или запустите скрипт с нужной версией.
