package net.mitask.requests;

import net.mitask.util.HttpMethod;

import java.util.ArrayList;
import java.util.List;

public class Router {
    protected final List<Route> routes = new ArrayList<>();

    public void get(String path, Route.RouteHandler handler) {
        routes.add(new Route(HttpMethod.GET, path, handler));
    }

    public void post(String path, Route.RouteHandler handler) {
        routes.add(new Route(HttpMethod.POST, path, handler));
    }

    public void use(String path, Router router) {
        router.routes.forEach(route -> routes.add(new Route(route.method, path + route.pathPattern, route.handler)));
    }
}
