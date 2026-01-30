package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.UserInteraction;

import java.util.List;
import java.util.Optional;

public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    Optional<UserInteraction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserInteraction> findByUserId(Long userId);

    List<UserInteraction> findByEventId(Long eventId);

    @Query("SELECT ui FROM UserInteraction ui WHERE ui.userId = :userId AND ui.eventId IN :eventIds")
    List<UserInteraction> findByUserIdAndEventIds(@Param("userId") Long userId,
                                                  @Param("eventIds") List<Long> eventIds);

    @Query("SELECT ui FROM UserInteraction ui WHERE ui.userId = :userId " +
            "ORDER BY ui.lastActionTimestamp DESC")
    List<UserInteraction> findRecentByUserId(@Param("userId") Long userId,
                                             org.springframework.data.domain.Pageable pageable);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT SUM(ui.actionWeight) FROM UserInteraction ui WHERE ui.eventId = :eventId")
    Double getTotalWeightByEventId(@Param("eventId") Long eventId);

    @Query("SELECT SUM(ui.actionWeight) FROM UserInteraction ui WHERE ui.eventId IN :eventIds")
    List<Object[]> getTotalWeightsByEventIds(@Param("eventIds") List<Long> eventIds);
}