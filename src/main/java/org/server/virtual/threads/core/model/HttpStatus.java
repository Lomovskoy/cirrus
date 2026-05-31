package org.server.virtual.threads.core.model;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
public enum HttpStatus {
    // 2xx Success
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NO_CONTENT(204, "No Content"),

    // 3xx Redirection
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),

    // 4xx Client Error
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),

    // 5xx Server Error
    INTERNAL_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented");

    private final int code;
    private final String reason;
    private static final Map<Integer, HttpStatus> CODE_MAP;

    static {
        Map<Integer, HttpStatus> map = new HashMap<>();
        for (HttpStatus status : values()) {
            map.put(status.code, status);
        }
        CODE_MAP = Collections.unmodifiableMap(map); // immutable for security
    }

    HttpStatus(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    /**
     * Fast HTTP code search.
     * O(1) complexity instead of O(n) when iterating over values()
     */
    public static HttpStatus fromCode(int code) {
        return CODE_MAP.get(code);
    }

    /**
     * Checks if a status with the given code exists.
     */
    public static boolean exists(int code) {
        return CODE_MAP.containsKey(code);
    }
}
