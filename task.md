## Урок 1 Скелет проекта Outbox

1. В компании используется событийная архитектура с Apache Kafka.
Сервис заказов должен публиковать события (order_created), но текущая реализация ненадёжна:

иногда данные сохраняются в БД, но событие не отправляется
иногда событие отправляется, но данные не сохраняются

Ваша задача — устранить эту проблему.

Реализовать сервис заказов с использованием Transactional Outbox pattern, обеспечив:

- отсутствие потери событий
- устойчивость к сбоям
- корректную обработку повторных отправок


Реализовать REST API на Spring Boot:

```Endpoint:
POST /orders
```

Поведение:
- создаёт заказ
- сохраняет заказ в БД
- записывает событие в outbox

2. База данных

Использовать PostgreSQL (или аналог).

Таблица orders:
id
description
created_at

Таблица outbox:
id
event_type
payload (text)
status (NEW, SENT, FAILED)
retry_count
created_at
next_execute_time

3. Транзакционность

Сохранение:

заказа
записи в outbox

должно происходить в рамках одной транзакции.

Если что-то падает — не сохраняется ничего.

4. Outbox Processor (worker)

Реализовать отдельный компонент:

Поведение:
периодически читает записи со статусом NEW
отправляет события в Kafka
обновляет статус:
SENT при успехе
FAILED при ошибке


![img.png](img.png)

orderCreated and saved to db -> send_kafka_message -> payment_micro_service/notification_micro_service

OUTBOX_TABLE
sent_to_kafka | payload(order) | attempts | policy | error | error_type

outbox_processor -> getTask() -> for each try to execute it


outbox_kafka_processor -> getTask (send_to_kafka) -> callKafkaProducer()

---

## Урок 2. Реализация Outbox Processor

На первом уроке мы сохраняем заказ и запись в outbox в одной транзакции.
Теперь нужно реализовать воркер, который эти записи обрабатывает.

### Задание 1. Архитектура: один процессор — один тип задачи

Посмотрите на поле `type` в таблице `outbox` и на enum `OutboxType`.

Сейчас есть только один тип: `SEND_TO_KAFKA`. В будущем могут появиться другие:
`SEND_EMAIL`, `CALL_WEBHOOK` и т.д. — каждый со своей логикой обработки.

Создать интерфейс `OutboxHandler`:

```java
public interface OutboxHandler {
    OutboxType getType();
    void handle(Outbox outbox);
}
```

Реализовать `KafkaOutboxHandler implements OutboxHandler`:
- `getType()` возвращает `OutboxType.SEND_TO_KAFKA`
- `handle()` пока просто логирует: `"Sending to Kafka: payload={}"` 

Подумайте: зачем разделять обработчики по типу, а не писать один большой `if/else`?
Что будет, если в `outbox` окажется запись с типом, для которого нет обработчика?

---

### Задание 2. Запрос на выборку pending-записей

В `OutboxRepository` добавить метод, который возвращает до `N` записей со статусом `NEW`,
у которых `next_execute_time <= текущее время`.

Использовать JPQL или нативный SQL — на выбор.

```java
List<Outbox> findPendingBatch(Instant now, int limit);
```

Подумайте: зачем нужен `next_execute_time`, если мы уже фильтруем по `status = NEW`?

---

### Задание 3. Реализация processBatch()

В `OutboxService` реализовать метод `processBatch(int batchSize)`.

Поведение:
- получить batch pending-записей из репозитория
- для каждой записи найти нужный `OutboxHandler` по `outbox.getType()` и вызвать `handle()`
- если `handle()` прошёл успешно — обновить статус на `SENT`
- если выбросил исключение — обновить статус на `FAILED`

Каждое событие обрабатывается и сохраняется **в отдельной транзакции**.
Почему нельзя обрабатывать весь batch в одной транзакции?

---

### Задание 4. Retry с задержкой

Вместо того чтобы сразу переводить в `FAILED`, реализовать retry:

- максимальное число попыток: 3 (вынести в константу)
- при ошибке: инкрементировать `retryCount`, отложить `nextExecuteTime` на `retryCount * 10` секунд
- перевести в `FAILED` только когда `retryCount >= MAX_RETRIES`

Проверить вручную: добавить в `handle()` случайный бросок исключения и убедиться, что запись не переходит сразу в `FAILED`, а делает несколько попыток.

---

### Задание 5. (По желанию) Защита от конкурентной обработки (SELECT FOR UPDATE SKIP LOCKED)

Представьте, что запущено два экземпляра сервиса. Оба одновременно читают pending-записи и получают одни и те же строки — событие отправится дважды.

Исправить запрос в `OutboxRepository`, добавив пессимистичную блокировку с `SKIP LOCKED`.

`SKIP LOCKED` означает: если строка уже заблокирована другой транзакцией — пропустить её, а не ждать.

Можно использовать нативный запрос или аннотацию `@Lock(LockModeType.PESSIMISTIC_WRITE)`.

---

### Итог урока

После выполнения всех заданий система должна:

1. Принимать POST /order → атомарно сохранять заказ + outbox-запись
2. Каждые 5 секунд процессор забирает до 10 pending-записей
3. Для каждой записи находит нужный обработчик по типу и вызывает его
4. При сбое — retry с backoff, после 3 попыток — FAILED
5. Дополнительно: Два параллельных процессора не берут одну и ту же запись