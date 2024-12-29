package net.mitask.requests;

import lombok.Getter;
import net.mitask.util.HttpMethod;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route {
    final String pathPattern;
    final Pattern compiledPattern;
    @Getter final HttpMethod method;
    @Getter final RouteHandler handler;

    public Route(HttpMethod method, String pathPattern, RouteHandler handler) {
        this.method = method;
        this.pathPattern = pathPattern;
        this.handler = handler;
        this.compiledPattern = Pattern.compile(pathPattern.replaceAll(":([a-zA-Z_][a-zA-Z0-9_]*)", "(?<$1>[^/]+)").replace("*", ".*"));
    }

    public boolean matches(String path, Map<String, String> urlParams) {
        Matcher matcher = compiledPattern.matcher(path);
        if (!matcher.matches()) return false;

        for (String groupName : compiledPattern.pattern().split("/")) {
            if(!groupName.startsWith("(?<")) continue;

            String paramName = groupName.substring(3, groupName.length() - 3);
            urlParams.put(paramName, matcher.group(paramName));
        }

        return true;
    }

    @FunctionalInterface
    public interface RouteHandler {
        void handle(HttpRequest request, HttpResponse response) throws IOException;
    }

    @FunctionalInterface
    public interface AdvancedRouteHandler {
        void handle(HttpRequest request, HttpResponse response, Object... data) throws IOException;
    }
}
