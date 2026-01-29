package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.DatabaseUpdater;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerConsumer {

    private final DatabaseUpdater databaseUpdater;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "analyzer-group")
    public void consumeUserAction(UserActionAvro userAction) {
        log.info("Updating user interaction: userId={}, eventId={}, action={}",
                userAction.getUserId(), userAction.getEventId(), userAction.getActionType());

        databaseUpdater.updateUserInteraction(userAction);
    }

    @KafkaListener(topics = "stats.events-similarity.v1", groupId = "analyzer-group")
    public void consumeEventSimilarity(EventSimilarityAvro similarity) {
        log.info("Updating event similarity: {} - {} = {}",
                similarity.getEventA(), similarity.getEventB(), similarity.getScore());

        databaseUpdater.updateEventSimilarity(similarity);
    }
}