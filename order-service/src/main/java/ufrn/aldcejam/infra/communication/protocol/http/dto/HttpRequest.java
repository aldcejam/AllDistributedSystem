package ufrn.aldcejam.infra.communication.protocol.http.dto;

import java.util.HashMap;
import java.util.Map;

public record HttpRequest(
                String method,
                String path,
                Map<String, String> headers,
                String body) {

        public static HttpRequest fromRawData(String rawData) {
                String[] lines = rawData.split("\r?\n");
                if (lines.length == 0)
                        return null;

                String[] firstLineParts = lines[0].split(" ");
                if (firstLineParts.length < 2)
                        return null;

                String method = firstLineParts[0];
                String path = firstLineParts[1];
                Map<String, String> headers = new HashMap<>();

                int i = 1;
                while (i < lines.length && !lines[i].trim().isEmpty()) {
                        String[] headerParts = lines[i].split(":", 2);
                        if (headerParts.length == 2) {
                                headers.put(headerParts[0].trim(), headerParts[1].trim());
                        }
                        i++;
                }

                StringBuilder bodyBuilder = new StringBuilder();
                i++;
                while (i < lines.length) {
                        bodyBuilder.append(lines[i]).append("\n");
                        i++;
                }

                return new HttpRequest(method, path, headers, bodyBuilder.toString().trim());
        }
        public String toRawData() {
                StringBuilder raw = new StringBuilder();
                raw.append(method).append(" ").append(path).append(" HTTP/1.1\r\n");
                for (Map.Entry<String, String> header : headers.entrySet()) {
                        raw.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
                }
                raw.append("\r\n");
                if (body != null && !body.isEmpty()) {
                        raw.append(body);
                }
                return raw.toString();
        }
}
