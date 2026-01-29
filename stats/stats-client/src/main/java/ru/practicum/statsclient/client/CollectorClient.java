package ru.practicum.statsclient.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;
import ru.practicum.grpc.stats.collector.UserActionControllerGrpc;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public void sendViewAction(Long userId, Long eventId) {
        sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    public void sendRegisterAction(Long userId, Long eventId) {
        sendUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    public void sendLikeAction(Long userId, Long eventId) {
        sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    private void sendUserAction(Long userId, Long eventId, ActionTypeProto actionType) {
        try {
            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .build();

            collectorStub.collectUserAction(request);
            log.debug("Sent {} action for user {} event {}", actionType, userId, eventId);

        } catch (Exception e) {
            log.error("Failed to send user action to Collector: userId={}, eventId={}", userId, eventId, e);
        }
    }
}
