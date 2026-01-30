//package ru.practicum.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import ru.practicum.model.EventSimilarity;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {
//
//    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);
//
//    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventId OR es.eventB = :eventId")
//    List<EventSimilarity> findByEventId(@Param("eventId") Long eventId);
//
//    @Query("SELECT es FROM EventSimilarity es WHERE (es.eventA = :eventId OR es.eventB = :eventId) " +
//            "ORDER BY es.similarityScore DESC")
//    List<EventSimilarity> findSimilarEvents(@Param("eventId") Long eventId,
//                                            org.springframework.data.domain.Pageable pageable);
//
//    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventA AND es.eventB = :eventB " +
//            "OR es.eventA = :eventB AND es.eventB = :eventA")
//    Optional<EventSimilarity> findSimilarityBetweenEvents(@Param("eventA") Long eventA,
//                                                          @Param("eventB") Long eventB);
//
//    @Query("SELECT es FROM EventSimilarity es WHERE (es.eventA = :eventId OR es.eventB = :eventId) " +
//            "AND (es.eventA NOT IN :excludedEvents AND es.eventB NOT IN :excludedEvents) " +
//            "ORDER BY es.similarityScore DESC")
//    List<EventSimilarity> findSimilarEventsExcluding(@Param("eventId") Long eventId,
//                                                     @Param("excludedEvents") List<Long> excludedEvents,
//                                                     org.springframework.data.domain.Pageable pageable);
//}

package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.EventSimilarityId;

import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarityId> {

    // Найти сходство по упорядоченной паре событий
    default Optional<EventSimilarity> findByOrderedEvents(Long event1, Long event2) {
        long first = Math.min(event1, event2);
        long second = Math.max(event1, event2);
        return findById(new EventSimilarityId(first, second));
    }

    // Найти похожие события для указанного события
    @Query("SELECT es FROM EventSimilarity es WHERE es.firstEvent = :eventId OR es.secondEvent = :eventId " +
            "ORDER BY es.score DESC")
    List<EventSimilarity> findSimilarEvents(@Param("eventId") Long eventId, Pageable pageable);

    // Найти похожие события, исключая определенные
    @Query("SELECT es FROM EventSimilarity es WHERE (es.firstEvent = :eventId OR es.secondEvent = :eventId) " +
            "AND (es.firstEvent NOT IN :excludedEvents AND es.secondEvent NOT IN :excludedEvents) " +
            "ORDER BY es.score DESC")
    List<EventSimilarity> findSimilarEventsExcluding(@Param("eventId") Long eventId,
                                                     @Param("excludedEvents") List<Long> excludedEvents,
                                                     Pageable pageable);
}