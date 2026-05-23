package ufrn.aldcejam;

import br.ufrn.transport.core.Transport;
import ufrn.aldcejam.communication.transport.TransportFactory;
import ufrn.aldcejam.util.ConfigLoader;

public class GatewayManagement {
    private final ServiceRegistry registry;

    public GatewayManagement() {
        this.registry = new ServiceRegistry();
        new HealthChecker(registry).start();
    }

    public void start() {
        String protocol = System.getProperty("gateway.protocol", ConfigLoader.getProperty("gateway.protocol", "TCP"))
                .toUpperCase();
        int port = Integer.parseInt(System.getProperty("gateway.port", ConfigLoader.getProperty("gateway.port", "9000")));

        System.out.println("[GATEWAY] Iniciando com o protocolo: " + protocol + " na porta: " + port);

        Transport server = TransportFactory.create(protocol);
        GatewayHandler handler = new GatewayHandler(registry);
        server.startServer(port, handler);
    }
}
