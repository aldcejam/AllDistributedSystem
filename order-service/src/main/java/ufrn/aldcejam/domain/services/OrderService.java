package ufrn.aldcejam.domain.services;

import ufrn.aldcejam.domain.dto.CreateOrderRequest;
import ufrn.aldcejam.domain.dto.OrderResponse;
import ufrn.aldcejam.infra.persistence.OrderRepository;

import java.util.List;

public class OrderService {
    private final OrderRepository repository;

    public OrderService() {
        this.repository = new OrderRepository();
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        System.out.println("[SERVICE] Criando pedido para o produto: " + request.productId());
        return repository.save(request);
    }

    public OrderResponse getOrder(int id) {
        return repository.findById(id);
    }

    public List<OrderResponse> listOrders() {
        return repository.listAll();
    }
}
