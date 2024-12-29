package net.mitask.requests;

import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * This class contains all info from the received request
 * @since 1.0.0
 * @author MiTask
 */
@AllArgsConstructor
public class HttpRequest {
    public final String method;
    public final String path;
    public final Map<String, String> queryParams;
    public final Map<String, String> urlParams;
    public final String body;
    public final Map<String, String> headers;
    public final Map<String, String> cookies;

    @SuppressWarnings("unchecked")
    public Map<String, Object> toJson() {
        try {
            return new Gson().fromJson(body, Map.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
}
