package ufrn.aldcejam.infra.communication.protocol;

import br.ufrn.middleware.util.JsonMarshaller;
import ufrn.aldcejam.infra.communication.protocol.http.dto.HttpRequest;
import ufrn.aldcejam.util.ConfigLoader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Protocol {

    public static void notifyGateway(String transport, int port) {
        String serviceName = ConfigLoader.getProperty("service.name", "Serviço-Desconhecido");
        int gatewayPort = Integer
                .parseInt(System.getProperty("gateway.port", ConfigLoader.getProperty("gateway.port", "9000")));
        String gatewayProtocol = System.getProperty("gateway.protocol",
                ConfigLoader.getProperty("gateway.protocol", "TCP"));
        String gatewayHost = System.getProperty("gateway.host", ConfigLoader.getProperty("gateway.host", "localhost"));
        String serviceHost = System.getProperty("service.host", ConfigLoader.getProperty("service.host", "localhost"));

        System.out.println("\n[NOTIFICAÇÃO GATEWAY]");
        System.out.println(
                "Enviando informações do serviço para o Gateway (" + gatewayHost + ") na porta " + gatewayPort + " via "
                        + gatewayProtocol
                        + "...");

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("name", serviceName);
        bodyMap.put("host", serviceHost);
        bodyMap.put("port", port);
        bodyMap.put("transport", transport);

        String jsonBody = JsonMarshaller.stringify(bodyMap);

        Map<String, String> headers = new HashMap<>();
        headers.put("Host", gatewayHost);
        headers.put("Content-Type", "application/json");
        headers.put("Content-Length", String.valueOf(jsonBody.length()));
        headers.put("Connection", "close");

        HttpRequest registerRequest = new HttpRequest("POST", "/gateway/register", headers, jsonBody);
        String httpRequest = registerRequest.toRawData();

        if ("TCP".equalsIgnoreCase(gatewayProtocol)) {
            sendTCP(gatewayHost, gatewayPort, httpRequest);
        } else if ("UDP".equalsIgnoreCase(gatewayProtocol)) {
            sendUDP(gatewayHost, gatewayPort, httpRequest);
        } else if ("GRPC".equalsIgnoreCase(gatewayProtocol)) {
            sendGRPC(gatewayHost, gatewayPort, serviceName, serviceHost, port, transport);
        }

        System.out.println(" - Nome do Serviço: " + serviceName);
        System.out.println(" - Transporte: " + transport);
        System.out.println(" - Porta do Serviço: " + port);
        System.out.println(" - Status: REGISTRADO");
        System.out.println("------------------------\n");
    }

    private static void sendTCP(String host, int port, String message) {
        try (Socket socket = new Socket(host, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Não foi possível conectar ao Gateway (TCP): " + e.getMessage());
        }
    }

    private static void sendUDP(String host, int port, String message) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Não foi possível conectar ao Gateway (UDP): " + e.getMessage());
        }
    }

    private static void sendGRPC(String host, int port, String serviceName, String serviceHost, int servicePort,
            String transport) {
        String target = host + ":" + port;
        io.grpc.ManagedChannel channel = io.grpc.ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();
        try {
            ufrn.aldcejam.grpc.GatewayServiceGrpc.GatewayServiceBlockingStub stub = ufrn.aldcejam.grpc.GatewayServiceGrpc
                    .newBlockingStub(channel);
            ufrn.aldcejam.grpc.GrpcRegisterRequest request = ufrn.aldcejam.grpc.GrpcRegisterRequest.newBuilder()
                    .setServiceName(serviceName)
                    .setHost(serviceHost)
                    .setPort(servicePort)
                    .setTransport(transport)
                    .build();
            stub.registerService(request);
        } catch (Exception e) {
            System.err.println("Não foi possível conectar ao Gateway (GRPC): " + e.getMessage());
        } finally {
            channel.shutdown();
        }
    }
}
