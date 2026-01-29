package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.UserInteraction;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserInteractionRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseUpdater {

    private final UserInteractionRepository userInteractionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    private static final double VIEW_WEIGHT = 1.0;
    private static final double REGISTER_WEIGHT = 2.0;
    private static final double LIKE_WEIGHT = 3.0;

    @Transactional
    public void updateUserInteraction(UserActionAvro userAction) {
        try {
            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    userAction.getTimestamp(),
                    ZoneId.systemDefault()
            );

            double weight = getWeight(userAction.getActionType().toString());

            UserInteraction interaction = userInteractionRepository
                    .findByUserIdAndEventId(userAction.getUserId(), userAction.getEventId())
                    .orElseGet(() -> UserInteraction.builder()
                            .userId(userAction.getUserId())
                            .eventId(userAction.getEventId())
                            .actionWeight(weight)
                            .lastActionTimestamp(timestamp)
                            .createdAt(LocalDateTime.now())
                            .build());

            if (weight > interaction.getActionWeight()) {
                interaction.setActionWeight(weight);
                interaction.setLastActionTimestamp(timestamp);
            }

            userInteractionRepository.save(interaction);
            log.debug("Updated user interaction: userId={}, eventId={}, weight={}",
                    userAction.getUserId(), userAction.getEventId(), weight);

        } catch (Exception e) {
            log.error("Error updating user interaction", e);
        }
    }

    @Transactional
    public void updateEventSimilarity(EventSimilarityAvro similarity) {
        try {
            LocalDateTime calculatedAt = LocalDateTime.ofInstant(
                    similarity.getTimestamp(),
                    ZoneId.systemDefault()
            );

            long eventA = similarity.getEventA();
            long eventB = similarity.getEventB();

            EventSimilarity eventSimilarity = eventSimilarityRepository
                    .findByEventAAndEventB(eventA, eventB)
                    .orElseGet(() -> EventSimilarity.builder()
                            .eventA(eventA)
                            .eventB(eventB)
                            .similarityScore(similarity.getScore())
                            .calculatedAt(calculatedAt)
                            .build());

            eventSimilarity.setSimilarityScore(similarity.getScore());
            eventSimilarity.setCalculatedAt(calculatedAt);

            eventSimilarityRepository.save(eventSimilarity);
            log.debug("Updated event similarity: {} - {} = {}",
                    eventA, eventB, similarity.getScore());

        } catch (Exception e) {
            log.error("Error updating event similarity", e);
        }
    }

    private double getWeight(String actionType) {
        return switch (actionType) {
            case "VIEW" -> VIEW_WEIGHT;
            case "REGISTER" -> REGISTER_WEIGHT;
            case "LIKE" -> LIKE_WEIGHT;
            default -> 0.0;
        };
    }
}