package ru.practicum.grpc;

import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @GrpcGlobalServerInterceptor
    public ServerInterceptor loggingInterceptor() {
        return new io.grpc.ServerInterceptor() {
            @Override
            public <ReqT, RespT> io.grpc.ServerCall.Listener<ReqT> interceptCall(
                    io.grpc.ServerCall<ReqT, RespT> call,
                    io.grpc.Metadata headers,
                    io.grpc.ServerCallHandler<ReqT, RespT> next) {

                System.out.println("gRPC call received: " + call.getMethodDescriptor().getFullMethodName());
                return next.startCall(call, headers);
            }
        };
    }
}