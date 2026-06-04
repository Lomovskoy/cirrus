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
    public static final int DEFAULT_SO_TIMEOUT = 0;

    @Builder.Default
    private final int port = DEFAULT_PORT;
    @Builder.Default
    private final int maxHeaderSize = DEFAULT_MAX_HEADER_SIZE;
    @Builder.Default
    private final int maxBodySize = DEFAULT_MAX_BODY_SIZE;
    @Builder.Default
    private final int backlog = DEFAULT_BACKLOG;
    @Builder.Default
    private final boolean tcpNoDelay = DEFAULT_TCP_NO_DELAY;
    @Builder.Default
    private int soTimeout = DEFAULT_SO_TIMEOUT; // 0 means infinite wait


    /**
     * Creates a configuration from System properties (vts.port, vts.max.header.size, vts.max.body.size)
     * If the property is not specified, default values are used.
     */
    public static ServerConfig fromSystemProperties() {
        var builder = ServerConfig.builder();

        var portProp = System.getProperty("vts.port");
        if (portProp != null) {
            try {
                builder.port(Integer.parseInt(portProp));
            } catch (NumberFormatException ignored) {
            }
        }

        var maxHeaderProp = System.getProperty("vts.max.header.size");
        if (maxHeaderProp != null) {
            try {
                builder.maxHeaderSize(Integer.parseInt(maxHeaderProp));
            } catch (NumberFormatException ignored) {
            }
        }

        var maxBodyProp = System.getProperty("vts.max.body.size");
        if (maxBodyProp != null) {
            try {
                builder.maxBodySize(Integer.parseInt(maxBodyProp));
            } catch (NumberFormatException ignored) {
            }
        }

        return builder.build();
    }
}
