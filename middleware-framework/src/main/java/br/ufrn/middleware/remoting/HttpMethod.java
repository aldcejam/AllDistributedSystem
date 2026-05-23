package br.ufrn.middleware.remoting;

public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH;

    public static HttpMethod fromString(String method) {
        return valueOf(method.toUpperCase());
    }
}
