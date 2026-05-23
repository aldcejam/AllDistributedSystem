package ufrn.aldcejam.domain.dto;

import java.util.Map;

public record ServiceRequest(
        String method,
        String path,
        Map<String, String> headers,
        String body) {
}
