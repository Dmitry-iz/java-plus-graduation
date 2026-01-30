//package ru.practicum.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
//import ru.practicum.ewm.stats.avro.UserActionAvro;
//import ru.practicum.model.EventSimilarity;
//import ru.practicum.model.UserInteraction;
//import ru.practicum.repository.EventSimilarityRepository;
//import ru.practicum.repository.UserInteractionRepository;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class DatabaseUpdater {
//
//    private final UserInteractionRepository userInteractionRepository;
//    private final EventSimilarityRepository eventSimilarityRepository;
//
//    private static final double VIEW_WEIGHT = 1.0;
//    private static final double REGISTER_WEIGHT = 2.0;
//    private static final double LIKE_WEIGHT = 3.0;
//
//    @Transactional
//    public void updateUserInteraction(UserActionAvro userAction) {
//        try {
//            LocalDateTime timestamp = LocalDateTime.ofInstant(
//                    userAction.getTimestamp(),
//                    ZoneId.systemDefault()
//            );
//
//            double weight = getWeight(userAction.getActionType().toString());
//
//            UserInteraction interaction = userInteractionRepository
//                    .findByUserIdAndEventId(userAction.getUserId(), userAction.getEventId())
//                    .orElseGet(() -> UserInteraction.builder()
//                            .userId(userAction.getUserId())
//                            .eventId(userAction.getEventId())
//                            .actionWeight(weight)
//                            .lastActionTimestamp(timestamp)
//                            .createdAt(LocalDateTime.now())
//                            .build());
//
//            if (weight > interaction.getActionWeight()) {
//                interaction.setActionWeight(weight);
//                interaction.setLastActionTimestamp(timestamp);
//            }
//
//            userInteractionRepository.save(interaction);
//            log.debug("Updated user interaction: userId={}, eventId={}, weight={}",
//                    userAction.getUserId(), userAction.getEventId(), weight);
//
//        } catch (Exception e) {
//            log.error("Error updating user interaction", e);
//        }
//    }
//
//    @Transactional
//    public void updateEventSimilarity(EventSimilarityAvro similarity) {
//        try {
//            LocalDateTime calculatedAt = LocalDateTime.ofInstant(
//                    similarity.getTimestamp(),
//                    ZoneId.systemDefault()
//            );
//
//            long eventA = similarity.getEventA();
//            long eventB = similarity.getEventB();
//
//            EventSimilarity eventSimilarity = eventSimilarityRepository
//                    .findByEventAAndEventB(eventA, eventB)
//                    .orElseGet(() -> EventSimilarity.builder()
//                            .eventA(eventA)
//                            .eventB(eventB)
//                            .similarityScore(similarity.getScore())
//                            .calculatedAt(calculatedAt)
//                            .build());
//
//            eventSimilarity.setSimilarityScore(similarity.getScore());
//            eventSimilarity.setCalculatedAt(calculatedAt);
//
//            eventSimilarityRepository.save(eventSimilarity);
//            log.debug("Updated event similarity: {} - {} = {}",
//                    eventA, eventB, similarity.getScore());
//
//        } catch (Exception e) {
//            log.error("Error updating event similarity", e);
//        }
//    }
//
//    private double getWeight(String actionType) {
//        return switch (actionType) {
//            case "VIEW" -> VIEW_WEIGHT;
//            case "REGISTER" -> REGISTER_WEIGHT;
//            case "LIKE" -> LIKE_WEIGHT;
//            default -> 0.0;
//        };
//    }
//}

package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.EventSimilarityId;
import ru.practicum.model.UserInteraction;
import ru.practicum.model.UserInteractionId;
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

    @Transactional
    public void updateUserInteraction(UserActionAvro userAction) {
        try {
            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    userAction.getTimestamp(),
                    ZoneId.systemDefault()
            );

            double weight = getWeight(userAction.getActionType().toString());

            var existing = userInteractionRepository.findByUserAndEvent(
                    userAction.getUserId(),
                    userAction.getEventId()
            );

            if (existing.isPresent()) {
                UserInteraction interaction = existing.get();
                if (weight > interaction.getUserScore()) {
                    interaction.setUserScore(weight);
                    interaction.setTimestampAction(timestamp);
                }
            } else {
                UserInteraction newInteraction = UserInteraction.builder()
                        .userId(userAction.getUserId())
                        .eventId(userAction.getEventId())
                        .userScore(weight)
                        .timestampAction(timestamp)
                        .build();
                userInteractionRepository.save(newInteraction);
            }

        } catch (Exception e) {
            log.error("Error updating user interaction", e);
        }
    }

    @Transactional
    public void updateEventSimilarity(EventSimilarityAvro similarity) {
        try {
            long eventA = similarity.getEventA();
            long eventB = similarity.getEventB();

            long first = Math.min(eventA, eventB);
            long second = Math.max(eventA, eventB);

            var existing = eventSimilarityRepository.findByOrderedEvents(first, second);

            if (existing.isPresent()) {
                EventSimilarity eventSimilarity = existing.get();
                eventSimilarity.setScore(similarity.getScore());
            } else {
                EventSimilarity newSimilarity = EventSimilarity.builder()
                        .firstEvent(first)
                        .secondEvent(second)
                        .score(similarity.getScore())
                        .build();
                eventSimilarityRepository.save(newSimilarity);
            }

        } catch (Exception e) {
            log.error("Error updating event similarity", e);
        }
    }

    private double getWeight(String actionType) {
        return switch (actionType) {
            case "VIEW" -> 1.0;
            case "REGISTER" -> 2.0;
            case "LIKE" -> 3.0;
            default -> 0.0;
        };
    }
}