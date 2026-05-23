package ufrn.aldcejam.infra.persistence;

import ufrn.aldcejam.domain.dto.CreateOrderRequest;
import ufrn.aldcejam.domain.dto.OrderResponse;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

public class OrderRepository {
    private final Map<Integer, OrderResponse> orders = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1000);

    public OrderRepository() {
        for (int i = 1; i <= 10; i++) {
            int id = idCounter.incrementAndGet();
            orders.put(id, new OrderResponse(id, "produto-" + i, i * 10, "PRE-POPULATED"));
        }
    }

    private void simulateLatency() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public OrderResponse save(CreateOrderRequest request) {
        simulateLatency();
        int id = idCounter.incrementAndGet();
        OrderResponse response = new OrderResponse(id, request.productId(), request.quantity(), "CREATED");
        orders.put(id, response);
        System.out.println("[REPO] Pedido salvo no mock: " + id);
        return response;
    }

    public OrderResponse findById(int id) {
        simulateLatency();
        return orders.get(id);
    }

    public List<OrderResponse> listAll() {
        simulateLatency();
        return orders.values().stream().toList();
    }
}
