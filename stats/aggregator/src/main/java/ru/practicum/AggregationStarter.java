package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("Aggregator service started and ready to process messages");
        log.info("Listening to topic: stats.user-actions.v1");
        log.info("Producing to topic: stats.events-similarity.v1");
    }
}