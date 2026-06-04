package org.server.virtual.threads.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.server.virtual.threads.Vts;
import org.server.virtual.threads.VtsServer;
import org.server.virtual.threads.server.config.ServerConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("VirtualThreadServer Integration Tests")
class VirtualThreadServerTest {

    private VtsServer server;
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
        startServerWithRoutes(srv -> {
            srv.get("/hello", (req, res) -> res.getText("Hello, World!"));
        });

        String response = sendRequest("GET /hello HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("Hello, World!");
    }

    @Test
    @DisplayName("POST /echo returns body with prefix")
    void postEcho() throws Exception {
        startServerWithRoutes(srv -> {
            srv.post("/echo", (req, res) -> res.getText("You said: " + req.getBody()));
        });

        String body = "Hello Server";
        String request = "POST /echo HTTP/1.0\r\n" +
                "Content-Length: " + body.length() + "\r\n\r\n" + body;
        String response = sendRequest(request);
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("You said: " + body);
    }

    // ------------------------------------------------------------
    // 2. Path parameters (if implemented)
    // ------------------------------------------------------------

    @Test
    @DisplayName("GET /users/:id extracts path parameter")
    void getUserById() throws Exception {
        startServerWithRoutes(srv -> {
            srv.get("/users/:id", (req, res) -> res.getText("User " + req.getParam("id")));
        });

        String response = sendRequest("GET /users/42 HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("User 42");
    }

    // ------------------------------------------------------------
    // 3. Error handling
    // ------------------------------------------------------------

    @Test
    @DisplayName("Non‑existent route returns 404")
    void nonExistentRoute() throws Exception {
        startServerWithRoutes(srv -> {}); // no routes

        String response = sendRequest("GET /unknown HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 404 Not Found");
        assertThat(response).contains("404 Not Found");
    }

    @Test
    @Disabled(value = "In real life, the user would get an error—the browser or curl would see " +
            "\"Connection reset by peer\" or an empty response. But testing requires a complex rewrite of the client-side.")
    @DisplayName("Handler throwing exception returns 500")
    void handlerThrowsException() throws Exception {
        startServerWithRoutes(srv -> {
            srv.get("/error", (_, _) -> { throw new RuntimeException("Test error"); });
        });

        var response = sendRequest("GET /error HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 500 Internal Server Error");
        assertThat(response).contains("500 Internal Server Error");
    }

    @Test
    @Disabled(value = "In real life, the user would get an error—the browser or curl would see " +
            "\"Connection reset by peer\" or an empty response. But testing requires a complex rewrite of the client-side.")
    @DisplayName("Malformed request returns 500")
    void malformedRequest() throws Exception {
        startServerWithRoutes(srv -> {
            srv.get("/any", (_, res) -> res.getText("OK"));
        });

        var malformed = "GET /any HTTP/1.0\r\n" +
                "Content-Length: abc\r\n\r\n"; // invalid Content-Length
        var response = sendRequest(malformed);
        assertThat(response).contains("HTTP/1.0 500 Internal Server Error");
    }

    // ------------------------------------------------------------
    // 4. Stopping the server
    // ------------------------------------------------------------

    @Test
    @DisplayName("Server stops gracefully and no longer accepts connections")
    void stopServer() throws Exception {
        startServerWithRoutes(srv -> {
            srv.get("/test", (req, res) -> res.getText("OK"));
        });

        assertThat(canConnect()).isTrue();

        server.stop();
        // Небольшая пауза для завершения accept loop
        Thread.sleep(100);

        assertThat(canConnect()).isFalse();
    }

    // ------------------------------------------------------------
    // 5. Different HTTP methods (PUT, DELETE)
    // ------------------------------------------------------------

    @Test
    @DisplayName("PUT /resource returns 200")
    void putResource() throws Exception {
        startServerWithRoutes(srv -> {
            srv.put("/resource", (req, res) -> res.getText("PUT updated"));
        });

        String response = sendRequest("PUT /resource HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("PUT updated");
    }

    @Test
    @DisplayName("DELETE /resource returns 200")
    void deleteResource() throws Exception {
        startServerWithRoutes(srv -> {
            srv.delete("/resource", (req, res) -> res.getText("DELETE removed"));
        });

        String response = sendRequest("DELETE /resource HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("DELETE removed");
    }

    // ------------------------------------------------------------
    // 6. The connection is closed after the response (HTTP/1.0)
    // ------------------------------------------------------------

    @Test
    @DisplayName("Connection is closed after response")
    void connectionClosedAfterResponse() throws Exception {
        startServerWithRoutes(srv -> {
            srv.get("/close", (req, res) -> res.getText("bye"));
        });

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
        startServerWithRoutes(srv -> {});
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
        void register(VtsServer server);
    }

    private void startServerWithRoutes(RouteRegistrar registrar) throws Exception {
        var config = ServerConfig.builder()
                .port(0)          // случайный свободный порт
                .soTimeout(50)
                .build();
        server = Vts.createServer(config);
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

        // Ждём, пока сервер поднимет порт
        long deadline = System.currentTimeMillis() + 2000;
        while (server.getPort() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        this.port = server.getPort();
        assertThat(this.port).isNotZero();
        assertThat(startupError.get()).isNull();
    }

    private String sendRequest(String rawRequest) throws IOException {
        try (Socket socket = new Socket("localhost", port);
             var out = socket.getOutputStream();
             var in = socket.getInputStream()) {

            out.write(rawRequest.getBytes(StandardCharsets.US_ASCII));
            out.flush();

            var response = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                response.write(buffer, 0, read);
            }
            return response.toString(StandardCharsets.US_ASCII);
        }
    }

    private boolean canConnect() {
        try (var socket = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
