package org.server.virtual.threads.core.router;


import org.server.virtual.threads.core.handler.RouteHandler;
import org.server.virtual.threads.core.model.HttpMethod;
import org.server.virtual.threads.core.model.HttpRequest;
import org.server.virtual.threads.core.model.HttpResponse;

import java.util.Map;

public interface Router {
    Router get(String path, RouteHandler handler);

    Router post(String path, RouteHandler handler);

    Router put(String path, RouteHandler handler);

    Router delete(String path, RouteHandler handler);

    RouteHandler findHandler(HttpMethod method, String path, Map<String, String> pathParams);

    void handle(HttpRequest request, HttpResponse response) throws Exception;
}
