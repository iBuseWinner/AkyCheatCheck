# AkyCheatCheck

<p align="left">
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.20.4-2f6f3e?style=for-the-badge">
  <img alt="Java" src="https://img.shields.io/badge/Java-17+-b07219?style=for-the-badge">
  <img alt="Paper" src="https://img.shields.io/badge/Paper-supported-1f6feb?style=for-the-badge">
  <img alt="Spigot" src="https://img.shields.io/badge/Spigot-supported-f58220?style=for-the-badge">
  <img alt="Gradle" src="https://img.shields.io/badge/Gradle-builds-02303a?style=for-the-badge&logo=gradle&logoColor=white">
</p>

<p align="left">
  <img alt="Version" src="https://img.shields.io/badge/version-1.0.0-111111?style=flat-square">
  <img alt="PlaceholderAPI" src="https://img.shields.io/badge/PlaceholderAPI-optional-526cfe?style=flat-square">
  <img alt="Web Dashboard" src="https://img.shields.io/badge/Web_dashboard-included-c23224?style=flat-square">
  <img alt="Architecture" src="https://img.shields.io/badge/architecture-service_based-1f7a4d?style=flat-square">
  <img alt="License" src="https://img.shields.io/badge/license-not_set-lightgrey?style=flat-square">
</p>

Плагин для проверки игроков на читы.  
Цель простая: быстро вызвать игрока, изолировать его, провести проверку и нормально сохранить результат.

Поддержка: **Paper / Spigot 1.20.4**  
Java: **17+**  
Package: `ru.akydevv`

---

## Что умеет

- вызов игрока на проверку;
- заморозка и телепорт в кабинку;
- несколько кабинок без конфликтов;
- BossBar с таймером;
- автобан за выход или истечение времени;
- авто-бан по фразам признания;
- отдельный чат игрока и модератора;
- блокировка команд во время проверки;
- блокировка телепортов, дропа, инвентаря, интерактов и урона;
- определение клиента через brand и каналы клиента;
- поддержка Lunar, Badlion, Forge, Fabric и похожих клиентов;
- трофей честному игроку после проверки;
- статистика модераторов;
- AFK-трекинг с автовызовом;
- логи в файл;
- Discord webhook;
- Telegram уведомления;
- Web-панель для просмотра проверок и логов;
- PlaceholderAPI expansion;
- API-события для других плагинов.

---

## Установка

1. Скачай `AkyCheatCheck-1.0.0.jar`.
2. Положи файл в папку:

```text
plugins/
```

3. Перезапусти сервер.
4. Открой конфиг:

```text
plugins/AkyCheatCheck/config.yml
```

5. Настрой кабинку проверки.

Можно вручную в `config.yml`.  
Можно из игры:

```text
/akycheck setcabin default
```

Команду нужно выполнить в месте, куда должен попадать игрок на проверку.

---

## Команды

Основная команда:

```text
/akycheck
```

Алиасы:

```text
/check
/cc
```

### Проверки

```text
/akycheck start <ник> [секунды]
```

Начать проверку.

```text
/akycheck release <ник>
/akycheck as <ник>
```

Отпустить игрока. Если включены трофеи, игрок получит предмет.

```text
/akycheck ban <ник> [причина]
/akycheck soft <ник> [причина]
```

Забанить игрока по итогам проверки.

```text
/akycheck deny <ник> [причина]
```

Забанить за игнор проверки.

```text
/akycheck confess <ник>
```

Забанить за признание.

```text
/akycheck addtime <ник> <минуты>
```

Добавить время к проверке.

### Информация

```text
/akycheck info <ник>
```

Показывает статус проверки и клиент игрока.

```text
/akycheck list
```

Показывает активные проверки.

```text
/akycheck stats
/akycheck stats <модератор>
```

Показывает статистику модераторов.

### Настройка

```text
/akycheck setcabin [id]
```

Сохраняет кабинку на текущей позиции.

```text
/akycheck reload
```

Перезагружает конфиг.

---

## Права

```text
akycheatcheck.moderator
```

Доступ к командам проверки.

```text
akycheatcheck.reload
```

Перезагрузка конфига и настройка кабинок.

```text
akycheatcheck.bypass
```

Иммунитет от AFK-вызова на проверку.

---

## PlaceholderAPI

Если на сервере стоит PlaceholderAPI, expansion подключится сам.

Доступные плейсхолдеры:

```text
%akycheck_active_checks%
%akycheck_moderator_checks%
%akycheck_moderator_bans%
```

Что они показывают:

| Placeholder | Значение |
| --- | --- |
| `%akycheck_active_checks%` | количество активных проверок |
| `%akycheck_moderator_checks%` | сколько проверок провёл модератор |
| `%akycheck_moderator_bans%` | сколько банов сделал модератор |

---

## Web-панель

В плагин встроена простая web-панель.  
Она показывает активные проверки и последние записи из `checks.log`.

Настройка в `config.yml`:

```yaml
web-dashboard:
  enabled: true
  host: '0.0.0.0'
  port: 8094
  token: 'change-me'
```

Открыть:

```text
http://IP_СЕРВЕРА:8094/?token=change-me
```

`token` лучше сразу поменять.

Пример:

```yaml
token: 'long-random-token-here'
```

---

## Логи и уведомления

AkyCheatCheck пишет журнал проверок в файл:

```text
plugins/AkyCheatCheck/logs/checks.log
```

Также можно включить Discord и Telegram.

```yaml
audit:
  file:
    enabled: true
  discord:
    enabled: false
    webhook-url: ''
  telegram:
    enabled: false
    bot-token: ''
    chat-id: ''
```

Все сетевые отправки идут асинхронно.

---

## Настройка античитов

В конфиге можно указать команды, которые выполняются при старте и конце проверки.

Пример:

```yaml
compatibility:
  enable-flight-while-frozen: true
  disable-paper-fly-kick: true
  commands-on-check-start:
    - 'grim exempt %player%'
    - 'vulcan bypass add %player%'
  commands-on-check-end:
    - 'grim unexempt %player%'
    - 'vulcan bypass remove %player%'
```

Так можно аккуратно работать рядом с GrimAC, Vulcan и похожими системами.

---

## Кабинки

Пример ручной настройки:

```yaml
cabins:
  - id: 'default'
    world: 'world'
    x: 0.5
    y: 100.0
    z: 0.5
    yaw: 0.0
    pitch: 0.0
```

Если кабинок несколько, плагин берёт свободную.  
Если все заняты, новая проверка не начнётся.

---

## API для разработчиков

Плагин вызывает события:

```java
AkyCheckStartEvent
AkyCheckEndEvent
```

Пример:

```java
@EventHandler
public void onCheckEnd(AkyCheckEndEvent event) {
    if (event.getReason() == CheckEndReason.RELEASED) {
        Bukkit.getLogger().info(event.getSuspectName() + " прошёл проверку");
    }
}
```

---

## Сборка

```bash
git clone https://github.com/USER/AkyCheatCheck.git
cd AkyCheatCheck
gradle build
```

Готовый jar будет здесь:

```text
build/libs/AkyCheatCheck-1.0.0.jar
```

Для сборки нужен JDK 17.

---

## Структура проекта

```text
ru.akydevv.akycheatcheck
├── api/event        # события для других плагинов
├── command          # команды
├── config           # чтение настроек
├── listener         # обработчики событий Bukkit
├── model            # модели данных
├── placeholder      # PlaceholderAPI expansion
├── repository       # хранение статистики
└── service          # логика плагина
```

Главный класс `AkyCheatCheckPlugin` только запускает и выключает части плагина.  
Основная логика вынесена в отдельные классы.

---

## Статус

Версия: `1.0.0`  
Сборка проверена на JDK 17.  
Целевая версия сервера: Paper / Spigot `1.20.4`.
