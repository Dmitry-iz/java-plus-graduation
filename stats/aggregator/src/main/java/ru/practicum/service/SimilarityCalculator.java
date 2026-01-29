package ru.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SimilarityCalculator {

    // 1. Веса действий пользователей с мероприятиями: Map<EventId, Map<UserId, MaxWeight>>
    private final Map<Long, Map<Long, Double>> eventUserWeights = new ConcurrentHashMap<>();

    // 2. Общие суммы весов для каждого мероприятия: Map<EventId, TotalWeight>
    private final Map<Long, Double> eventTotalWeights = new ConcurrentHashMap<>();

    // 3. Суммы минимальных весов для пар мероприятий: Map<EventId, Map<EventId, MinWeightSum>>
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    // 4. Все ID мероприятий
    private final Set<Long> allEventIds = ConcurrentHashMap.newKeySet();

    // Веса действий по ТЗ
    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    /**
     * Обработка действия пользователя по ТЗ
     */
    public void processUserAction(Long userId, Long eventId, String actionType) {
        double weight = getActionWeight(actionType);

        // Обновляем веса действий
        updateUserWeight(eventId, userId, weight);

        // Пересчитываем сходства с другими мероприятиями
        recalculateSimilarities(eventId);
    }

    /**
     * Обновление максимального веса действия пользователя для мероприятия
     */
    private void updateUserWeight(Long eventId, Long userId, double newWeight) {
        eventUserWeights.compute(eventId, (eId, userWeightsMap) -> {
            if (userWeightsMap == null) {
                userWeightsMap = new ConcurrentHashMap<>();
            }

            Double currentWeight = userWeightsMap.get(userId);

            // Берем максимальный вес (по ТЗ)
            if (currentWeight == null || newWeight > currentWeight) {
                userWeightsMap.put(userId, newWeight);

                // Обновляем общую сумму весов мероприятия
                updateTotalWeight(eventId, currentWeight, newWeight);

                // Обновляем суммы минимальных весов с другими мероприятиями
                updateMinWeights(eventId, userId, currentWeight, newWeight);
            }

            return userWeightsMap;
        });

        allEventIds.add(eventId);
    }

    /**
     * Обновление общей суммы весов мероприятия
     */
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

    /**
     * Обновление сумм минимальных весов для пар мероприятий
     */
    private void updateMinWeights(Long updatedEventId, Long userId, Double oldWeight, Double newWeight) {
        // Находим все мероприятия, с которыми взаимодействовал этот пользователь
        for (Long otherEventId : allEventIds) {
            if (!otherEventId.equals(updatedEventId)) {
                Map<Long, Double> otherEventWeights = eventUserWeights.get(otherEventId);
                if (otherEventWeights != null) {
                    Double otherWeight = otherEventWeights.get(userId);
                    if (otherWeight != null) {
                        // Обновляем сумму минимальных весов для пары
                        updatePairMinWeight(updatedEventId, otherEventId, userId,
                                oldWeight, newWeight, otherWeight);
                    }
                }
            }
        }
    }

    /**
     * Обновление суммы минимальных весов для конкретной пары мероприятий
     */
    private void updatePairMinWeight(Long eventA, Long eventB, Long userId,
                                     Double oldWeightA, Double newWeightA, Double weightB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        final double oldMin = (oldWeightA != null) ? Math.min(oldWeightA, weightB) : 0.0;
        final double newMin = Math.min(newWeightA, weightB);
        final double delta = newMin - oldMin;  // вычисляем дельту

        // Используем merge для атомарного обновления
        minWeightsSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, (existing, deltaValue) -> existing + deltaValue);
    }

    /**
     * Пересчет сходств для мероприятия после обновления
     */
    private void recalculateSimilarities(Long updatedEventId) {
        for (Long otherEventId : allEventIds) {
            if (!otherEventId.equals(updatedEventId)) {
                calculateSimilarity(updatedEventId, otherEventId);
            }
        }
    }

    /**
     * Расчет косинусного сходства по формуле из ТЗ
     * similarity(A, B) = S_min(A, B) / sqrt(S_A * S_B)
     */
    public double calculateSimilarity(Long eventId1, Long eventId2) {
        long first = Math.min(eventId1, eventId2);
        long second = Math.max(eventId1, eventId2);

        Double minSum = getMinWeightSum(first, second);
        Double total1 = eventTotalWeights.getOrDefault(first, 0.0);
        Double total2 = eventTotalWeights.getOrDefault(second, 0.0);

        if (total1 == 0.0 || total2 == 0.0 || minSum == 0.0) {
            return 0.0;
        }

        // Формула косинусного сходства из ТЗ
        return minSum / Math.sqrt(total1 * total2);
    }

    /**
     * Получение суммы минимальных весов для пары мероприятий
     */
    private Double getMinWeightSum(long eventId1, long eventId2) {
        long first = Math.min(eventId1, eventId2);
        long second = Math.max(eventId1, eventId2);

        Map<Long, Double> secondMap = minWeightsSums.get(first);
        if (secondMap == null) {
            return 0.0;
        }

        return secondMap.getOrDefault(second, 0.0);
    }

    /**
     * Получение веса действия по типу
     */
    private double getActionWeight(String actionType) {
        return switch (actionType) {
            case "VIEW" -> VIEW_WEIGHT;
            case "REGISTER" -> REGISTER_WEIGHT;
            case "LIKE" -> LIKE_WEIGHT;
            default -> 0.0;
        };
    }

    /**
     * Получение всех ID мероприятий
     */
    public Set<Long> getAllEventIds() {
        return new HashSet<>(allEventIds);
    }

    /**
     * Получение суммы минимальных весов для пары (для тестирования)
     */
    public Double getMinSumForPair(Long eventId1, Long eventId2) {
        return getMinWeightSum(eventId1, eventId2);
    }

    /**
     * Получение общей суммы весов мероприятия (для тестирования)
     */
    public Double getTotalWeight(Long eventId) {
        return eventTotalWeights.getOrDefault(eventId, 0.0);
    }
}