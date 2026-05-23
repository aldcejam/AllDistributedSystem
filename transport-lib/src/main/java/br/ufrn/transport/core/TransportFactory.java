package br.ufrn.transport.core;

public class TransportFactory {
    public static Transport create(String type) {
        return switch (type.toUpperCase()) {
            case "TCP" -> new TCP();
            case "UDP" -> new UDP();
            default -> throw new IllegalArgumentException("Tipo de transporte desconhecido: " + type);
        };
    }
}
