package org.server.virtual.threads.server.config;

public class TestServerConfig {

    public static ServerConfig withSmallLimits() {
        return ServerConfig.builder()
                .maxHeaderSize(128)    // very small for error checking
                .maxBodySize(256)
                .build();
    }

    public static ServerConfig withDefaultLimits() {
        return ServerConfig.builder().build();
    }
}
