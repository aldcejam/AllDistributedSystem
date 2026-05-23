package ufrn.aldcejam.domain.dto;

import java.util.Map;

public record ServiceResponse(
        int statusCode,
        Map<String, String> headers,
        String body) {
}
