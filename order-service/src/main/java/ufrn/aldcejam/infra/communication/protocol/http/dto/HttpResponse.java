package ufrn.aldcejam.infra.communication.protocol.http.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private final PrintWriter out;
    private int statusCode = 200;
    private String statusMessage = "OK";
    private final Map<String, String> headers = new HashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    public HttpResponse(PrintWriter out) {
        this.out = out;
        headers.put("Content-Type", "application/json; charset=utf-8");
    }

    public HttpResponse status(int code) {
        this.statusCode = code;
        this.statusMessage = switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "Unknown";
        };
        return this;
    }

    public HttpResponse header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public void send(Object body) {
        if (out == null)
            return;

        try {
            String content;
            if (body == null) {
                headers.put("Content-Type", "text/plain; charset=utf-8");
                content = "";
            } else if (body instanceof java.util.Map || body instanceof java.lang.Iterable
                    || body.getClass().isRecord()) {
                headers.put("Content-Type", "application/json; charset=utf-8");
                content = mapper.writeValueAsString(body);
            } else if (body instanceof String s && (s.trim().startsWith("{") || s.trim().startsWith("["))) {
                headers.put("Content-Type", "application/json; charset=utf-8");
                content = s;
            } else {
                headers.put("Content-Type", "text/plain; charset=utf-8");
                content = String.valueOf(body);
            }

            headers.put("Content-Length", String.valueOf(content.getBytes().length));
            headers.put("Connection", "close");

            out.print("HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n");
            for (Map.Entry<String, String> header : headers.entrySet()) {
                out.print(header.getKey() + ": " + header.getValue() + "\r\n");
            }
            out.print("\r\n");
            out.print(content);
            out.flush();
        } catch (Exception e) {
            out.print("HTTP/1.1 500 Internal Server Error\r\n");
            out.print("Content-Type: text/plain\r\n\r\n");
            out.print("Erro ao processar resposta: " + e.getMessage());
            out.flush();
        }
    }
}
