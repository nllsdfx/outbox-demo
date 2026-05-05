package org.example.outboxdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Entity
@Table(name = "outbox")
@Getter
@Setter
@Accessors(chain = true)
public class Outbox {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private OutboxType type;

  @Column(name = "payload")
  private String payload;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private OutboxStatus status;

  @Column(name = "retry_count")
  private Integer retryCount;

  @Column(name = "created_at")
  private Instant createAt;

  @Column(name = "next_execute_time")
  private Instant nextExecuteTime;

  public static Outbox create(OutboxType outboxType, String items, Instant now) {
    return new Outbox()
        .setCreateAt(now)
        .setPayload(items)
        .setType(outboxType)
        .setStatus(OutboxStatus.NEW)
        .setRetryCount(0)
        .setNextExecuteTime(now.plus(2, ChronoUnit.SECONDS));
  }
}
