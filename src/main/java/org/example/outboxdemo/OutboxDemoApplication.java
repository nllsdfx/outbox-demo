package org.example.outboxdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@EnableJpaRepositories
@SpringBootApplication
public class OutboxDemoApplication {

  static void main(String[] args) {
    SpringApplication.run(OutboxDemoApplication.class, args);
  }

}
