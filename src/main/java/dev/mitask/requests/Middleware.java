package dev.mitask.requests;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class Middleware {
    private final Iterator<MiddlewareHandler> iterator;
    private final Route.RouteHandler finalHandler;

    public Middleware(List<MiddlewareHandler> middlewares, Route.RouteHandler finalHandler) {
        this.iterator = middlewares.iterator();
        this.finalHandler = finalHandler;
    }

    public void next(HttpRequest req, HttpResponse res) throws IOException {
        if (iterator.hasNext()) {
            MiddlewareHandler current = iterator.next();
            current.handle(req, res, this);
        } else {
            finalHandler.handle(req, res);
        }
    }

    @FunctionalInterface
    public interface MiddlewareHandler {
        void handle(HttpRequest req, HttpResponse res, Middleware chain) throws IOException;
    }
}
