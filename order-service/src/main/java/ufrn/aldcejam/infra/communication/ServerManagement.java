package ufrn.aldcejam.infra.communication;

import ufrn.aldcejam.infra.communication.protocol.http.Http;
import ufrn.aldcejam.infra.communication.protocol.grpc.GrpcProtocol;
import ufrn.aldcejam.util.ConfigLoader;

public class ServerManagement {

    public void start() {
        selectServer();
    }

    private void selectServer() {
        String protocol = System.getProperty("gateway.protocol", ConfigLoader.getProperty("gateway.protocol", "TCP"))
                .toUpperCase();

        switch (protocol) {
            case "TCP", "UDP" -> new Http(protocol).start();
            case "GRPC" -> new GrpcProtocol().start();
            default -> System.out.println("Protocolo não identificado");
        }
    }
}
