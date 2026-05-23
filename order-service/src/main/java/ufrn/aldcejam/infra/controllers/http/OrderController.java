package ufrn.aldcejam.infra.controllers.http;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.Controller;
import br.ufrn.middleware.annotations.Get;
import br.ufrn.middleware.annotations.Post;
import br.ufrn.middleware.lifecycle.Lifecycle;
import com.fasterxml.jackson.databind.ObjectMapper;
import ufrn.aldcejam.domain.dto.CreateOrderRequest;
import ufrn.aldcejam.domain.dto.OrderResponse;
import ufrn.aldcejam.domain.services.OrderService;

import java.util.List;

@Controller(path = "/orders", lifecycle = Lifecycle.STATIC)
public class OrderController {
    private final OrderService orderService;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderController() {
        this.orderService = new OrderService();
    }

    @Post(path = "/")
    public String create(@Body String body) {
        try {
            CreateOrderRequest request = mapper.readValue(body, CreateOrderRequest.class);
            OrderResponse response = orderService.createOrder(request);

            if (response != null) {
                return mapper.writeValueAsString(response);
            } else {
                return "{\"error\": \"Erro ao criar pedido.\"}";
            }
        } catch (Exception e) {
            return "{\"error\": \"Bad Request: " + e.getMessage() + "\"}";
        }
    }

    @Get(path = "/")
    public String list() {
        try {
            List<OrderResponse> orders = orderService.listOrders();
            return mapper.writeValueAsString(orders);
        } catch (Exception e) {
            return "{\"error\": \"Erro ao listar pedidos: " + e.getMessage() + "\"}";
        }
    }
}
