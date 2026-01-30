package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.UserInteraction;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserInteractionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserInteractionRepository userInteractionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    private static final int DEFAULT_K_NEIGHBORS = 10;
    private static final int DEFAULT_RECENT_EVENTS = 20;

    @Transactional(readOnly = true)
    public List<EventSimilarity> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        Pageable pageable = PageRequest.of(0, maxResults * 2);

        List<EventSimilarity> similarities = eventSimilarityRepository
                .findSimilarEvents(eventId, pageable);

        if (similarities.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserInteraction> userInteractions = userInteractionRepository
                .findByUserId(userId);

        Set<Long> userEventIds = userInteractions.stream()
                .map(UserInteraction::getEventId)
                .collect(Collectors.toSet());

        List<EventSimilarity> filtered = similarities.stream()
                .filter(similarity -> {
                    Long otherEvent = similarity.getEventA().equals(eventId)
                            ? similarity.getEventB()
                            : similarity.getEventA();
                    return !userEventIds.contains(otherEvent);
                })
                .limit(maxResults)
                .collect(Collectors.toList());

        log.debug("Found {} similar events for event {} excluding user {} events",
                filtered.size(), eventId, userId);

        return filtered;
    }

    @Transactional(readOnly = true)
    public Map<Long, Double> getRecommendationsForUser(Long userId, int maxResults) {
        Pageable recentPageable = PageRequest.of(0, DEFAULT_RECENT_EVENTS);
        List<UserInteraction> recentInteractions = userInteractionRepository
                .findRecentByUserId(userId, recentPageable);

        if (recentInteractions.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> userEventIds = recentInteractions.stream()
                .map(UserInteraction::getEventId)
                .collect(Collectors.toSet());

        Set<Long> candidateEvents = new HashSet<>();
        for (UserInteraction interaction : recentInteractions) {
            Long eventId = interaction.getEventId();

            Pageable similarPageable = PageRequest.of(0, DEFAULT_K_NEIGHBORS);
            List<EventSimilarity> similarities = eventSimilarityRepository
                    .findSimilarEventsExcluding(eventId, new ArrayList<>(userEventIds), similarPageable);

            for (EventSimilarity similarity : similarities) {
                Long similarEvent = similarity.getEventA().equals(eventId)
                        ? similarity.getEventB()
                        : similarity.getEventA();
                candidateEvents.add(similarEvent);

                if (candidateEvents.size() >= maxResults * 2) {
                    break;
                }
            }

            if (candidateEvents.size() >= maxResults * 2) {
                break;
            }
        }

        Map<Long, Double> recommendations = new HashMap<>();
        for (Long candidateEvent : candidateEvents) {
            double predictedScore = predictEventScore(userId, candidateEvent, userEventIds);
            if (predictedScore > 0) {
                recommendations.put(candidateEvent, predictedScore);
            }

            if (recommendations.size() >= maxResults) {
                break;
            }
        }

        return recommendations.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private double predictEventScore(Long userId, Long targetEventId, Set<Long> userEventIds) {
        List<EventSimilarity> nearestNeighbors = new ArrayList<>();

        for (Long userEventId : userEventIds) {
            eventSimilarityRepository
                    .findSimilarityBetweenEvents(userEventId, targetEventId)
                    .ifPresent(nearestNeighbors::add);
        }

        if (nearestNeighbors.isEmpty()) {
            return 0.0;
        }

        nearestNeighbors.sort(Comparator.comparing(EventSimilarity::getSimilarityScore).reversed());

        List<EventSimilarity> kNeighbors = nearestNeighbors.stream()
                .limit(DEFAULT_K_NEIGHBORS)
                .collect(Collectors.toList());

        Map<Long, Double> userRatings = new HashMap<>();
        List<UserInteraction> userInteractions = userInteractionRepository
                .findByUserIdAndEventIds(userId, new ArrayList<>(userEventIds));

        for (UserInteraction interaction : userInteractions) {
            userRatings.put(interaction.getEventId(), interaction.getActionWeight());
        }

        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (EventSimilarity neighbor : kNeighbors) {
            Long neighborEventId = neighbor.getEventA().equals(targetEventId)
                    ? neighbor.getEventB()
                    : neighbor.getEventA();

            Double userRating = userRatings.get(neighborEventId);
            if (userRating != null) {
                weightedSum += neighbor.getSimilarityScore() * userRating;
                similaritySum += neighbor.getSimilarityScore();
            }
        }

        if (similaritySum == 0.0) {
            return 0.0;
        }

        return weightedSum / similaritySum;
    }

    @Transactional(readOnly = true)
    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        Map<Long, Double> result = new HashMap<>();

        if (eventIds == null || eventIds.isEmpty()) {
            log.warn("Empty eventIds list provided");
            return result;
        }

        log.debug("Getting interactions count for eventIds: {}", eventIds);

        List<Object[]> totalWeights = userInteractionRepository
                .getTotalWeightsByEventIds(eventIds);

        log.debug("Raw SQL results: {}", totalWeights);

        for (Long eventId : eventIds) {
            result.put(eventId, 0.0);
        }

        if (!totalWeights.isEmpty()) {
            Object firstResult = totalWeights.get(0);
            if (firstResult != null && firstResult instanceof Object[]) {
                Object[] row = (Object[]) firstResult;
                if (row.length > 0) {
                    Double totalSum = (Double) row[0];
                    if (totalSum != null) {
                        log.warn("Query returns single sum for all events: {}", totalSum);
                        if (!eventIds.isEmpty()) {
                            result.put(eventIds.get(0), totalSum);
                        }
                    }
                }
            }
        }

        log.debug("Final interactions count result: {}", result);
        return result;
    }
}