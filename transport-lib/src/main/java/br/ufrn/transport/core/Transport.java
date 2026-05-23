package br.ufrn.transport.core;

import java.io.OutputStream;

public abstract class Transport {
    public abstract void startServer(int port, TransportListener listener);
    public abstract void send(String rawData, String host, int port, OutputStream clientOut);
    public boolean checkHealth(String serviceName, String host, int port) {
        return false;
    }
}
