package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CollectorStarter {

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("Collector service started and ready");
        log.info("gRPC UserActionController available on random port");
        log.info("Producing to Kafka topic: stats.user-actions.v1");
        log.info("Action types supported: VIEW, REGISTER, LIKE");
    }
}