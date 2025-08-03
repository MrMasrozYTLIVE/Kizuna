package dev.mitask.requests;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.AllArgsConstructor;
import dev.mitask.util.HttpMethod;

import java.util.Map;

/**
 * This class contains all info from the received request
 * @since 1.0.0
 * @author MiTask
 */
@AllArgsConstructor
public class HttpRequest {
    public final HttpMethod method;
    public final String path;
    public final Map<String, String> queryParams;
    public final Map<String, String> urlParams;
    public final String body;
    public final Map<String, String> headers;
    public final Map<String, String> cookies;
    public final String IP;

    @SuppressWarnings("unchecked")
    public Map<String, Object> toJson() {
        if(method != HttpMethod.POST || body == null || body.isBlank()) return null;

        try {
            return new Gson().fromJson(body, Map.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
}
