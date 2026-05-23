package ufrn.aldcejam.communication.transport;

import br.ufrn.transport.core.TCP;
import br.ufrn.transport.core.UDP;
import br.ufrn.transport.core.Transport;

public class TransportFactory {
    public static Transport create(String type) {
        return switch (type.toUpperCase()) {
            case "TCP" -> new TCP();
            case "UDP" -> new UDP();
            case "GRPC" -> new GRPC();
            default -> throw new IllegalArgumentException("Tipo de transporte desconhecido: " + type);
        };
    }
}
