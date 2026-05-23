package br.ufrn.transport.core;

import java.io.OutputStream;

public interface TransportListener {
    /**
     * Chamado quando dados brutos são recebidos pelo servidor de transporte.
     */
    void onDataReceived(String rawData, OutputStream out);

    /**
     * Chamado quando o servidor é criado com sucesso, informando a porta real.
     */
    void onServerCreated(int port);
}
