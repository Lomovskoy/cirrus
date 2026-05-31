package org.server.virtual.threads.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * HTTP request. Immutable object.
 */
@Getter
public class HttpRequest {
    private final HttpMethod method;
    private final String path;
    private final Map<String, String> params;
    private final Map<String, String> headers;
    private final String body;

    public HttpRequest(HttpMethod method, String path, Map<String, String> params, Map<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.params = Map.copyOf(params != null ? params : Map.of());
        this.headers = Map.copyOf(headers != null ? headers : Map.of());
        this.body = body != null ? body : "";
    }

    // Alternative constructor for simple cases (without parameters and body)
    public HttpRequest(HttpMethod method, String path) {
        this(method, path, Map.of(), Map.of(), "");
    }

    public String getParam(String name) { return params.get(name); }
    public String getHeader(String name) { return headers.get(name.toLowerCase()); }
}
