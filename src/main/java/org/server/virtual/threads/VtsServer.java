package org.server.virtual.threads;

import org.server.virtual.threads.core.handler.RouteHandler;

/**
 * The server's public interface. The user interacts with it.
 */
public interface VtsServer {
    VtsServer get(String path, RouteHandler handler);

    VtsServer post(String path, RouteHandler handler);

    VtsServer put(String path, RouteHandler handler);

    VtsServer delete(String path, RouteHandler handler);

    void start() throws Exception;

    void stop();

    int getPort();
}
