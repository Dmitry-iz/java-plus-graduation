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
//@Table(name = "event_similarities",
//        uniqueConstraints = @UniqueConstraint(columnNames = {"event_a", "event_b"}))
//public class EventSimilarity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "event_a", nullable = false)
//    private Long eventA;
//
//    @Column(name = "event_b", nullable = false)
//    private Long eventB;
//
//    @Column(name = "similarity_score", nullable = false)
//    private Double similarityScore;
//
//    @Column(name = "calculated_at", nullable = false)
//    private LocalDateTime calculatedAt;
//}

package ru.practicum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "event_similarities")
@IdClass(EventSimilarityId.class)
public class EventSimilarity {

    @Id
    @Column(name = "first_event", nullable = false)
    private Long firstEvent;

    @Id
    @Column(name = "second_event", nullable = false)
    private Long secondEvent;

    @Column(name = "score", nullable = false)
    private Double score;
}