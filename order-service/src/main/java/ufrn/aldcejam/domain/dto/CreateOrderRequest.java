package ufrn.aldcejam.domain.dto;

public record CreateOrderRequest(
        String productId,
        int quantity) {
}
