package org.server.virtual.threads.core.router;

import org.server.virtual.threads.core.handler.RouteHandler;
import org.server.virtual.threads.core.model.HttpMethod;
import org.server.virtual.threads.core.model.HttpRequest;
import org.server.virtual.threads.core.model.HttpResponse;
import org.server.virtual.threads.core.model.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.server.virtual.threads.core.constants.HttpConstants.COLON;
import static org.server.virtual.threads.core.constants.HttpConstants.SLASH;

public class TrieRouter implements Router {

    private static class Node {
        final boolean isParam;
        final String paramName;
        final Map<String, Node> children = new ConcurrentHashMap<>();
        final Map<HttpMethod, RouteHandler> handlers = new ConcurrentHashMap<>();

        Node() {
            this(false, null);
        }

        Node(boolean isParam, String paramName) {
            this.isParam = isParam;
            this.paramName = paramName;
        }
    }

    private final Node root = new Node();

    private void addRoute(HttpMethod method, String path, RouteHandler handler) {
        var node = root;
        var segments = path.split(SLASH);
        for (var segment : segments) {
            if (segment.isEmpty()) continue;
            if (segment.startsWith(COLON)) {
                var paramName = segment.substring(1);
                node = node.children.computeIfAbsent(segment, _ -> new Node(true, paramName));
            } else {
                node = node.children.computeIfAbsent(segment, _ -> new Node(false, null));
            }
        }
        node.handlers.put(method, handler);
    }

    @Override
    public Router get(String path, RouteHandler handler) {
        addRoute(HttpMethod.GET, path, handler);
        return this;
    }

    @Override
    public Router post(String path, RouteHandler handler) {
        addRoute(HttpMethod.POST, path, handler);
        return this;
    }

    @Override
    public Router put(String path, RouteHandler handler) {
        addRoute(HttpMethod.PUT, path, handler);
        return this;
    }

    @Override
    public Router delete(String path, RouteHandler handler) {
        addRoute(HttpMethod.DELETE, path, handler);
        return this;
    }

    /**
     * Finds a handler for the specified HTTP method and request path tree.
     * Two types of segments are supported:
     * Static — exact match, e.g. {@code "/users"}</li>
     * Parametric — start with a colon, e.g. {@code "/:id"}, placed in {@code pathParams}
     *
     * @param method HTTP method (GET, POST, ...) — case-sensitive.
     * @param path request path, starting with '/', e.g. {@code "/users/123"}.
     * @param pathParams {@link Map} for storing found path parameters
     * (mutable, will be populated during the search).
     * @return handler for the given method and path, or {@code null} if the route is not found.
     */
    @Override
    public RouteHandler findHandler(HttpMethod method, String path, Map<String, String> pathParams) {
        var node = root;
        var segments = path.split(SLASH);
        for (var segment : segments) {
            if (segment.isEmpty()) continue;
            var next = node.children.get(segment);
            if (next == null) {
                // looking for a parametric node
                for (var child : node.children.values()) {
                    if (child.isParam) {
                        pathParams.put(child.paramName, segment);
                        next = child;
                        break;
                    }
                }
                if (next == null) return null;
            }
            node = next;
        }
        return node.handlers.get(method);
    }

    /**
     * Processes an incoming HTTP request, routing it to the appropriate handler.
     * <p>
     * Creates an empty map for path parameters.
     * Calls {@link #findHandler} to search for a handler based on the request method and path.
     * If a handler is found:
     * Creates a new {@link HttpRequest} object, enriched with the path parameters.
     * Calls {@link RouteHandler#handle(HttpRequest, HttpResponse)}.
     * If a handler is not found, sets the response status to 404 Not Found
     * and a text message indicating the method and path.
     * <p>
     * Important: This method does not catch exceptions thrown by the handler.
     * They should be handled at a higher level (for example, in {@code VirtualThreadServer}).
     *
     * @param request - incoming HTTP request (method, path, headers, body).
     * @param response - empty response object, which will be filled by the handler
     * or, in case of an error, filled with a 404 response code.
     * @throws Exception - if the handler throws an exception, it is rethrown.
     *
     * @see #findHandler
     * @see RouteHandler#handle(HttpRequest, HttpResponse)
     */
    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        var pathParams = new HashMap<String, String>();
        var handler = findHandler(request.getMethod(), request.getPath(), pathParams);
        if (handler != null) {
            // Creating a new request with path parameters
            var enrichedRequest = new HttpRequest(
                    request.getMethod(),
                    request.getPath(),
                    pathParams,
                    request.getHeaders(),
                    request.getBody()
            );
            handler.handle(enrichedRequest, response);
        } else {
            response.status(HttpStatus.NOT_FOUND)
                    .getText(String.format(
                            "%d: %s",
                            HttpStatus.NOT_FOUND.getCode(),
                            HttpStatus.NOT_FOUND.getReason()
                    ) + request.getMethod() + " " + request.getPath());
        }
    }
}
