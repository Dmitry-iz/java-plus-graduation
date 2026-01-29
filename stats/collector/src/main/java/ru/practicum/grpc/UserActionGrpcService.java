package ru.practicum.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;
import ru.practicum.grpc.stats.collector.UserActionControllerGrpc;
import ru.practicum.service.CollectorService;

import java.time.Instant;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionGrpcService extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final CollectorService collectorService;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.info("Received user action via gRPC: userId={}, eventId={}, actionType={}",
                request.getUserId(), request.getEventId(), request.getActionType());

        try {
            Instant timestamp;
            if (request.hasTimestamp()) {
                com.google.protobuf.Timestamp protoTimestamp = request.getTimestamp();
                timestamp = Instant.ofEpochSecond(
                        protoTimestamp.getSeconds(),
                        protoTimestamp.getNanos()
                );
            } else {
                timestamp = Instant.now();
            }

            ActionTypeAvro actionType = mapToAvroActionType(request.getActionType());

            collectorService.sendUserAction(
                    request.getUserId(),
                    request.getEventId(),
                    actionType,
                    timestamp
            );

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

            log.debug("Successfully processed gRPC user action for user {}, event {}",
                    request.getUserId(), request.getEventId());

        } catch (Exception e) {
            log.error("Error processing gRPC user action: userId={}, eventId={}",
                    request.getUserId(), request.getEventId(), e);
            responseObserver.onError(e);
        }
    }

    private ActionTypeAvro mapToAvroActionType(ActionTypeProto protoType) {
        return switch (protoType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> {
                log.warn("Unknown action type received: {}", protoType);
                throw new IllegalArgumentException("Unknown action type: " + protoType);
            }
        };
    }
}