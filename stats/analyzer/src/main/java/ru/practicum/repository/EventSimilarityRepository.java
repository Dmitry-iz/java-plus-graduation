package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.EventSimilarity;

import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);

    // ИСПРАВИТЬ: Убрать этот метод или изменить на правильный
    // findByEventAOrEventB(Long eventId) ← НЕПРАВИЛЬНО

    // ВМЕСТО ЭТОГО добавить правильные методы:
    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventId OR es.eventB = :eventId")
    List<EventSimilarity> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT es FROM EventSimilarity es WHERE (es.eventA = :eventId OR es.eventB = :eventId) " +
            "ORDER BY es.similarityScore DESC")
    List<EventSimilarity> findSimilarEvents(@Param("eventId") Long eventId,
                                            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventA AND es.eventB = :eventB " +
            "OR es.eventA = :eventB AND es.eventB = :eventA")
    Optional<EventSimilarity> findSimilarityBetweenEvents(@Param("eventA") Long eventA,
                                                          @Param("eventB") Long eventB);

    @Query("SELECT es FROM EventSimilarity es WHERE (es.eventA = :eventId OR es.eventB = :eventId) " +
            "AND (es.eventA NOT IN :excludedEvents AND es.eventB NOT IN :excludedEvents) " +
            "ORDER BY es.similarityScore DESC")
    List<EventSimilarity> findSimilarEventsExcluding(@Param("eventId") Long eventId,
                                                     @Param("excludedEvents") List<Long> excludedEvents,
                                                     org.springframework.data.domain.Pageable pageable);
}