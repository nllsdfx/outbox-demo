package org.example.outboxdemo.service;

import lombok.RequiredArgsConstructor;
import org.example.outboxdemo.entity.Outbox;
import org.example.outboxdemo.repository.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {

  private final OutboxRepository outboxRepository;

  @Transactional
  public Outbox save(Outbox outbox) {
    return outboxRepository.save(outbox);
  }
}
