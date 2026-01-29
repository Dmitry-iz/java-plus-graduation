package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerStarter {

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("Analyzer service started and ready");
        log.info("Listening to topics: stats.user-actions.v1, stats.events-similarity.v1");
        log.info("gRPC RecommendationsController available");
    }
}