package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final DatabaseUpdater databaseUpdater;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "analyzer-user-actions")
    public void consumeUserAction(UserActionAvro userAction) {
        log.info("User action: userId={}, eventId={}, action={}",
                userAction.getUserId(), userAction.getEventId(), userAction.getActionType());
        databaseUpdater.updateUserInteraction(userAction);
    }

    @KafkaListener(topics = "stats.events-similarity.v1", groupId = "analyzer-event-similarities")
    public void consumeEventSimilarity(EventSimilarityAvro similarity) {
        log.info("Event similarity: {} - {} = {}",
                similarity.getEventA(), similarity.getEventB(), similarity.getScore());
        databaseUpdater.updateEventSimilarity(similarity);
    }
}