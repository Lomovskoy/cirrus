package org.server.virtual.threads.server;

import org.server.virtual.threads.core.model.HttpMethod;
import org.server.virtual.threads.core.model.HttpRequest;
import org.server.virtual.threads.server.config.ServerConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.server.virtual.threads.core.constants.HttpConstants.*;

/**
 * HTTP/1.0 and HTTP/1.1 request parser.
 * Supports: request line, headers, body (Content-Length)
 */
public class HttpParser {

    // Indexes in the parsed query string
    private static final int REQUEST_LINE_EXPECTED_PARTS = 2;
    private static final int REQUEST_LINE_METHOD_INDEX = 0;
    private static final int REQUEST_LINE_PATH_INDEX = 1;
    private static final int REQUEST_LINE_VERSION_INDEX = 2;

    private final int maxHeaderSize;
    private final int maxBodySize;

    public HttpParser(ServerConfig config) {
        this.maxHeaderSize = config.getMaxHeaderSize();
        this.maxBodySize = config.getMaxBodySize();
    }

    public HttpParser() {
        this(ServerConfig.fromSystemProperties());
    }

    /**
     * Main parsing method.
     * @param input: socket stream
     * @return the completed HttpRequest
     */
    public HttpRequest parse(InputStream input) throws IOException {

        var reader = new BufferedReader(new InputStreamReader(input, US_ASCII), maxHeaderSize);
        var requestLine = readLineSafe(reader);

        if (requestLine.isEmpty()) {
            throw new IOException(ERROR_EMPTY_REQUEST);
        }

        var parts = parseRequestLine(requestLine);
        var headers = parseHeaders(reader);
        var body = parseBody(reader, headers);

        return new HttpRequest(parts.method, parts.path, Map.of(), headers, body);
    }

    /**
     * Reads a single line from the stream, limiting the size to prevent an attacker
     * from sending an infinite header.
     * @return a string without the trailing \r and \n characters
     */
    private String readLineSafe(BufferedReader reader) throws IOException {
        var line = new StringBuilder();
        int charsRead = 0;

        int nextChar;
        while ((nextChar = reader.read()) != -1) {

            if (charsRead > maxHeaderSize) {
                throw new IOException(String.format(ERROR_HEADER_TOO_LARGE, maxHeaderSize));
            }

            char ch = (char) nextChar;
            if (ch == LF) break; // Line feed (\n) - end of current line

            // Carriage return (\r) - ignore (skip)
            if (ch != CR) {
                line.append(ch);
                charsRead++;
            }
        }
        return line.toString();
    }

    private boolean isBlank(String str) {
        return str.isEmpty();
    }

    /**
     * Parses the request line, for example:
     * "GET /index.html HTTP/1.1" → method=GET, path=/index.html
     */
    private RequestLineParts parseRequestLine(String line) throws IOException {
        var parts = line.split(SPACE);

        // There must be at least two parts: method and path
        if (parts.length < REQUEST_LINE_EXPECTED_PARTS) {
            throw new IOException(String.format(ERROR_INVALID_REQUEST_LINE, line));
        }

        var methodStr = parts[REQUEST_LINE_METHOD_INDEX];
        var fullPath = parts[REQUEST_LINE_PATH_INDEX];

        // Convert the method string to an enum
        var method = HttpMethod.fromString(methodStr);
        if (method == null) {
            throw new IOException(String.format(ERROR_UNKNOWN_METHOD, methodStr));
        }

        // Cut off query parameters (?a=b&c=d) - we don't support it yet, just remove it
        var path = extractPathWithoutQuery(fullPath);

        // If the string has a third part, it's the HTTP version
        // (e.g., "HTTP/1.1"). Check that it starts with "HTTP/".
        if (parts.length > REQUEST_LINE_VERSION_INDEX) {
            var version = parts[REQUEST_LINE_VERSION_INDEX];
            if (!version.startsWith(HTTP_VERSION_PREFIX) || !version.equals(HTTP_1_0)) {
                throw new IOException(String.format(ERROR_INVALID_HTTP_VERSION, version));
            }
        }

        return new RequestLineParts(method, path);
    }

    /**
     * Removes the query string from the path, leaving only the bare path.
     * For example: "/search?q=java" → "/search"
     */
    private String extractPathWithoutQuery(String fullPath) {
        int queryIndex = fullPath.indexOf(QUESTION_MARK);
        return (queryIndex != -1) ? fullPath.substring(0, queryIndex) : fullPath;
    }

    /**
     * Reads all headers up to a blank line.
     * Each header is in the form "Name: Value".
     * The name is converted to lowercase for easier searching.
     */
    private Map<String, String> parseHeaders(BufferedReader reader) throws IOException {
        var headers = new HashMap<String, String>();

        String line;
        while (!isBlank(line = readLineSafe(reader))) {
            int colonIndex = line.indexOf(COLON);
            if (colonIndex > 0) {
                var name = line.substring(0, colonIndex).trim().toLowerCase(Locale.ROOT);
                var value = line.substring(colonIndex + 1).trim();
                headers.put(name, value);
            }
        }
        return headers;
    }

    /**
     * Reads the request body if the Content-Length header is passed.
     * Returns an empty string if there is nobody.
     */
    private String parseBody(BufferedReader reader, Map<String, String> headers) throws IOException {
        var contentLengthStr = headers.get(HEADER_CONTENT_LENGTH);
        if (contentLengthStr == null) {
            return EMPTY;   // no title → nobody
        }

        int contentLength;
        try {
            contentLength = Integer.parseInt(contentLengthStr);
        } catch (NumberFormatException e) {
            throw new IOException(String.format(ERROR_INVALID_CONTENT_LENGTH, contentLengthStr));
        }

        if (contentLength <= 0) {
            return EMPTY;
        }

        if (contentLength > maxBodySize) {
            throw new IOException(String.format(ERROR_BODY_TOO_LARGE, contentLength, maxBodySize));
        }

        return readBody(reader, contentLength);
    }

    /**
     * Reads exactly length characters from reader and returns them as a string.
     */
    private String readBody(BufferedReader reader, int length) throws IOException {
        var bodyChars = new char[length];
        int totalRead = 0;
        while (totalRead < length) {
            int read = reader.read(bodyChars, totalRead, length - totalRead);
            if (read == -1) {
                throw new IOException(String.format(ERROR_PREMATURE_EOF, length, totalRead));
            }
            totalRead += read;
        }
        return new String(bodyChars, 0, totalRead);
    }

    private record RequestLineParts(HttpMethod method, String path) {}
}
