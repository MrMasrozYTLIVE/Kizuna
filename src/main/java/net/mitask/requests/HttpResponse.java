package net.mitask.requests;

import com.google.gson.Gson;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class is used to send response to the user.
 * @since 1.0.0
 * @author MiTask
 */
@SuppressWarnings("unused")
public class HttpResponse {
    private final BufferedWriter out;
    private final TemplateEngine templateEngine;
    private final List<String> headers = new LinkedList<>();
    private final Map<String, Object> cookies = new LinkedHashMap<>();
    private int statusCode = 200;

    public HttpResponse(BufferedWriter out, TemplateEngine templateEngine) {
        this.out = out;
        this.templateEngine = templateEngine;
    }

    /**
     * Sets integer status of the response
     * @param statusCode See {@link net.mitask.util.HttpStatusCode}
     */
    public void setStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Adds a header to the response
     * @param key Header key
     * @param value Header value
     */
    public void addHeader(String key, Object value) {
        headers.add(key + ": " + value);
    }

    /**
     * Adds a cookie to the response
     * @param key Cookie key
     * @param value Cookie value
     */
    public void addCookie(String key, Object value) {
        cookies.put(key, value);
    }

    /**
     * This is internal method to write all HTTP headers to the final response
     */
    private void writeHeaders() throws IOException {
        String statusMessage = HttpStatusCode.STATUS_CODES.getOrDefault(statusCode, "");
        out.write("HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n");
        for (String header : headers) {
            out.write(header + "\r\n");
        }

        for (Map.Entry<String, Object> cookie : cookies.entrySet()) {
            out.write("Set-Cookie: " + cookie.getKey() + "=" + cookie.getValue() + "\r\n");
        }
        out.write("\r\n");
    }

    public void sendText(String text) throws IOException {
        addHeader("Content-Type", "text/plain");
        addHeader("Content-Length", text.length());
        writeHeaders();
        out.write(text);
    }

    public void sendCustom(int httpCode, String contentType, String data) throws IOException {
        setStatus(httpCode);
        addHeader("Content-Type", contentType);
        addHeader("Content-Length", data.length());
        writeHeaders();
        out.write(data);
    }

    public void sendJson(Object data) throws IOException {
        String json = new Gson().toJson(data);
        setStatus(200);
        addHeader("Content-Type", "application/json");
        addHeader("Content-Length", json.length());
        writeHeaders();
        out.write(json);
    }

    public void sendFile(String filePath) throws IOException {
        Path file = Path.of(filePath);
        if (Files.exists(file)) {
            String contentType = Files.probeContentType(file);
            byte[] content = Files.readAllBytes(file);
            addHeader("Content-Type", contentType);
            addHeader("Content-Length", content.length);
            writeHeaders();
            out.write(new String(content, StandardCharsets.UTF_8));
        } else {
            setStatus(404);
            sendText("Not Found");
        }
    }

    public void sendTemplate(String templateName, Map<String, Object> parameters) throws IOException {
        if(templateEngine == null) {
            setStatus(500);
            sendText("Template could not be rendered because TemplateEngine is null! (TemplateDir not set?)");
            return;
        }

        if(!templateEngine.hasTemplate(templateName)) {
            setStatus(500);
            sendText("Template could not be rendered! (TemplateDir is set incorrectly or Template does not exist)");
            return;
        }

        Utf8ByteOutput output = new Utf8ByteOutput();
        templateEngine.render(templateName, parameters, output);
        addHeader("Content-Type", "text/html");
        addHeader("Content-Length", output.getContentLength());
        writeHeaders();
        out.write(new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    public void redirect(String url) throws IOException {
        addHeader("Location", url);
        sendCustom(302, "", "");
    }
}