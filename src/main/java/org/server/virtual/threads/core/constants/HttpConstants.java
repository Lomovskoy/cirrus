package org.server.virtual.threads.core.constants;

public final class HttpConstants {

    private HttpConstants() {} // prohibit the creation of instances

    // ========== Methods ==========
    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_DELETE = "DELETE";
    public static final String METHOD_HEAD = "HEAD";
    public static final String METHOD_OPTIONS = "OPTIONS";
    public static final String METHOD_PATCH = "PATCH";
    public static final String METHOD_TRACE = "TRACE";

    // ========== Protocol Versions ==========
    public static final String HTTP_1_0 = "HTTP/1.0";
    public static final String HTTP_1_1 = "HTTP/1.1";
    public static final String HTTP_2_0 = "HTTP/2.0";

    // ========== Headers ==========
    public static final String HEADER_CONTENT_LENGTH = "content-length";
    public static final String HEADER_CONTENT_TYPE = "content-type";
    public static final String HEADER_HOST = "host";
    public static final String HEADER_USER_AGENT = "user-agent";
    public static final String HEADER_ACCEPT = "accept";
    public static final String HEADER_CONNECTION = "connection";
    public static final String HEADER_TRANSFER_ENCODING = "transfer-encoding";

    // ========== Header Values ==========
    public static final String CONNECTION_KEEP_ALIVE = "keep-alive";
    public static final String CONNECTION_CLOSE = "close";
    public static final String TRANSFER_ENCODING_CHUNKED = "chunked";

    // ========== Content-Type ==========
    public static final String CONTENT_TYPE_TEXT = "text/plain";
    public static final String CONTENT_TYPE_HTML = "text/html";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_XML = "application/xml";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";

    // ========== Encodings ==========
    public static final String CHARSET_UTF_8 = "charset=utf-8";
    public static final String US_ASCII = "US-ASCII";

    // ========== Symbols ==========
    public static final String HTTP_VERSION_PREFIX = "HTTP/";
    public static final char CR = '\r';
    public static final char LF = '\n';
    public static final String SPACE = " ";
    public static final char COLON = ':';
    public static final char QUESTION_MARK = '?';
    public static final char SLASH = '/';
    public static final char DOT = '.';
    public static final char EQUALS = '=';
    public static final char AMPERSAND = '&';

    // ========== Strings ==========
    public static final String EMPTY = "";
    public static final String SPACE_STR = " ";
    public static final String COLON_SPACE = ": ";
    public static final String CRLF = "\r\n";

    // ========== Error messages ==========
    public static final String ERROR_EMPTY_REQUEST = "Empty request";
    public static final String ERROR_HEADER_TOO_LARGE = "Header too large: %d bytes";
    public static final String ERROR_INVALID_REQUEST_LINE = "Invalid request line: %s";
    public static final String ERROR_UNKNOWN_METHOD = "Unknown HTTP method: %s";
    public static final String ERROR_INVALID_HTTP_VERSION = "Invalid HTTP version: %s";
    public static final String ERROR_INVALID_CONTENT_LENGTH = "Invalid Content-Length: %s";
    public static final String ERROR_BODY_TOO_LARGE = "Body too large: %d bytes (max %d bytes)";

    private static final int CR_CHAR = CR;
    private static final int LF_CHAR = LF;
}
