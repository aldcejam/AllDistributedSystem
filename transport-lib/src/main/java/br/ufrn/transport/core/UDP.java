package br.ufrn.transport.core;

import java.io.*;
import java.net.*;

public class UDP extends Transport {

    @Override
    public void startServer(int port, TransportListener listener) {
        try (DatagramSocket datagramSocket = new DatagramSocket(port)) {
            int actualPort = datagramSocket.getLocalPort();
            System.out.println("[TRANSPORT-UDP] Servidor iniciado na porta: " + actualPort);

            if (listener != null) {
                listener.onServerCreated(actualPort);
            }

            while (true) {
                byte[] buffer = new byte[65535];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet);
                Thread.startVirtualThread(() -> {
                    try {
                        String rawData = new String(packet.getData(), 0, packet.getLength());

                        ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
                        if (listener != null && !rawData.isEmpty()) {
                            listener.onDataReceived(rawData, responseBuffer);
                        }

                        byte[] responseBytes = responseBuffer.toByteArray();
                        if (responseBytes.length > 0) {
                            DatagramPacket responsePacket = new DatagramPacket(
                                    responseBytes,
                                    responseBytes.length,
                                    packet.getAddress(),
                                    packet.getPort());
                            datagramSocket.send(responsePacket);
                        }
                    } catch (Exception e) {
                        System.err.println("[TRANSPORT-UDP] Erro processando pacote: " + e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("[TRANSPORT-UDP] Erro fatal: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(String rawData, String host, int port, OutputStream clientOut) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] data = rawData.getBytes();
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);

            if (clientOut != null) {
                byte[] buffer = new byte[65535];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                socket.setSoTimeout(5000);
                try {
                    socket.receive(responsePacket);
                    clientOut.write(responsePacket.getData(), 0, responsePacket.getLength());
                    clientOut.flush();
                } catch (SocketTimeoutException e) {
                }
            }
        } catch (Exception e) {
            System.err.println("[TRANSPORT-UDP] Erro ao enviar: " + e.getMessage());
        }
    }

    @Override
    public boolean checkHealth(String serviceName, String host, int port) {
        String request = "GET /health HTTP/1.1\r\nHost: " + host + "\r\nConnection: close\r\n\r\n";
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(3000);
            byte[] data = request.getBytes();
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);

            byte[] buffer = new byte[65535];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(responsePacket);

            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            return response.contains("200");
        } catch (Exception e) {
            return false;
        }
    }
}
