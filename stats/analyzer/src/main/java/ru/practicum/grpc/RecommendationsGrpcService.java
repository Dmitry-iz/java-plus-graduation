//package ru.practicum.grpc;
//
//import io.grpc.stub.StreamObserver;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.devh.boot.grpc.server.service.GrpcService;
//import ru.practicum.grpc.stats.recommendation.*;
//import ru.practicum.model.EventSimilarity;
//import ru.practicum.service.RecommendationService;
//
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//@GrpcService
//@RequiredArgsConstructor
//public class RecommendationsGrpcService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
//
//    private final RecommendationService recommendationService;
//
//    @Override
//    public void getRecommendationsForUser(UserPredictionsRequestProto request,
//                                          StreamObserver<RecommendedEventProto> responseObserver) {
//        log.info("Get recommendations for user: userId={}, maxResults={}",
//                request.getUserId(), request.getMaxResults());
//
//        try {
//            Map<Long, Double> recommendations = recommendationService
//                    .getRecommendationsForUser(request.getUserId(), request.getMaxResults());
//
//            for (Map.Entry<Long, Double> entry : recommendations.entrySet()) {
//                RecommendedEventProto response = RecommendedEventProto.newBuilder()
//                        .setEventId(entry.getKey())
//                        .setScore(entry.getValue())
//                        .build();
//                responseObserver.onNext(response);
//            }
//
//            responseObserver.onCompleted();
//            log.debug("Sent {} recommendations for user {}", recommendations.size(), request.getUserId());
//
//        } catch (Exception e) {
//            log.error("Error getting recommendations for user {}", request.getUserId(), e);
//            responseObserver.onError(e);
//        }
//    }
//
//    @Override
//    public void getSimilarEvents(SimilarEventsRequestProto request,
//                                 StreamObserver<RecommendedEventProto> responseObserver) {
//        log.info("Get similar events: eventId={}, userId={}, maxResults={}",
//                request.getEventId(), request.getUserId(), request.getMaxResults());
//
//        try {
//            List<EventSimilarity> similarEvents = recommendationService
//                    .getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults());
//
//            for (EventSimilarity similarity : similarEvents) {
//                Long similarEventId = similarity.getEventA().equals(request.getEventId())
//                        ? similarity.getEventB()
//                        : similarity.getEventA();
//
//                RecommendedEventProto response = RecommendedEventProto.newBuilder()
//                        .setEventId(similarEventId)
//                        .setScore(similarity.getSimilarityScore())
//                        .build();
//                responseObserver.onNext(response);
//            }
//
//            responseObserver.onCompleted();
//            log.debug("Sent {} similar events for event {}", similarEvents.size(), request.getEventId());
//
//        } catch (Exception e) {
//            log.error("Error getting similar events for event {}", request.getEventId(), e);
//            responseObserver.onError(e);
//        }
//    }
//
//    @Override
//    public void getInteractionsCount(InteractionsCountRequestProto request,
//                                     StreamObserver<RecommendedEventProto> responseObserver) {
//        log.info("Get interactions count for {} events", request.getEventIdCount());
//
//        try {
//            Map<Long, Double> interactions = recommendationService
//                    .getInteractionsCount(request.getEventIdList());
//
//            for (Map.Entry<Long, Double> entry : interactions.entrySet()) {
//                RecommendedEventProto response = RecommendedEventProto.newBuilder()
//                        .setEventId(entry.getKey())
//                        .setScore(entry.getValue())
//                        .build();
//                responseObserver.onNext(response);
//            }
//
//            responseObserver.onCompleted();
//            log.debug("Sent interactions count for {} events", interactions.size());
//
//        } catch (Exception e) {
//            log.error("Error getting interactions count", e);
//            responseObserver.onError(e);
//        }
//    }
//}

package ru.practicum.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.grpc.stats.recommendation.*;
import ru.practicum.model.EventSimilarity;
import ru.practicum.service.RecommendationService;

import java.util.List;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        Map<Long, Double> recommendations = recommendationService
                .getRecommendationsForUser(request.getUserId(), request.getMaxResults());

        recommendations.forEach((eventId, score) -> {
            responseObserver.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(eventId)
                    .setScore(score)
                    .build());
        });

        responseObserver.onCompleted();
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        List<EventSimilarity> similarEvents = recommendationService
                .getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults());

        for (EventSimilarity similarity : similarEvents) {
            Long similarEventId = similarity.getFirstEvent().equals(request.getEventId())
                    ? similarity.getSecondEvent()
                    : similarity.getFirstEvent();

            responseObserver.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(similarEventId)
                    .setScore(similarity.getScore())
                    .build());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        Map<Long, Double> interactions = recommendationService
                .getInteractionsCount(request.getEventIdList());

        interactions.forEach((eventId, score) -> {
            responseObserver.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(eventId)
                    .setScore(score)
                    .build());
        });

        responseObserver.onCompleted();
    }
}