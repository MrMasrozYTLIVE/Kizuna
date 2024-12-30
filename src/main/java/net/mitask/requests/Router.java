package net.mitask.requests;

import net.mitask.util.HttpMethod;

import java.util.ArrayList;
import java.util.List;

public class Router {
    protected final List<Route> routes = new ArrayList<>();

    public Route get(String path, Route.RouteHandler handler) {
        Route route = new Route(HttpMethod.GET, path, handler);
        routes.add(route);
        return route;
    }

    public Route post(String path, Route.RouteHandler handler) {
        Route route = new Route(HttpMethod.POST, path, handler);
        routes.add(route);
        return route;
    }

    public void use(String path, Router router) {
        router.routes.forEach(route -> routes.add(new Route(route.method, path + route.pathPattern, route.handler)));
    }
}
