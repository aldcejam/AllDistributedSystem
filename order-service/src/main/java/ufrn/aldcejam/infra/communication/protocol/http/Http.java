package ufrn.aldcejam.infra.communication.protocol.http;

import br.ufrn.middleware.core.Broker;
import br.ufrn.middleware.remoting.HttpMethod;
import br.ufrn.middleware.remoting.MiddlewareRequest;
import br.ufrn.transport.core.Transport;
import br.ufrn.transport.core.TransportFactory;
import br.ufrn.transport.core.TransportListener;
import ufrn.aldcejam.infra.communication.protocol.Protocol;
import ufrn.aldcejam.infra.communication.protocol.http.dto.HttpRequest;
import ufrn.aldcejam.infra.communication.protocol.http.dto.HttpResponse;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Http implements TransportListener {
    private final Transport transport;
    private final String transportType;
    private final Broker broker;

    public Http(String transportType) {
        this.transportType = transportType.toUpperCase();
        this.transport = TransportFactory.create(this.transportType);
        this.broker = new Broker();
        this.broker.scan();
    }

    public void start() {
        transport.startServer(0, this);
    }

    @Override
    public void onServerCreated(int serverPort) {
        Protocol.notifyGateway(this.transportType, serverPort);
    }

    @Override
    public void onDataReceived(String rawData, OutputStream out) {
        HttpRequest request = HttpRequest.fromRawData(rawData);
        if (request == null) return;

        PrintWriter writer = new PrintWriter(out, true);
        System.out.println("[HTTP] Requisição recebida via Middleware: " + request.method() + " " + request.path());

        MiddlewareRequest mwRequest = new MiddlewareRequest(
                HttpMethod.fromString(request.method()),
                request.path(),
                new HashMap<>(),
                parseQueryParams(request.path()),
                request.body()
        );

        String jsonResult = broker.dispatch(mwRequest);
        new HttpResponse(writer).status(200).send(jsonResult);
    }

    private Map<String, String> parseQueryParams(String path) {
        Map<String, String> params = new HashMap<>();
        if (path.contains("?")) {
            String query = path.substring(path.indexOf("?") + 1);
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }
}