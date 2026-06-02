package org.server.virtual.threads.core.model;

import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.server.virtual.threads.core.constants.HttpConstants.HEADER_CONTENT_LENGTH;
import static org.server.virtual.threads.core.constants.HttpConstants.HEADER_CONTENT_TYPE;

/**
 * HTTP response. Mutable – built gradually.
 */
@Getter
public class HttpResponse {
    private HttpStatus status;
    private final Map<String, String> headers;
    private byte[] body;

    public HttpResponse() {
        this.status = HttpStatus.OK;
        this.headers = new LinkedHashMap<>();
        this.body = new byte[0];
    }

    public HttpResponse status(HttpStatus status) {
        this.status = status;
        return this;
    }

    // header by name
    public HttpResponse header(String name, String value) {
        if (name.equalsIgnoreCase(HEADER_CONTENT_LENGTH) && body.length > 0) {
            throw new IllegalStateException("Cannot set Content-Length after body is set");
        }

        if (value == null) {
            headers.remove(name.toLowerCase());
        } else {
            headers.put(name.toLowerCase(), value);
        }
        return this;
    }

    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    // Content-Type enum
    public HttpResponse type(ContentType contentType) {
        return header(HEADER_CONTENT_TYPE, contentType.getValue());
    }

    // Content-Type line (for custom)
    public HttpResponse type(String contentType) {
        return header(HEADER_CONTENT_TYPE, contentType);
    }

    public HttpResponse body(byte[] body) {
        header(HEADER_CONTENT_LENGTH, String.valueOf(body.length));
        this.body = body.clone();
        return this;
    }

    public HttpResponse body(String body) {
        return body(body.getBytes(StandardCharsets.UTF_8));
    }

    public String bodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    // Convenience methods with default Content-Type
    public HttpResponse getText(String content) {
        type(ContentType.TEXT);
        return body(content);
    }

    public HttpResponse getHtml(String content) {
        type(ContentType.HTML);
        return body(content);
    }

    public HttpResponse getJson(String content) {
        type(ContentType.JSON);
        return body(content);
    }

    public HttpResponse getXml(String content) {
        type(ContentType.XML);
        return body(content);
    }

    public HttpResponse getCss(String content) {
        type(ContentType.CSS);
        return body(content);
    }

    public HttpResponse getJs(String content) {
        type(ContentType.JAVASCRIPT);
        return body(content);
    }
}
