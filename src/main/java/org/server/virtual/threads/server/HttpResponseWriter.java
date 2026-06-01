package org.server.virtual.threads.server;

import org.server.virtual.threads.core.model.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.server.virtual.threads.core.constants.HttpConstants.*;

/**
 * Generates an HTTP response from an HttpResponse object and writes it to the OutputStream.
 * Operates over the HTTP/1.0 protocol.
 */
public class HttpResponseWriter {

    private static final Charset US_ASCII = StandardCharsets.US_ASCII;

    /**
     * Converts the HttpResponse to a byte array and writes it to the output stream.
     * First, collects the entire response in memory, then sends it.
     *
     * @param out is the output stream (e.g., a socket's OutputStream)
     * @param response is the response object
     */
    public void write(OutputStream out, HttpResponse response) throws IOException {
        byte[] fullResponse = buildResponseBytes(response);
        out.write(fullResponse);
        out.flush();
    }

    /**
     * Assembles the full response as a byte array.
     * Response structure:
     * 1. Status line (HTTP/1.0 200 OK\r\n)
     * 2. Headers (one or more, each ending with \r\n)
     * 3. Empty line (\r\n)
     * 4. Body (optional)
     *
     * @param response response
     * @return byte array ready to send
     */
    private byte[] buildResponseBytes(HttpResponse response) {
        var buffer = new ByteArrayOutputStream();

        var statusLine = HTTP_1_0 + SPACE + response.getStatus().getCode() + SPACE + response.getStatus().getReason() + CRLF;
        buffer.writeBytes(statusLine.getBytes(US_ASCII));

        // Copy the original headers (keys are in the original case, but case is usually not important)
        // Important: headers must be written BEFORE the body.
        var headers = response.getHeaders();

        // If the Content-Length header is missing, but the body is present, we add it automatically.
        var hasContentLength = headers.containsKey(HEADER_CONTENT_LENGTH.toLowerCase());
        var body = response.getBody();
        var hasBody = body != null && body.length > 0;

        // Write all the headers that are already in the response
        for (var entry : headers.entrySet()) {
            buffer.writeBytes((entry.getKey() + COLON_SPACE + entry.getValue() + CRLF).getBytes(US_ASCII));
        }

        // Add Content-Length if the body is present but the header is not set
        if (hasBody && !hasContentLength) {
            buffer.writeBytes((HEADER_CONTENT_LENGTH + COLON_SPACE + body.length + CRLF).getBytes(US_ASCII));
        }

        // An empty line separating the headings and body
        buffer.writeBytes(CRLF.getBytes(US_ASCII));
        if (hasBody) buffer.writeBytes(body);

        return buffer.toByteArray();
    }
}
