package org.server.virtual.threads.core.model;

import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

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
        if (value == null) {
            headers.remove(name.toLowerCase());
        } else {
            headers.put(name.toLowerCase(), value);
        }
        return this;
    }

    public String header(String name) {
        return headers.get(name.toLowerCase());
    }

    // Content-Type enum
    public HttpResponse type(ContentType contentType) {
        return header("Content-Type", contentType.getValue());
    }

    // Content-Type line (for custom)
    public HttpResponse type(String contentType) {
        return header("Content-Type", contentType);
    }

    // Тело
    public HttpResponse body(byte[] body) {
        this.body = body.clone();
        header("Content-Length", String.valueOf(body.length));
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
