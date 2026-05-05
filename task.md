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

```sql
create table orders
(
    id    serial not null primary key,
    items text   not null
);

CREATE TABLE outbox
(
    id                serial       NOT NULL,
    payload           text         NOT NULL,
    type              varchar(255) NOT NULL,
    status            VARCHAR(255) NOT NULL,
    retry_count       INTEGER      NOT NULL default 0,
    created_at        TIMESTAMP WITHOUT TIME ZONE,
    next_execute_time TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_outbox PRIMARY KEY (id)
);
```