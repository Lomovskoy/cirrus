package org.server.virtual.threads;

import org.server.virtual.threads.core.handler.RouteHandler;

/**
 * The server's public interface. The user interacts with it.
 */
public interface CirrusServer {
    CirrusServer get(String path, RouteHandler handler);

    CirrusServer post(String path, RouteHandler handler);

    CirrusServer put(String path, RouteHandler handler);

    CirrusServer delete(String path, RouteHandler handler);

    void start() throws Exception;

    void stop();

    int getPort();
}
