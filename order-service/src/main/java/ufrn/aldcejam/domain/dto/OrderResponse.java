package ufrn.aldcejam.domain.dto;

public record OrderResponse(
        int id,
        String productId,
        int quantity,
        String status) {
}
