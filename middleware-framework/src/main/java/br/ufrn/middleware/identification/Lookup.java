package br.ufrn.middleware.identification;

import br.ufrn.middleware.remoting.HttpMethod;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Lookup {
    private final Map<String, RouteDefinition> registry = new HashMap<>();

    public void register(HttpMethod httpMethod, String path, RouteDefinition definition) {
        String key = formatKey(httpMethod, path);
        registry.put(key, definition);
        System.out.println("[LOOKUP] Rota registrada: " + key);
    }

    public Optional<RouteDefinition> find(HttpMethod httpMethod, String path) {
        return Optional.ofNullable(registry.get(formatKey(httpMethod, path)));
    }

    private String formatKey(HttpMethod httpMethod, String path) {
        return httpMethod.name() + ":" + normalizePath(path);
    }

    private String normalizePath(String path) {
        if (path == null) return "/";
        if (!path.startsWith("/")) path = "/" + path;
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path;
    }
}
