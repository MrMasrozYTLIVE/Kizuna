package net.mitask.requests;

import gg.jte.TemplateEngine;
import gg.jte.TemplateException;
import gg.jte.output.Utf8ByteOutput;
import net.mitask.util.HttpStatusCode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse {
    private final BufferedWriter out;
    private final TemplateEngine templateEngine;
    private final Map<String, Object> headers = new LinkedHashMap<>();
    private int statusCode = 200;

    public HttpResponse(BufferedWriter out, TemplateEngine templateEngine) {
        this.out = out;
        this.templateEngine = templateEngine;
    }

    public void setStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public void addHeader(String key, Object value) {
        headers.put(key, value);
    }

    private void writeHeaders() throws IOException {
        String statusMessage = HttpStatusCode.STATUS_CODES.getOrDefault(statusCode, "");
        out.write("HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n");
        for (Map.Entry<String, Object> header : headers.entrySet()) {
            out.write(header.getKey() + ": " + header.getValue() + "\r\n");
        }
        out.write("\r\n");
    }

    public void sendText(String text) throws IOException {
        headers.put("Content-Type", "text/plain");
        headers.put("Content-Length", text.length());
        writeHeaders();
        out.write(text);
    }

    public void sendCustom(int httpCode, String contentType, String data) throws IOException {
        setStatus(httpCode);
        headers.put("Content-Type", contentType);
        headers.put("Content-Length", data.length());
        writeHeaders();
        out.write(data);
    }

    public void sendFile(String filePath) throws IOException {
        Path file = Path.of(filePath);
        if (Files.exists(file)) {
            String contentType = Files.probeContentType(file);
            byte[] content = Files.readAllBytes(file);
            headers.put("Content-Type", contentType);
            headers.put("Content-Length", content.length);
            writeHeaders();
            out.write(new String(content, StandardCharsets.UTF_8));
        } else {
            setStatus(404);
            sendText("Not Found");
        }
    }

    public void sendTemplate(String templateName, Map<String, Object> parameters) throws IOException {
        Utf8ByteOutput output = new Utf8ByteOutput();

        try {
            templateEngine.render(templateName, parameters, output);
        } catch (TemplateException e) {
            setStatus(500);
            sendText(e.getMessage());
            return;
        }

        headers.put("Content-Type", "text/html");
        headers.put("Content-Length", output.getContentLength());
        writeHeaders();
        out.write(new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    public void redirect(String url) throws IOException {
        sendCustom(302, "text/html", """
                <html>
                    <head>
                        <meta http-equiv="Refresh" content="0; URL=%s" />
                    </head>
                </html>
                """.formatted(url));
    }
}