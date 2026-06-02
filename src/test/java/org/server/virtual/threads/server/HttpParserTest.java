package org.server.virtual.threads.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.server.virtual.threads.core.model.HttpMethod;
import org.server.virtual.threads.server.config.ServerConfig;
import org.server.virtual.threads.server.config.TestServerConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;


class HttpParserTest {

    // ======================== POSITIVE TESTS ========================

    @Test
    @DisplayName("Parsing a simple GET request without headers or body")
    void shouldParseSimpleGetRequest() throws IOException {
        var rawRequest = "GET /index.html HTTP/1.0\r\n\r\n";
        var parser = new HttpParser(TestServerConfig.withDefaultLimits());

        var request = parser.parse(toInputStream(rawRequest));

        assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(request.getPath()).isEqualTo("/index.html");
        assertThat(request.getBody()).isEmpty();
        assertTrue(request.getHeaders().isEmpty());
    }

    @Test
    @DisplayName("Parsing a GET request with query parameters (should be discarded)")
    void shouldStripQueryParametersFromGetRequest() throws IOException {
        var rawRequest = "GET /search?q=java&page=2 HTTP/1.0\r\n\r\n";
        var parser = new HttpParser(TestServerConfig.withDefaultLimits());

        var request = parser.parse(toInputStream(rawRequest));

        assertThat(request.getPath()).isEqualTo("/search");
    }

    @Test
    @DisplayName("Parsing a POST request with body and Content-Length")
    void shouldParsePostRequestWithBody() throws IOException {
        var body = "name=John&age=30";
        var rawRequest = "POST /users HTTP/1.0\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Content-Type: application/x-www-form-urlencoded\r\n" +
                "\r\n" +
                body;
        var parser = new HttpParser(TestServerConfig.withDefaultLimits());

        var request = parser.parse(toInputStream(rawRequest));

        assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
        assertThat(request.getPath()).isEqualTo("/users");
        assertThat(request.getBody()).isEqualTo(body);
        assertThat(request.getHeader("content-type")).isEqualTo("application/x-www-form-urlencoded");
        assertThat(request.getHeader("content-length")).isEqualTo(String.valueOf(body.length()));
    }

    @Test
    @DisplayName("Parsing a request without the HTTP version (method and path only)")
    void shouldParseRequestWithoutHttpVersion() throws IOException {
        var rawRequest = "GET /about\r\nHost: localhost\r\n\r\n";
        var parser = new HttpParser(TestServerConfig.withDefaultLimits());

        var request = parser.parse(toInputStream(rawRequest));

        assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(request.getPath()).isEqualTo("/about");
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /test",
            "POST, /api/data",
            "PUT, /resource/1",
            "DELETE, /resource/1",
            "HEAD, /health",
            "OPTIONS, /"
    })
    @DisplayName("Parsing all supported HTTP methods")
    void shouldParseAllHttpMethods(String method, String path) throws IOException {
        var rawRequest = method + " " + path + " HTTP/1.0\r\n\r\n";
        var parser = new HttpParser(TestServerConfig.withDefaultLimits());

        var request = parser.parse(toInputStream(rawRequest));

        assertThat(request.getMethod().name()).isEqualTo(method);
        assertThat(request.getPath()).isEqualTo(path);
    }

    @Test
    @DisplayName("Headers are converted to lowercase and parsed correctly.")
    void headersAreNormalizedToLowerCase() throws IOException {
        String rawRequest = "GET /test HTTP/1.0\r\n" +
                "Content-Type: application/json\r\n" +
                "X-Custom-Header: SomeValue\r\n" +
                "Accept-Language: ru-RU\r\n" +
                "\r\n";

        var parser = new HttpParser(TestServerConfig.withDefaultLimits());
        var request = parser.parse(toInputStream(rawRequest));

        assertThat(request.getHeader("content-type")).isEqualTo("application/json");
        assertThat(request.getHeader("CONTENT-TYPE")).isEqualTo("application/json"); // регистронезависимо
        assertThat(request.getHeader("x-custom-header")).isEqualTo("SomeValue");
        assertThat(request.getHeader("accept-language")).isEqualTo("ru-RU");
    }

    @Test
    @DisplayName("Empty body if Content-Length is missing")
    void emptyBodyWhenNoContentLength() throws IOException {
        var rawRequest = "POST /submit HTTP/1.0\r\n\r\n";
        var parser = new HttpParser(TestServerConfig.withDefaultLimits());

        var request = parser.parse(toInputStream(rawRequest));

        assertThat(request.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Content-Length = 0 gives an empty body")
    void zeroContentLengthGivesEmptyBody() throws IOException {
        var rawRequest = "POST /submit HTTP/1.0\r\n" +
                "Content-Length: 0\r\n" +
                "\r\n";

        var parser = new HttpParser(TestServerConfig.withDefaultLimits());
        var request = parser.parse(toInputStream(rawRequest));

        assertThat(request.getBody()).isEmpty();
    }

    // ======================== NEGATIVE TESTS ========================

    @Test
    @DisplayName("Empty request -> exception")
    void emptyRequestThrowsException() {
        var rawRequest = "";
        var parser = new HttpParser(TestServerConfig.withDefaultLimits());

        assertThatThrownBy(() -> parser.parse(toInputStream(rawRequest)))
                .isInstanceOf(IOException.class)
                .hasMessage("Empty request");
    }

    @Test
    @DisplayName("Query with spaces only -> exception")
    void blankRequestThrowsException() {
        var rawRequest = "   \r\n\r\n";
        var parser = new HttpParser(TestServerConfig.withDefaultLimits());

        assertThatThrownBy(() -> parser.parse(toInputStream(rawRequest)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid request line");
    }

    @Test
    @DisplayName("Unknown HTTP method -> exception")
    void unknownHttpMethodThrowsException() {
        var rawRequest = "UNKNOWN /path HTTP/1.0\r\n\r\n";
        var parser = new HttpParser(TestServerConfig.withDefaultLimits());

        assertThatThrownBy(() -> parser.parse(toInputStream(rawRequest)))
                .isInstanceOf(IOException.class)
                .hasMessage("Unknown HTTP method: UNKNOWN");
    }

    @ParameterizedTest
    @CsvSource({
            "GET /test HTTP/1.0,                      true",
            "GET /test HTTP/1.1,                      false",
            "GET /test HTTP/2.0,                      false",
            "GET /test HTTP/0.9,                      false",
            "GET /test HTTP/3.0,                      false",
            "GET /test HTTP/1.0extra,                 false",
            "GET /test HTTP/1.0?param,                false",
            "GET /test,                               true",   // without a version, do we consider HTTP/1.0
            "GET /test HTTP/1,                        false",
            "GET /test HTTP/abc,                      false",
            "GET /test FTP/1.0,                       false",
            "GET /test HTTP/,                         false",
            "GET /test,                               true"
    })
    @DisplayName("Incorrect HTTP version -> exception")
    void onlyHttp10IsAccepted(String requestLine, boolean shouldPass) throws IOException {
        var rawRequest = requestLine + "\r\n\r\n";
        var parser = new HttpParser(TestServerConfig.withDefaultLimits());

        if (shouldPass) {
            var request = parser.parse(toInputStream(rawRequest));
            assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
            assertThat(request.getPath()).isEqualTo("/test");
        } else {
            assertThatThrownBy(() -> parser.parse(toInputStream(rawRequest)))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Invalid HTTP version");
        }
    }

    @Test
    @DisplayName("Invalid Content-Length (not a number) -> exception")
    void invalidContentLengthThrowsException() {
        var rawRequest = "POST /test HTTP/1.0\r\n" +
                "Content-Length: abc\r\n" +
                "\r\n";

        var parser = new HttpParser(TestServerConfig.withDefaultLimits());

        assertThatThrownBy(() -> parser.parse(toInputStream(rawRequest)))
                .isInstanceOf(IOException.class)
                .hasMessage("Invalid Content-Length: abc");
    }

    @Test
    @DisplayName("Negative Content-Length -> body is not readable")
    void negativeContentLengthIgnored() throws IOException {
        var rawRequest = "POST /test HTTP/1.0\r\n" +
                "Content-Length: -10\r\n" +
                "\r\n";

        var parser = new HttpParser(TestServerConfig.withDefaultLimits());
        var request = parser.parse(toInputStream(rawRequest));

        assertThat(request.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Body is larger than limit -> exception")
    void bodyExceedsMaxSizeThrowsException() {
        int maxBodySize = 128;
        var config = ServerConfig.builder().maxBodySize(maxBodySize).build();
        var body = "x".repeat(maxBodySize + 1);
        var rawRequest = "POST /test HTTP/1.0\r\n" +
                "Content-Length: " + (maxBodySize + 1) + "\r\n" +
                "\r\n" +
                body;

        var parser = new HttpParser(config);

        assertThatThrownBy(() -> parser.parse(toInputStream(rawRequest)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Body too large")
                .hasMessageContaining(String.valueOf(maxBodySize));
    }

    @Test
    @DisplayName("Title exceeds limit -> exception")
    void headerExceedsMaxSizeThrowsException() {
        int maxHeaderSize = 32;
        var config = ServerConfig.builder().maxHeaderSize(maxHeaderSize).build();
        var longHeader = "X-Long: " + "a".repeat(maxHeaderSize) + "\r\n";
        var rawRequest = "GET /test HTTP/1.0\r\n" + longHeader + "\r\n";

        var parser = new HttpParser(config);

        assertThatThrownBy(() -> parser.parse(toInputStream(rawRequest)))
                .isInstanceOf(IOException.class)
                .hasMessage("Header too large: " + maxHeaderSize + " bytes");
    }

    @Test
    @DisplayName("There is no empty line after the headers -> parsing should complete (the body is empty)")
    void missingEmptyLineAfterHeadersStillParses() throws IOException {
        // The request is invalid, but the parser must read everything to the end of the stream.
        var rawRequest = "GET /test HTTP/1.0\r\nHost: localhost";
        var parser = new HttpParser(TestServerConfig.withDefaultLimits());

        // There should be no exception, the body is just empty
        var request = parser.parse(toInputStream(rawRequest));

        assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(request.getPath()).isEqualTo("/test");
    }

    // ======================== BOUNDARY TESTS ========================

    @Test
    @DisplayName("The body is exactly the maximum size")
    void bodyExactlyMaxSize() throws IOException {
        int maxBodySize = 100;
        var config = ServerConfig.builder().maxBodySize(maxBodySize).build();
        var body = "x".repeat(maxBodySize);
        var rawRequest = "POST /test HTTP/1.0\r\n" +
                "Content-Length: " + maxBodySize + "\r\n" +
                "\r\n" +
                body;

        var parser = new HttpParser(config);
        var request = parser.parse(toInputStream(rawRequest));

        assertThat(request.getBody()).hasSize(maxBodySize);
        assertThat(request.getBody()).isEqualTo(body);
    }

    @Test
    @DisplayName("The title is exactly the maximum size")
    void headerExactlyMaxSize() throws IOException {
        int maxHeaderSize = 50;
        var config = ServerConfig.builder().maxHeaderSize(maxHeaderSize).build();
        // Header name + ": " + value must fit exactly within the limit
        var headerValue = "x".repeat(maxHeaderSize - 8); // "X-Test: " takes up 8 characters
        var rawRequest = "GET /test HTTP/1.0\r\n" +
                "X-Test: " + headerValue + "\r\n" +
                "\r\n";

        var parser = new HttpParser(config);
        // There should be no exception
        var request = parser.parse(toInputStream(rawRequest));

        assertThat(request.getHeader("x-test")).isEqualTo(headerValue);
    }

    @Test
    @DisplayName("Header without colon should be ignored")
    void headerWithoutColonIgnored() throws IOException {
        var rawRequest = "GET /test HTTP/1.0\r\n" +
                "InvalidHeader\r\n" +
                "X-Valid: value\r\n" +
                "\r\n";

        var parser = new HttpParser(TestServerConfig.withDefaultLimits());
        var request = parser.parse(toInputStream(rawRequest));

        // The "InvalidHeader" header without a colon should be omitted
        assertThat(request.getHeader("InvalidHeader")).isNull();
        assertThat(request.getHeader("X-Valid")).isEqualTo("value");
    }

    @Test
    @DisplayName("Header with colon at position 0 should be ignored")
    void headerColonAtStartIgnored() throws IOException {
        String rawRequest = "GET /test HTTP/1.0\r\n" +
                ": value\r\n" +
                "X-Valid: value2\r\n" +
                "\r\n";

        var parser = new HttpParser(TestServerConfig.withDefaultLimits());
        var request = parser.parse(toInputStream(rawRequest));

        // The ":value" header should not be added
        assertThat(request.getHeader("")).isNull();
        assertThat(request.getHeader("X-Valid")).isEqualTo("value2");
    }

    @Test
    @DisplayName("Should throw IOException when body is shorter than Content-Length")
    void bodyShorterThanContentLength() {
        String rawRequest = "POST /test HTTP/1.0\r\n" +
                "Content-Length: 100\r\n" +
                "\r\n" +
                "Short body"; // реальная длина 10 байт, а заголовок обещает 100

        HttpParser parser = new HttpParser(TestServerConfig.withDefaultLimits());

        assertThatThrownBy(() -> parser.parse(toInputStream(rawRequest)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Premature EOF");
    }
    // ======================== AUXILIARY METHODS ========================

    private InputStream toInputStream(String raw) {
        return new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII));
    }
}
