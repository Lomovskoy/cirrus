package org.server.virtual.threads.server.config;

import lombok.Builder;
import lombok.Getter;

/**
 * Server configuration.
 * All parameters have default values.
 */
@Getter
@Builder
public class ServerConfig {

    // Default values
    public static final int DEFAULT_PORT = 8080;
    public static final int DEFAULT_MAX_HEADER_SIZE = 8192;    // 8KB
    public static final int DEFAULT_MAX_BODY_SIZE = 10_485_760; // 10MB
    public static final int DEFAULT_BACKLOG = 50000;
    public static final boolean DEFAULT_TCP_NO_DELAY = true;

    private int port = DEFAULT_PORT;
    private int maxHeaderSize = DEFAULT_MAX_HEADER_SIZE;
    private int maxBodySize = DEFAULT_MAX_BODY_SIZE;
    private int backlog = DEFAULT_BACKLOG;
    private boolean tcpNoDelay = DEFAULT_TCP_NO_DELAY;

    // Reading from System properties
    public ServerConfig() {
        String maxHeaderProp = System.getProperty("vts.max.header.size");
        if (maxHeaderProp != null) {
            try {
                this.maxHeaderSize = Integer.parseInt(maxHeaderProp);
            } catch (NumberFormatException ignored) {}
        }

        String maxBodyProp = System.getProperty("vts.max.body.size");
        if (maxBodyProp != null) {
            try {
                this.maxBodySize = Integer.parseInt(maxBodyProp);
            } catch (NumberFormatException ignored) {}
        }

        String portProp = System.getProperty("vts.port");
        if (portProp != null) {
            try {
                this.port = Integer.parseInt(portProp);
            } catch (NumberFormatException ignored) {}
        }
    }
}
