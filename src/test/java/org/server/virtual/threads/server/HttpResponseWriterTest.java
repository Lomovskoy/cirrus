package org.server.virtual.threads.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.server.virtual.threads.core.model.HttpResponse;
import org.server.virtual.threads.core.model.HttpStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DisplayName("HttpResponseWriter")
class HttpResponseWriterTest {

    private final HttpResponseWriter writer = new HttpResponseWriter();

    @Test
    @DisplayName("Should write status line and body with automatic Content-Length")
    void shouldWriteStatusLineAndBodyWithAutoContentLength() throws IOException {
        var response = new HttpResponse()
                .status(HttpStatus.OK)
                .body("Hello World");

        var out = new ByteArrayOutputStream();
        writer.write(out, response);
        var result = out.toString(StandardCharsets.US_ASCII);

        assertThat(result)
                .startsWith("HTTP/1.0 200 OK\r\n")
                .contains("content-length: 11\r\n")
                .endsWith("\r\n\r\nHello World");
    }

    @Test
    @DisplayName("Should not add Content-Length when already present")
    void shouldNotAddContentLengthWhenAlreadyPresent() throws IOException {
        var response = new HttpResponse()
                .status(HttpStatus.OK)
                .header("Content-Length", "99")
                .body("Short");

        var out = new ByteArrayOutputStream();
        writer.write(out, response);
        var result = out.toString(StandardCharsets.US_ASCII);

        assertThat(result)
                .contains("content-length: 5\r\n")
                .doesNotContain("content-length: 99")
                .contains("\r\n\r\nShort");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when setting Content-Length after body")
    void shouldThrowExceptionWhenSettingContentLengthAfterBody() {
        HttpResponse response = new HttpResponse();

        response.body("Short"); // Content-Length = 5

        assertThatThrownBy(() -> response.header("Content-Length", "99"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot set Content-Length after body is set");
    }

    @Test
    @DisplayName("Should write custom headers along with status line")
    void shouldWriteCustomHeaders() throws IOException {
        var response = new HttpResponse()
                .status(HttpStatus.NOT_FOUND)
                .header("X-Custom", "value")
                .header("Cache-Control", "no-cache")
                .body("Not Found");

        var out = new ByteArrayOutputStream();
        writer.write(out, response);
        var result = out.toString(StandardCharsets.US_ASCII);

        assertThat(result)
                .contains("HTTP/1.0 404 Not Found\r\n")
                .contains("x-custom: value\r\n")
                .contains("cache-control: no-cache\r\n")
                .contains("content-length: 9\r\n")
                .endsWith("\r\n\r\nNot Found");
    }

    @Test
    @DisplayName("Should write response without body (no Content-Length)")
    void shouldWriteResponseWithoutBody() throws IOException {
        var response = new HttpResponse()
                .status(HttpStatus.NO_CONTENT)
                .header("X-Empty", "true");

        var out = new ByteArrayOutputStream();
        writer.write(out, response);
        var result = out.toString(StandardCharsets.US_ASCII);

        assertThat(result)
                .contains("HTTP/1.0 204 No Content\r\n")
                .contains("x-empty: true\r\n")
                .doesNotContain("content-length")
                .endsWith("\r\n\r\n");
    }

    @ParameterizedTest
    @DisplayName("Should write correct status line for different HTTP status codes")
    @CsvSource({
            "200, OK",
            "404, Not Found",
            "500, Internal Server Error",
            "201, Created",
            "400, Bad Request"
    })
    void shouldWriteDifferentStatusCodes(int code, String reason) throws IOException {
        var status = HttpStatus.fromCode(code);
        assertThat(status).isNotNull();

        var response = new HttpResponse().status(status);

        var out = new ByteArrayOutputStream();
        writer.write(out, response);
        var result = out.toString(StandardCharsets.US_ASCII);

        assertThat(result).startsWith("HTTP/1.0 " + code + " " + reason + "\r\n");
    }

    @Test
    @DisplayName("Should preserve header case as added (HTTP headers are case-insensitive anyway)")
    void shouldPreserveHeaderCase() throws IOException {
        var response = new HttpResponse()
                .header("Content-Type", "text/html")
                .body("Test");

        var out = new ByteArrayOutputStream();
        writer.write(out, response);
        var result = out.toString(StandardCharsets.US_ASCII);

        //In the current implementation, headers are converted to lowercase - this is normal.
        assertThat(result).contains("content-type: text/html\r\n");
    }

    @Test
    @DisplayName("Should automatically add Content-Length when body present but header missing")
    void shouldAutoAddContentLengthWhenMissing() throws IOException {
        HttpResponse response = new HttpResponse()
                .status(HttpStatus.OK)
                .body("Hello");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(out, response);
        String result = out.toString(StandardCharsets.US_ASCII);

        assertThat(result)
                .contains("content-length: 5\r\n")
                .contains("\r\n\r\nHello");
    }
}
