package ru.practicum.statsclient.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.grpc.stats.recommendation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public Map<Long, Double> getRecommendationsForUser(Long userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerStub.getRecommendationsForUser(request);
            return streamFromIterator(iterator)
                    .collect(Collectors.toMap(
                            RecommendedEventProto::getEventId,
                            RecommendedEventProto::getScore
                    ));

        } catch (Exception e) {
            log.error("Failed to get recommendations for user {}", userId, e);
            return Collections.emptyMap();
        }
    }

    public List<RecommendedEvent> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerStub.getSimilarEvents(request);
            return streamFromIterator(iterator)
                    .map(proto -> new RecommendedEvent(proto.getEventId(), proto.getScore()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get similar events for event {}", eventId, e);
            return Collections.emptyList();
        }
    }

    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerStub.getInteractionsCount(request);
            return streamFromIterator(iterator)
                    .collect(Collectors.toMap(
                            RecommendedEventProto::getEventId,
                            RecommendedEventProto::getScore
                    ));

        } catch (Exception e) {
            log.error("Failed to get interactions count for events {}", eventIds, e);
            return Collections.emptyMap();
        }
    }

    private Stream<RecommendedEventProto> streamFromIterator(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }

    @RequiredArgsConstructor
    @Getter
    public static class RecommendedEvent {
        private final Long eventId;
        private final Double score;
    }
}
