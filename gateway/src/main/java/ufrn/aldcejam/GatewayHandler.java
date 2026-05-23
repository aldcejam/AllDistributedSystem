package ufrn.aldcejam;

import br.ufrn.middleware.core.Broker;
import br.ufrn.middleware.remoting.HttpMethod;
import br.ufrn.middleware.remoting.MiddlewareRequest;
import lombok.Getter;
import br.ufrn.transport.core.Transport;
import br.ufrn.transport.core.TransportFactory;
import br.ufrn.transport.core.TransportListener;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GatewayHandler implements TransportListener {
    @Getter
    private final ServiceRegistry registry;
    private final Map<String, Transport> transports = new ConcurrentHashMap<>();
    private final Broker broker = new Broker();

    public GatewayHandler(ServiceRegistry registry) {
        this.registry = registry;
        GatewayController.setRegistry(registry);
        broker.scan("ufrn.aldcejam");
    }

    private record HttpRequestContext(
            String method,
            String originalPath,
            String serviceName,
            String remainingPath,
            String body,
            String rawData,
            String firstLine
    ) {}

    @Override
    public void onDataReceived(String rawData, OutputStream out) {
        HttpRequestContext ctx = parseRequest(rawData);
        if (ctx == null) return;

        if (ctx.originalPath.startsWith("/gateway")) {
            handleMiddlewareRequest(ctx, out);
        } else if (rawData.startsWith("REGISTER")) {
            handleTextRegistration(rawData, out);
        } else {
            handleForwarding(ctx, out);
        }
    }

    private HttpRequestContext parseRequest(String rawData) {
        String[] lines = rawData.split("\r\n");
        if (lines.length == 0) return null;

        String firstLine = lines[0];
        String[] parts = firstLine.split(" ");
        if (parts.length < 2) return null;

        String method = parts[0];
        String originalPath = parts[1];
        
        String pathForExtraction = originalPath.startsWith("/") ? originalPath.substring(1) : originalPath;
        String[] pathParts = pathForExtraction.split("/", 2);
        String serviceName = pathParts[0];
        String remainingPath = pathParts.length > 1 ? "/" + pathParts[1] : "/";

        String body = "";
        if (rawData.contains("\r\n\r\n")) {
            body = rawData.substring(rawData.indexOf("\r\n\r\n") + 4);
        }

        return new HttpRequestContext(method, originalPath, serviceName, remainingPath, body, rawData, firstLine);
    }

    private void handleMiddlewareRequest(HttpRequestContext ctx, OutputStream out) {
        System.out.println("[GATEWAY] Middleware: " + ctx.method + " " + ctx.originalPath);

        MiddlewareRequest mwRequest = new MiddlewareRequest(
                HttpMethod.fromString(ctx.method),
                ctx.originalPath,
                new HashMap<>(),
                parseQueryParams(ctx.originalPath),
                ctx.body
        );

        String responseJson = broker.dispatch(mwRequest);
        sendHttpResponse(out, 200, "OK", responseJson);
    }

    private void handleForwarding(HttpRequestContext ctx, OutputStream out) {
        ServiceRegistry.InstanceInfo instance = registry.getNextInstance(ctx.serviceName);

        if (instance == null) {
            System.err.println("[GATEWAY] Serviço não encontrado: " + ctx.serviceName);
            sendHttpResponse(out, 404, "Not Found", "{\"error\": \"Serviço Não Encontrado\"}");
            return;
        }

        System.out.println("[GATEWAY] Forward -> " + ctx.serviceName + " (" + instance.host() + ":" + instance.port() + ")");

        String[] firstLineParts = ctx.firstLine.split(" ");
        String protocol = firstLineParts.length > 2 ? firstLineParts[2] : "HTTP/1.1";
        String modifiedFirstLine = ctx.method + " " + ctx.remainingPath + " " + protocol;
        
        String modifiedRawData = ctx.rawData.replaceFirst(Pattern.quote(ctx.firstLine), Matcher.quoteReplacement(modifiedFirstLine));

        Transport transport = transports.computeIfAbsent(instance.transport(), TransportFactory::create);
        transport.send(modifiedRawData, instance.host(), instance.port(), out);
    }

    private void sendHttpResponse(OutputStream out, int status, String statusText, String body) {
        if (out == null) return;
        try {
            PrintWriter writer = new PrintWriter(out, true);
            writer.print("HTTP/1.1 " + status + " " + statusText + "\r\n");
            writer.print("Content-Type: application/json\r\n");
            writer.print("Content-Length: " + body.length() + "\r\n");
            writer.print("Connection: close\r\n\r\n");
            writer.print(body);
            writer.flush();
        } catch (Exception e) {
            System.err.println("[GATEWAY] Erro ao enviar resposta: " + e.getMessage());
        }
    }

    private void handleTextRegistration(String rawData, OutputStream out) {
        try {
            String[] parts = rawData.split(" ");
            if (parts.length >= 5) {
                registry.register(parts[1], parts[2], Integer.parseInt(parts[3]), parts[4].trim());
                sendHttpResponse(out, 200, "OK", "");
            }
        } catch (Exception e) {
            System.err.println("[GATEWAY] Erro no registro: " + e.getMessage());
            sendHttpResponse(out, 400, "Bad Request", "{\"error\": \"Falha no registro\"}");
        }
    }

    private Map<String, String> parseQueryParams(String path) {
        Map<String, String> params = new HashMap<>();
        if (path.contains("?")) {
            String query = path.substring(path.indexOf("?") + 1);
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }

    @Override
    public void onServerCreated(int serverPort) {
        System.out.println("[GATEWAY] Servidor ativo na porta: " + serverPort);
    }
}
