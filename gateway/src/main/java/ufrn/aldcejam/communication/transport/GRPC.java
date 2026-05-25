package ufrn.aldcejam.communication.transport;

import br.ufrn.transport.core.Transport;
import br.ufrn.transport.core.TransportListener;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import ufrn.aldcejam.GatewayHandler;
import ufrn.aldcejam.ServiceRegistry;
import ufrn.aldcejam.grpc.*;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class GRPC extends Transport {
    private static final int TIMEOUT_MS = 500;
    private Server server;

    private static final MethodDescriptor.Marshaller<byte[]> BYTE_MARSHALLER = new MethodDescriptor.Marshaller<>() {
        @Override
        public InputStream stream(byte[] value) {
            return new ByteArrayInputStream(value);
        }

        @Override
        public byte[] parse(InputStream stream) {
            try {
                return stream.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };

    @Override
    public void startServer(int port, TransportListener listener) {
        if (!(listener instanceof GatewayHandler handler)) {
            throw new IllegalArgumentException("Listener do gRPC no Gateway deve ser GatewayHandler");
        }

        HandlerRegistry fallbackRegistry = new HandlerRegistry() {
            @Override
            public ServerMethodDefinition<?, ?> lookupMethod(String methodName, String authority) {
                MethodDescriptor<byte[], byte[]> method = MethodDescriptor.<byte[], byte[]>newBuilder()
                        .setType(MethodDescriptor.MethodType.UNARY)
                        .setFullMethodName(methodName)
                        .setRequestMarshaller(BYTE_MARSHALLER)
                        .setResponseMarshaller(BYTE_MARSHALLER)
                        .build();

                return ServerMethodDefinition.create(method, new ProxyCallHandler(methodName, handler));
            }
        };

        try {
            this.server = ServerBuilder.forPort(port)
                    .addService(new GatewayServiceImpl(handler))
                    .fallbackHandlerRegistry(fallbackRegistry)
                    .build();
            this.server.start();
            int actualPort = this.server.getPort();
            System.out.println("[GATEWAY-GRPC] Servidor iniciado na porta: " + actualPort);
            
            listener.onServerCreated(actualPort);
            this.server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            System.err.println("[GATEWAY-GRPC] Erro fatal: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(String rawData, String host, int port, OutputStream clientOut) {
        String target = host + ":" + port;
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();

        try {
            GatewayServiceGrpc.GatewayServiceBlockingStub stub = GatewayServiceGrpc.newBlockingStub(channel);
            // Chamada genérica gRPC via gateway
            stub.proxyCall(GrpcRegisterRequest.newBuilder().build());

            if (clientOut != null) {
                PrintWriter writer = new PrintWriter(clientOut, true);
                writer.println("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nProxy call completed (gRPC)");
            }
        } catch (Exception e) {
            System.err.println("[GRPC] Erro ao enviar: " + e.getMessage());
        } finally {
            channel.shutdown();
        }
    }

    @Override
    public boolean checkHealth(String serviceName, String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        try {
            GatewayServiceGrpc.GatewayServiceBlockingStub stub = GatewayServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(TIMEOUT_MS, TimeUnit.MILLISECONDS);

            GrpcHealthRequest request = GrpcHealthRequest.newBuilder().setServiceName(serviceName).build();
            GrpcHealthResponse response = stub.checkHealth(request);
            return response.getHealthy();
        } catch (Exception e) {
            return false;
        } finally {
            channel.shutdown();
        }
    }

    private static class ProxyCallHandler implements ServerCallHandler<byte[], byte[]> {
        private final String fullMethodName;
        private final GatewayHandler handler;

        public ProxyCallHandler(String fullMethodName, GatewayHandler handler) {
            this.fullMethodName = fullMethodName;
            this.handler = handler;
        }

        @Override
        public ServerCall.Listener<byte[]> startCall(ServerCall<byte[], byte[]> call, Metadata headers) {
            String fullServiceName = MethodDescriptor.extractFullServiceName(fullMethodName);
            String simpleServiceName = (fullServiceName != null && fullServiceName.contains("."))
                    ? fullServiceName.substring(fullServiceName.lastIndexOf('.') + 1)
                    : fullServiceName;

            ServiceRegistry.InstanceInfo instance = handler.getRegistry().getNextInstance(simpleServiceName);
            if (instance == null) {
                call.close(Status.NOT_FOUND.withDescription("Servico destino nao encontrado: " + simpleServiceName), new Metadata());
                return new ServerCall.Listener<>() {};
            }

            System.out.println("[GATEWAY-PROXY-GRPC] Roteando " + fullMethodName + " -> " + instance.host() + ":" + instance.port());

            ManagedChannel channel = ManagedChannelBuilder.forAddress(instance.host(), instance.port())
                    .usePlaintext()
                    .build();

            MethodDescriptor<byte[], byte[]> method = MethodDescriptor.<byte[], byte[]>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(fullMethodName)
                    .setRequestMarshaller(BYTE_MARSHALLER)
                    .setResponseMarshaller(BYTE_MARSHALLER)
                    .build();

            ClientCall<byte[], byte[]> clientCall = channel.newCall(method, CallOptions.DEFAULT);
            clientCall.start(new ClientCall.Listener<>() {
                @Override public void onHeaders(Metadata clientHeaders) { call.sendHeaders(clientHeaders); }
                @Override public void onMessage(byte[] message) { call.sendMessage(message); }
                @Override public void onClose(Status status, Metadata trailers) {
                    call.close(status, trailers);
                    channel.shutdown();
                }
            }, headers);

            clientCall.request(1);
            call.request(1);

            return new ServerCall.Listener<>() {
                @Override public void onMessage(byte[] messageBytes) { clientCall.sendMessage(messageBytes); }
                @Override public void onHalfClose() { clientCall.halfClose(); }
                @Override public void onCancel() { clientCall.cancel("Gateway client cancelled", null); }
            };
        }
    }

    private static class GatewayServiceImpl extends GatewayServiceGrpc.GatewayServiceImplBase {
        private final GatewayHandler handler;

        public GatewayServiceImpl(GatewayHandler handler) {
            this.handler = handler;
        }

        @Override
        public void registerService(GrpcRegisterRequest request, StreamObserver<GrpcRegisterResponse> responseObserver) {
            handler.getRegistry().register(request.getServiceName(), request.getHost(), request.getPort(), request.getTransport());
            responseObserver.onNext(GrpcRegisterResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        }

        @Override
        public void checkHealth(GrpcHealthRequest request, StreamObserver<GrpcHealthResponse> responseObserver) {
            responseObserver.onNext(GrpcHealthResponse.newBuilder().setHealthy(true).build());
            responseObserver.onCompleted();
        }
    }
}
