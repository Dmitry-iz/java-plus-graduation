package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.SimilarityCalculator;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final SimilarityCalculator similarityCalculator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String SIMILARITY_TOPIC = "stats.events-similarity.v1";

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "aggregator-group")
    public void consumeUserAction(UserActionAvro userAction) {
        log.info("Processing user action: userId={}, eventId={}, actionType={}",
                userAction.getUserId(), userAction.getEventId(), userAction.getActionType());

        try {
            String actionType = userAction.getActionType().toString();

            // Обрабатываем действие
            similarityCalculator.processUserAction(
                    userAction.getUserId(),
                    userAction.getEventId(),
                    actionType
            );

            // Рассчитываем и отправляем обновленные сходства
            sendUpdatedSimilarities(userAction.getEventId());

        } catch (Exception e) {
            log.error("Error processing user action", e);
        }
    }

    private void sendUpdatedSimilarities(Long updatedEventId) {
        for (Long otherEventId : similarityCalculator.getAllEventIds()) {
            if (!otherEventId.equals(updatedEventId)) {
                double similarity = similarityCalculator.calculateSimilarity(updatedEventId, otherEventId);

                if (similarity > 0) {
                    EventSimilarityAvro similarityEvent = createSimilarityEvent(
                            updatedEventId, otherEventId, similarity);

                    String key = getEventPairKey(updatedEventId, otherEventId);
                    kafkaTemplate.send(SIMILARITY_TOPIC, key, similarityEvent);

                    log.debug("Sent similarity: {} - {} = {}",
                            updatedEventId, otherEventId, similarity);
                }
            }
        }
    }

    private EventSimilarityAvro createSimilarityEvent(Long eventId1, Long eventId2, double score) {
        long first = Math.min(eventId1, eventId2);
        long second = Math.max(eventId1, eventId2);

        return EventSimilarityAvro.newBuilder()
                .setEventA(first)
                .setEventB(second)
                .setScore(score)
                .setTimestamp(Instant.now())  // Используем Instant
                .build();
    }

    private String getEventPairKey(Long eventId1, Long eventId2) {
        long first = Math.min(eventId1, eventId2);
        long second = Math.max(eventId1, eventId2);
        return first + "_" + second;
    }
}