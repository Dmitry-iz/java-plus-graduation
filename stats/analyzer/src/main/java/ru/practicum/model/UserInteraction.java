//package ru.practicum.model;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//
//@Data
//@Entity
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//@Table(name = "user_interactions",
//        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}))
//public class UserInteraction {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "user_id", nullable = false)
//    private Long userId;
//
//    @Column(name = "event_id", nullable = false)
//    private Long eventId;
//
//    @Column(name = "action_weight", nullable = false)
//    private Double actionWeight;
//
//    @Column(name = "last_action_timestamp", nullable = false)
//    private LocalDateTime lastActionTimestamp;
//
//    @Column(name = "created_at", nullable = false)
//    private LocalDateTime createdAt;
//}

package ru.practicum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_actions")
@IdClass(UserInteractionId.class)
public class UserInteraction {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_score", nullable = false)
    private Double userScore;

    @Column(name = "timestamp_action", nullable = false)
    private LocalDateTime timestampAction;
}