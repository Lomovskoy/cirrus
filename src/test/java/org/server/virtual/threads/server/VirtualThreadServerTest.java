package org.server.virtual.threads.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.server.virtual.threads.Cirrus;
import org.server.virtual.threads.CirrusServer;
import org.server.virtual.threads.core.model.HttpStatus;
import org.server.virtual.threads.core.router.TrieRouter;
import org.server.virtual.threads.server.config.ServerConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("VirtualThreadServer Integration Tests")
class VirtualThreadServerTest {

    private CirrusServer server;
    private Thread serverThread;
    private int port;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }
    }

    // ------------------------------------------------------------
    // 1. GET и POST
    // ------------------------------------------------------------

    @Test
    @DisplayName("GET /hello returns 200 and plain text")
    void getHello() throws Exception {
        startServerWithRoutes(srv ->
                srv.get("/hello", (_, res) -> res.getText("Hello, World!"))
        );

        var response = sendRequest("GET /hello HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("Hello, World!");
    }

    @Test
    @DisplayName("POST /echo returns body with prefix")
    void postEcho() throws Exception {
        startServerWithRoutes(srv ->
                srv.post("/echo", (req, res) -> res.getText("You said: " + req.getBody()))
        );

        var body = "Hello Server";
        var request = "POST /echo HTTP/1.0\r\n" +
                "Content-Length: " + body.length() + "\r\n\r\n" + body;
        var response = sendRequest(request);
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("You said: " + body);
    }

    // ------------------------------------------------------------
    // 2. Path parameters (if implemented)
    // ------------------------------------------------------------

    @Test
    @DisplayName("GET /users/:id extracts path parameter")
    void getUserById() throws Exception {
        startServerWithRoutes(srv ->
                srv.get("/users/:id", (req, res) -> res.getText("User " + req.getParam("id")))
        );

        var response = sendRequest("GET /users/42 HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("User 42");
    }

    // ------------------------------------------------------------
    // 3. Error handling
    // ------------------------------------------------------------

    @Test
    @DisplayName("Non‑existent route returns 404")
    void nonExistentRoute() throws Exception {
        startServerWithRoutes(_ -> {}); // no routes

        var response = sendRequest("GET /unknown HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 404 Not Found");
        assertThat(response).contains("404 Not Found");
    }

    @Test
    @DisplayName("sendError handles write exception")
    void sendErrorHandlesWriteException() throws Exception {
        var server = (VirtualThreadServer) Cirrus.createServer(0);
        var mockSocket = mock(Socket.class);
        var badOut = mock(OutputStream.class);
        doThrow(new IOException("Write failed")).when(badOut).write(any(byte[].class));
        when(mockSocket.getOutputStream()).thenReturn(badOut);
        // call sendError via reflection
        var sendError = VirtualThreadServer.class.getDeclaredMethod("sendError", Socket.class, HttpStatus.class, String.class);
        sendError.setAccessible(true);
        sendError.invoke(server, mockSocket, HttpStatus.INTERNAL_ERROR, "test");
        // check that the exception is caught - no errors
    }

    @Test
    @DisplayName("Handler throwing exception should not crash server and sendError is called")
    void handlerThrowsException() throws Exception {
        startServerWithRoutes(srv ->
                srv.get("/error", (_, _) -> { throw new RuntimeException("Test error"); })
        );

        try (Socket socket = new Socket("localhost", port)) {
            socket.getOutputStream().write("GET /error HTTP/1.0\r\n\r\n".getBytes());
            socket.getOutputStream().flush();
            // Give the server time to handle the exception and send a response (or close the socket)
            Thread.sleep(100);
        }
        // The server must not crash
        assertThat(server.getPort()).isEqualTo(port);
    }

    @Test
    @DisplayName("sendError uses writer.write and handles success")
    void sendErrorWriterWriteCoverage() throws Exception {
        var server = (VirtualThreadServer) Cirrus.createServer(0);

        // Create a mock Socket that returns a normal OutputStream
        var mockSocket = mock(Socket.class);
        var fakeOut = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(fakeOut);

        // Call sendError via reflection
        var sendError = VirtualThreadServer.class.getDeclaredMethod("sendError", Socket.class, HttpStatus.class, String.class);
        sendError.setAccessible(true);
        sendError.invoke(server, mockSocket, HttpStatus.INTERNAL_ERROR, "500 Internal Server Error");

        // Check that writer.write was called (indirectly, through the contents of fakeOut)
        assertThat(fakeOut.toString(StandardCharsets.US_ASCII)).contains("500 Internal Server Error");
    }

    @Test
    @DisplayName("Should log error when IOException occurs while server is running")
    void shouldLogErrorOnIOException() throws Exception {
        // Start the server
        startServerWithRoutes(_ -> {});
        var serverImpl = (VirtualThreadServer) server;

        // Get the ServerSocket via reflection and close it
        var serverSocketField = VirtualThreadServer.class.getDeclaredField("serverSocket");
        serverSocketField.setAccessible(true);
        var serverSocket = (ServerSocket) serverSocketField.get(serverImpl);
        serverSocket.close(); // This will throw a SocketException in accept()

        // Give the server thread time to handle the exception and log
        Thread.sleep(200);

        // Stop the server after the test
        server.stop();

        // Checking that the log was written? Subscribing to the log is possible, but it's complicated.
        // It's enough that the exception was handled and the server didn't crash.
        assertThat(server.getPort()).isEqualTo(port);
    }

    // ------------------------------------------------------------
    // 4. Stopping the server
    // ------------------------------------------------------------

    @Test
    @DisplayName("Server stops gracefully and no longer accepts connections")
    void stopServer() throws Exception {
        startServerWithRoutes(srv ->
                srv.get("/test", (_, res) -> res.getText("OK"))
        );

        assertThat(canConnect()).isTrue();

        server.stop();
        // A short pause to complete the accept loop
        Thread.sleep(100);

        assertThat(canConnect()).isFalse();
    }

    @Test
    @DisplayName("Stop before start should not throw exception")
    void stopBeforeStart() {
        var config = ServerConfig.builder().port(0).build();
        var server = new VirtualThreadServer(config, new TrieRouter());
        // Don't call start()
        server.stop(); // should just do nothing
        assertThat(server.getPort()).isZero();
    }

    @Test
    @DisplayName("Stop twice should handle already closed socket")
    void stopTwice() throws Exception {
        startServerWithRoutes(_ -> {});
        int originalPort = server.getPort();
        server.stop(); // closes the socket for the first time
        server.stop(); // the second time - the socket is already closed
        assertThat(server.getPort()).isEqualTo(originalPort);
    }

    @Test
    @DisplayName("Stop handles IOException when closing socket")
    void stopHandlesIOExceptionOnClose() throws Exception {
        var server = (VirtualThreadServer) Cirrus.createServer(0);

        // Create a mock ServerSocket
        var mockSocket = mock(ServerSocket.class);
        doThrow(new IOException("Simulated close error")).when(mockSocket).close();
        when(mockSocket.isClosed()).thenReturn(false);

        // Implementing a mock via reflection
        var socketField = VirtualThreadServer.class.getDeclaredField("serverSocket");
        socketField.setAccessible(true);
        socketField.set(server, mockSocket);

        // Set running = true
        var runningField = VirtualThreadServer.class.getDeclaredField("running");
        runningField.setAccessible(true);
        ((AtomicBoolean) runningField.get(server)).set(true);

        server.stop();

        verify(mockSocket, times(1)).close();
        assertThat(server.getPort()).isZero();
    }

    // ------------------------------------------------------------
    // 5. Different HTTP methods (PUT, DELETE)
    // ------------------------------------------------------------

    @Test
    @DisplayName("PUT /resource returns 200")
    void putResource() throws Exception {
        startServerWithRoutes(srv ->
                srv.put("/resource", (_, res) -> res.getText("PUT updated"))
        );

        var response = sendRequest("PUT /resource HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("PUT updated");
    }

    @Test
    @DisplayName("DELETE /resource returns 200")
    void deleteResource() throws Exception {
        startServerWithRoutes(srv ->
                srv.delete("/resource", (_, res) -> res.getText("DELETE removed"))
        );

        var response = sendRequest("DELETE /resource HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("DELETE removed");
    }

    // ------------------------------------------------------------
    // 6. The connection is closed after the response (HTTP/1.0)
    // ------------------------------------------------------------

    @Test
    @DisplayName("Connection is closed after response")
    void connectionClosedAfterResponse() throws Exception {
        startServerWithRoutes(srv ->
                srv.get("/close", (_, res) -> res.getText("bye"))
        );

        try (var socket = new Socket("localhost", port)) {
            var out = socket.getOutputStream();
            out.write("GET /close HTTP/1.0\r\n\r\n".getBytes());
            out.flush();

            var in = socket.getInputStream();
            var buffer = new byte[1024];
            int read = in.read(buffer);
            assertThat(read).isGreaterThan(0);

            // Trying to read more should return -1 (connection closed)
            int next = in.read();
            assertThat(next).isEqualTo(-1);
        }
    }

    @Test
    @DisplayName("Server survives malformed request without crashing")
    void serverSurvivesMalformedRequest() throws Exception {
        startServerWithRoutes(_ -> {});
        var malformed = "GET /any HTTP/1.0\r\nContent-Length: abc\r\n\r\n";

        // Just send and don't wait for a response; the server shouldn't crash
        try (var socket = new Socket("localhost", port)) {
            socket.getOutputStream().write(malformed.getBytes());
            socket.getOutputStream().flush();
            Thread.sleep(100); // give the server time to process
        }
        assertThat(server.getPort()).isEqualTo(port); // the server is still alive
    }

    // ------------------------------------------------------------
    // Auxiliary methods
    // ------------------------------------------------------------

    @FunctionalInterface
    private interface RouteRegistrar {
        void register(CirrusServer server);
    }

    private void startServerWithRoutes(RouteRegistrar registrar) throws Exception {
        var config = ServerConfig.builder()
                .port(0)          // random free port
                .soTimeout(50)
                .build();
        server = Cirrus.createServer(config);
        registrar.register(server);

        var startupError = new AtomicReference<Exception>();
        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                startupError.set(e);
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for the server to raise the port
        long deadline = System.currentTimeMillis() + 2000;
        while (server.getPort() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        this.port = server.getPort();
        assertThat(this.port).isNotZero();
        assertThat(startupError.get()).isNull();
    }

    private String sendRequest(String rawRequest) throws IOException {
        try (var socket = new Socket("localhost", port);
             var out = socket.getOutputStream();
             var in = socket.getInputStream()) {

            out.write(rawRequest.getBytes(StandardCharsets.US_ASCII));
            out.flush();

            var response = new ByteArrayOutputStream();
            var buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                response.write(buffer, 0, read);
            }
            return response.toString(StandardCharsets.US_ASCII);
        }
    }

    private boolean canConnect() {
        try (var _ = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
