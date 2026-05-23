package br.ufrn.middleware.remoting;

import java.util.Map;

public record MiddlewareRequest(
        HttpMethod method,
        String path,
        Map<String, String> headers,
        Map<String, String> queryParams,
        String body
) {
}
