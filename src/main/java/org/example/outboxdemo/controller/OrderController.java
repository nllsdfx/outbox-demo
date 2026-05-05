package org.example.outboxdemo.controller;

import lombok.RequiredArgsConstructor;
import org.example.outboxdemo.service.OrderService;
import org.example.outboxdemo.entity.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class OrderController {

  private final OrderService service;

  @PostMapping("/order")
  public ResponseEntity<Order> createOrder(@RequestBody Order order) {
    return ResponseEntity.ok(service.saveOrder(order));
  }

}
