package org.server.virtual.threads.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class HttpParserConstructorTest {

    private Properties originalProperties;

    @BeforeEach
    void saveOriginalProperties() {
        originalProperties = (Properties) System.getProperties().clone();
    }

    @AfterEach
    void restoreProperties() {
        System.setProperties(originalProperties);
    }

    @Test
    @DisplayName("Default constructor uses system properties")
    void defaultConstructorUsesSystemProperties() {
        System.setProperty("cirrus.max.header.size", "4096");
        System.setProperty("cirrus.max.body.size", "2097152");

        var parser = new HttpParser(); // this(ServerConfig.fromSystemProperties())

        // You can check that the parser uses these values using reflection or by adding getters to HttpParser.
        // If there are no getters, we'll test the behavior: send a request that exceeds the limit and catch the exception.
        var rawRequest = "GET /test HTTP/1.0\r\n" +
                "X-Large: " + "x".repeat(5000) + "\r\n\r\n";
        var input = new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(IOException.class)
                .hasMessage("Header too large: 4096 bytes");
    }

    @Test
    @DisplayName("Default constructor uses default limits when system properties absent")
    void defaultConstructorUsesDefaultLimits() {
        System.clearProperty("cirrus.max.header.size");
        System.clearProperty("cirrus.max.body.size");

        var parser = new HttpParser();

        var rawRequest = "GET /test HTTP/1.0\r\n" +
                "X-Large: " + "x".repeat(9000) + "\r\n\r\n";
        var input = new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.US_ASCII));

        // By default, maxHeaderSize = 8192, so 9000 bytes will cause an exception
        assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(IOException.class)
                .hasMessage("Header too large: 8192 bytes");
    }
}
