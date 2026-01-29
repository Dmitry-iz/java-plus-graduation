package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String USER_ACTIONS_TOPIC = "stats.user-actions.v1";

    public void sendUserAction(Long userId, Long eventId, ActionTypeAvro actionType, Instant timestamp) {
        UserActionAvro userAction = UserActionAvro.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(timestamp)
                .build();

        try {
            String key = userId + "_" + eventId;
            kafkaTemplate.send(USER_ACTIONS_TOPIC, key, userAction)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send user action to Kafka topic {}: {}",
                                    USER_ACTIONS_TOPIC, ex.getMessage());
                        } else {
                            log.debug("Successfully sent user action to Kafka: userId={}, eventId={}, action={}, offset={}",
                                    userId, eventId, actionType, result.getRecordMetadata().offset());
                        }
                    });

        } catch (Exception e) {
            log.error("Error sending user action to Kafka: userId={}, eventId={}", userId, eventId, e);
            throw new RuntimeException("Failed to send user action to Kafka", e);
        }
    }

    public void sendViewAction(Long userId, Long eventId) {
        sendUserAction(userId, eventId, ActionTypeAvro.VIEW, Instant.now());
    }

    public void sendRegisterAction(Long userId, Long eventId) {
        sendUserAction(userId, eventId, ActionTypeAvro.REGISTER, Instant.now());
    }

    public void sendLikeAction(Long userId, Long eventId) {
        sendUserAction(userId, eventId, ActionTypeAvro.LIKE, Instant.now());
    }
}