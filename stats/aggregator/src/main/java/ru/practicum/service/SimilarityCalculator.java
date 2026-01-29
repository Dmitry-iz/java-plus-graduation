package ru.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SimilarityCalculator {

    private final Map<Long, Map<Long, Double>> eventUserWeights = new ConcurrentHashMap<>();

    private final Map<Long, Double> eventTotalWeights = new ConcurrentHashMap<>();

    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    private final Set<Long> allEventIds = ConcurrentHashMap.newKeySet();

    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    public void processUserAction(Long userId, Long eventId, String actionType) {
        double weight = getActionWeight(actionType);

        updateUserWeight(eventId, userId, weight);

        recalculateSimilarities(eventId);
    }

    private void updateUserWeight(Long eventId, Long userId, double newWeight) {
        eventUserWeights.compute(eventId, (eId, userWeightsMap) -> {
            if (userWeightsMap == null) {
                userWeightsMap = new ConcurrentHashMap<>();
            }

            Double currentWeight = userWeightsMap.get(userId);

            if (currentWeight == null || newWeight > currentWeight) {
                userWeightsMap.put(userId, newWeight);

                updateTotalWeight(eventId, currentWeight, newWeight);

                updateMinWeights(eventId, userId, currentWeight, newWeight);
            }

            return userWeightsMap;
        });

        allEventIds.add(eventId);
    }

    private void updateTotalWeight(Long eventId, Double oldWeight, Double newWeight) {
        eventTotalWeights.compute(eventId, (eId, total) -> {
            if (total == null) {
                total = 0.0;
            }

            if (oldWeight != null) {
                total -= oldWeight;
            }
            total += newWeight;

            return total;
        });
    }

    private void updateMinWeights(Long updatedEventId, Long userId, Double oldWeight, Double newWeight) {
        for (Long otherEventId : allEventIds) {
            if (!otherEventId.equals(updatedEventId)) {
                Map<Long, Double> otherEventWeights = eventUserWeights.get(otherEventId);
                if (otherEventWeights != null) {
                    Double otherWeight = otherEventWeights.get(userId);
                    if (otherWeight != null) {
                        updatePairMinWeight(updatedEventId, otherEventId, userId,
                                oldWeight, newWeight, otherWeight);
                    }
                }
            }
        }
    }

    private void updatePairMinWeight(Long eventA, Long eventB, Long userId,
                                     Double oldWeightA, Double newWeightA, Double weightB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        final double oldMin = (oldWeightA != null) ? Math.min(oldWeightA, weightB) : 0.0;
        final double newMin = Math.min(newWeightA, weightB);
        final double delta = newMin - oldMin;

        minWeightsSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, (existing, deltaValue) -> existing + deltaValue);
    }

    private void recalculateSimilarities(Long updatedEventId) {
        for (Long otherEventId : allEventIds) {
            if (!otherEventId.equals(updatedEventId)) {
                calculateSimilarity(updatedEventId, otherEventId);
            }
        }
    }

    public double calculateSimilarity(Long eventId1, Long eventId2) {
        long first = Math.min(eventId1, eventId2);
        long second = Math.max(eventId1, eventId2);

        Double minSum = getMinWeightSum(first, second);
        Double total1 = eventTotalWeights.getOrDefault(first, 0.0);
        Double total2 = eventTotalWeights.getOrDefault(second, 0.0);

        if (total1 == 0.0 || total2 == 0.0 || minSum == 0.0) {
            return 0.0;
        }

        return minSum / Math.sqrt(total1 * total2);
    }

    private Double getMinWeightSum(long eventId1, long eventId2) {
        long first = Math.min(eventId1, eventId2);
        long second = Math.max(eventId1, eventId2);

        Map<Long, Double> secondMap = minWeightsSums.get(first);
        if (secondMap == null) {
            return 0.0;
        }

        return secondMap.getOrDefault(second, 0.0);
    }

    private double getActionWeight(String actionType) {
        return switch (actionType) {
            case "VIEW" -> VIEW_WEIGHT;
            case "REGISTER" -> REGISTER_WEIGHT;
            case "LIKE" -> LIKE_WEIGHT;
            default -> 0.0;
        };
    }

    public Set<Long> getAllEventIds() {
        return new HashSet<>(allEventIds);
    }

    public Double getMinSumForPair(Long eventId1, Long eventId2) {
        return getMinWeightSum(eventId1, eventId2);
    }

    public Double getTotalWeight(Long eventId) {
        return eventTotalWeights.getOrDefault(eventId, 0.0);
    }
}