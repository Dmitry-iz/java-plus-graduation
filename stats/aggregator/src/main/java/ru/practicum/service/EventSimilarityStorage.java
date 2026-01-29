package ru.practicum.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventSimilarityStorage {

    // Map<EventId, Map<UserId, MaxWeight>> - веса действий пользователей с мероприятиями
    private final Map<Long, Map<Long, Double>> eventUserWeights = new ConcurrentHashMap<>();

    // Map<EventId, TotalWeight> - общие суммы весов каждого мероприятия
    private final Map<Long, Double> eventTotalWeights = new ConcurrentHashMap<>();

    // Map<FirstEventId, Map<SecondEventId, MinWeightSum>> - суммы минимальных весов для пар
    private final Map<Long, Map<Long, Double>> minWeightsSum = new ConcurrentHashMap<>();

    // Веса действий по ТЗ
    private static final double VIEW_WEIGHT = 1.0;
    private static final double REGISTER_WEIGHT = 2.0;
    private static final double LIKE_WEIGHT = 3.0;

    public double getActionWeight(String actionType) {
        return switch (actionType) {
            case "VIEW" -> VIEW_WEIGHT;
            case "REGISTER" -> REGISTER_WEIGHT;
            case "LIKE" -> LIKE_WEIGHT;
            default -> 0.0;
        };
    }

    // Получить текущий вес пользователя для мероприятия
    public double getUserEventWeight(Long userId, Long eventId) {
        Map<Long, Double> userWeights = eventUserWeights.get(eventId);
        if (userWeights == null) {
            return 0.0;
        }
        return userWeights.getOrDefault(userId, 0.0);
    }

    // Обновить вес пользователя для мероприятия (берем максимальный)
    public void updateUserEventWeight(Long userId, Long eventId, double newWeight) {
        Map<Long, Double> userWeights = eventUserWeights.computeIfAbsent(
                eventId, k -> new ConcurrentHashMap<>());

        Double currentWeight = userWeights.get(userId);
        if (currentWeight == null || newWeight > currentWeight) {
            userWeights.put(userId, newWeight);
            recalculateEventTotalWeight(eventId);
        }
    }

    // Пересчитать общий вес для мероприятия
    private void recalculateEventTotalWeight(Long eventId) {
        Map<Long, Double> userWeights = eventUserWeights.get(eventId);
        if (userWeights == null) {
            eventTotalWeights.put(eventId, 0.0);
            return;
        }

        double total = userWeights.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        eventTotalWeights.put(eventId, total);
    }

    // Получить общий вес мероприятия
    public double getEventTotalWeight(Long eventId) {
        return eventTotalWeights.getOrDefault(eventId, 0.0);
    }

    // Сохранить сумму минимальных весов для пары мероприятий (с упорядочиванием)
    public void putMinWeightSum(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        minWeightsSum
                .computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .put(second, sum);
    }

    // Получить сумму минимальных весов для пары мероприятий (с упорядочиванием)
    public double getMinWeightSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        Map<Long, Double> secondMap = minWeightsSum.get(first);
        if (secondMap == null) {
            return 0.0;
        }
        return secondMap.getOrDefault(second, 0.0);
    }

    // Пересчитать суммы минимальных весов для мероприятия со всеми остальными
    public void recalculateMinWeightsForEvent(Long updatedEventId) {
        Map<Long, Double> updatedUserWeights = eventUserWeights.get(updatedEventId);
        if (updatedUserWeights == null || updatedUserWeights.isEmpty()) {
            return;
        }

        for (Long otherEventId : eventUserWeights.keySet()) {
            if (!otherEventId.equals(updatedEventId)) {
                Map<Long, Double> otherUserWeights = eventUserWeights.get(otherEventId);
                if (otherUserWeights == null || otherUserWeights.isEmpty()) {
                    continue;
                }

                double sumMin = calculateMinWeightSum(updatedUserWeights, otherUserWeights);
                putMinWeightSum(updatedEventId, otherEventId, sumMin);
            }
        }
    }

    // Вычислить сумму минимальных весов для двух наборов пользовательских весов
    private double calculateMinWeightSum(Map<Long, Double> weights1, Map<Long, Double> weights2) {
        double sum = 0.0;

        // Итерируем по пользователям первого мероприятия
        for (Map.Entry<Long, Double> entry : weights1.entrySet()) {
            Long userId = entry.getKey();
            Double weight1 = entry.getValue();
            Double weight2 = weights2.get(userId);

            if (weight2 != null) {
                sum += Math.min(weight1, weight2);
            }
        }

        return sum;
    }

    // Получить все ID мероприятий
    public Set<Long> getAllEventIds() {
        return eventUserWeights.keySet();
    }

    // Получить пользовательские веса для мероприятия
    public Map<Long, Double> getUserWeightsForEvent(Long eventId) {
        return eventUserWeights.getOrDefault(eventId, new ConcurrentHashMap<>());
    }

    // Рассчитать сходство по формуле из ТЗ: similarity(ip,iq) = S_min(ip,iq) / sqrt(S_ip * S_iq)
    public double calculateSimilarity(Long eventId1, Long eventId2) {
        if (eventId1.equals(eventId2)) {
            return 1.0;
        }

        double sumMin = getMinWeightSum(eventId1, eventId2);
        double total1 = getEventTotalWeight(eventId1);
        double total2 = getEventTotalWeight(eventId2);

        if (total1 == 0.0 || total2 == 0.0) {
            return 0.0;
        }

        return sumMin / Math.sqrt(total1 * total2);
    }
}