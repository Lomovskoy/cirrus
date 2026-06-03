package org.server.virtual.threads.core.handler;

import org.server.virtual.threads.core.model.HttpRequest;
import org.server.virtual.threads.core.model.HttpResponse;

@FunctionalInterface
public interface RouteHandler {
    void handle(HttpRequest request, HttpResponse response) throws Exception;
}