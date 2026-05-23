package ufrn.aldcejam.infra.communication.protocol.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;
import ufrn.aldcejam.grpc.GatewayServiceGrpc;
import ufrn.aldcejam.grpc.GrpcHealthRequest;
import ufrn.aldcejam.grpc.GrpcHealthResponse;
import ufrn.aldcejam.infra.communication.protocol.Protocol;
import ufrn.aldcejam.infra.controllers.grpc.OrderController;

import java.io.IOException;

public class GrpcProtocol {
    private Server server;
    private int port;

    public void start() {
        try {
            this.server = ServerBuilder.forPort(0)
                    .addService(new OrderController())
                    .addService(new GatewayServiceGrpc.GatewayServiceImplBase() {
                        @Override
                        public void checkHealth(GrpcHealthRequest request,
                                StreamObserver<GrpcHealthResponse> responseObserver) {
                            responseObserver.onNext(GrpcHealthResponse.newBuilder().setHealthy(true).build());
                            responseObserver.onCompleted();
                        }
                    })
                    .addService(ProtoReflectionService.newInstance())
                    .build()
                    .start();

            this.port = this.server.getPort();
            System.out.println("[GRPC] Servidor gRPC nativo iniciado na porta " + this.port);
            Protocol.notifyGateway("GRPC", this.port);

            this.server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            System.err.println("[GRPC] Erro ao iniciar servidor: " + e.getMessage());
        }
    }
}
