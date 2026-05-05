package org.example.outboxdemo.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.example.outboxdemo.entity.Order;
import org.example.outboxdemo.entity.Outbox;
import org.example.outboxdemo.entity.OutboxType;
import org.example.outboxdemo.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository repository;
  private final OutboxService outboxService;

  @Transactional
  public Order saveOrder(Order order) {
    Order saved = repository.save(order);
    Instant now = Instant.now();
    Outbox outbox = Outbox.create(OutboxType.SEND_TO_KAFKA, saved.getId().toString(), now);
    outboxService.save(outbox);
    return saved;
  }
}
