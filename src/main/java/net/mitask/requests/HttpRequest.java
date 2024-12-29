package net.mitask.requests;

import java.util.Map;

public class HttpRequest {
    public final String method;
    public final String path;
    public final Map<String, String> queryParams;
    public final Map<String, String> urlParams;
    public final String body;

    public HttpRequest(String method, String path, Map<String, String> queryParams, String body, Map<String, String> urlParams) {
        this.method = method;
        this.path = path;
        this.queryParams = queryParams;
        this.urlParams = urlParams;
        this.body = body;
    }
}