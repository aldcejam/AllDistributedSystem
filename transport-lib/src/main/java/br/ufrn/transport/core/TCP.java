package br.ufrn.transport.core;

import java.io.*;
import java.net.*;

public class TCP extends Transport {
    private static final int TIMEOUT_MS = 5000;

    @Override
    public void startServer(int port, TransportListener listener) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            int actualPort = serverSocket.getLocalPort();
            System.out.println("[TRANSPORT-TCP] Servidor iniciado na porta: " + actualPort);
            
            if (listener != null) {
                listener.onServerCreated(actualPort);
            }

            while (true) {
                Socket socket = serverSocket.accept();
                Thread.startVirtualThread(() -> handleRequest(socket, listener));
            }
        } catch (IOException e) {
            System.err.println("[TRANSPORT-TCP] Erro fatal: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void handleRequest(Socket socket, TransportListener listener) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            StringBuilder rawRequest = new StringBuilder();
            String line;
            int contentLength = 0;

            while ((line = in.readLine()) != null && !line.isEmpty()) {
                rawRequest.append(line).append("\r\n");
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                }
            }
            rawRequest.append("\r\n");

            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                int totalRead = 0;
                while (totalRead < contentLength) {
                    int read = in.read(bodyChars, totalRead, contentLength - totalRead);
                    if (read == -1) break;
                    totalRead += read;
                }
                rawRequest.append(bodyChars, 0, totalRead);
            }

            if (!rawRequest.isEmpty() && listener != null) {
                listener.onDataReceived(rawRequest.toString(), out);
            }
        } catch (Exception e) {
            System.err.println("[TRANSPORT-TCP] Erro ao processar requisição: " + e.getMessage());
        }
    }

    @Override
    public void send(String rawData, String host, int port, OutputStream clientOut) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            out.write(rawData.getBytes());
            out.flush();

            if (clientOut != null) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    clientOut.write(buffer, 0, bytesRead);
                }
                clientOut.flush();
            }
        } catch (Exception e) {
            System.err.println("[TRANSPORT-TCP] Erro ao enviar: " + e.getMessage());
        }
    }

    @Override
    public boolean checkHealth(String serviceName, String host, int port) {
        String request = "GET /health HTTP/1.1\r\nHost: " + host + "\r\nConnection: close\r\n\r\n";
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.print(request);
            out.flush();
            String statusLine = in.readLine();
            return statusLine != null && statusLine.contains("200");
        } catch (Exception e) {
            return false;
        }
    }
}
