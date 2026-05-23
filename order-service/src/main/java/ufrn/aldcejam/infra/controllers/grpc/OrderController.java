package ufrn.aldcejam.infra.controllers.grpc;

import io.grpc.stub.StreamObserver;
import ufrn.aldcejam.domain.dto.OrderResponse;
import ufrn.aldcejam.domain.dto.CreateOrderRequest;
import ufrn.aldcejam.domain.services.OrderService;
import ufrn.aldcejam.grpc.*;

public class OrderController extends order_serviceGrpc.order_serviceImplBase {
    private final OrderService orderService;

    public OrderController() {
        this.orderService = new OrderService();
    }

    @Override
    public void createOrder(GrpcCreateOrderRequest request, StreamObserver<GrpcCreateOrderResponse> responseObserver) {
        System.out.println("[GRPC] Recebido CreateOrder para: " + request.getProductId());

        CreateOrderRequest domainReq = new CreateOrderRequest(
                request.getProductId(),
                request.getQuantity());

        OrderResponse domainRes = orderService.createOrder(domainReq);

        GrpcCreateOrderResponse response = GrpcCreateOrderResponse.newBuilder()
                .setId(domainRes.id())
                .setStatus(domainRes.status())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getOrder(GrpcGetOrderRequest request, StreamObserver<GrpcOrderResponse> responseObserver) {
        OrderResponse domainRes = orderService.getOrder(request.getId());

        if (domainRes != null) {
            GrpcOrderResponse response = GrpcOrderResponse.newBuilder()
                    .setId(domainRes.id())
                    .setProductId(domainRes.productId())
                    .setQuantity(domainRes.quantity())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver
                    .onError(io.grpc.Status.NOT_FOUND.withDescription("Pedido não encontrado").asRuntimeException());
        }
    }

    @Override
    public void listOrders(com.google.protobuf.Empty request, StreamObserver<GrpcListOrdersResponse> responseObserver) {
        var orders = orderService.listOrders();
        GrpcListOrdersResponse.Builder responseBuilder = GrpcListOrdersResponse.newBuilder();

        for (OrderResponse order : orders) {
            GrpcOrderResponse grpcOrder = GrpcOrderResponse.newBuilder()
                    .setId(order.id())
                    .setProductId(order.productId())
                    .setQuantity(order.quantity())
                    .build();
            responseBuilder.addOrders(grpcOrder);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
