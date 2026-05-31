package org.server.virtual.threads.core.model;

import lombok.Getter;

@Getter
public enum ContentType {

    TEXT("text/plain; charset=utf-8"),
    HTML("text/html; charset=utf-8"),
    JSON("application/json; charset=utf-8"),
    XML("application/xml; charset=utf-8"),
    CSS("text/css; charset=utf-8"),
    JAVASCRIPT("application/javascript; charset=utf-8"),
    FORM("application/x-www-form-urlencoded; charset=utf-8"),
    MULTIPART("multipart/form-data"),
    EVENT_STREAM("text/event-stream; charset=utf-8"),
    OCTET_STREAM("application/octet-stream");

    private final String value;

    ContentType(String value) {
        this.value = value;
    }
}
