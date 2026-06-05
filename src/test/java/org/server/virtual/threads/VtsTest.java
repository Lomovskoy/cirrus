package org.server.virtual.threads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.server.virtual.threads.server.config.ServerConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("Vts facade tests")
class VtsTest {

    @Test
    @DisplayName("Cover default constructor of Vts")
    void coverDefaultConstructor() {
         var vts = new Vts();
        assertThat(vts).isNotNull();
    }

    @Test
    @DisplayName("createServer(port) should return a working server")
    void createServerWithPort() throws Exception {
        int port = findFreePort();
        var server = Vts.createServer(port);
        server.get("/test", (req, res) -> res.getText("OK"));
        startServerInThread(server);

        var response = sendRequest(port, "GET /test HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("OK");

        server.stop();
    }

    @Test
    @DisplayName("createServer(ServerConfig) should respect configuration")
    void createServerWithConfig() throws Exception {
        int port = findFreePort();
        var config = ServerConfig.builder()
                .port(port)
                .maxHeaderSize(1024) // we don't check, but the configuration is applied
                .build();
        var server = Vts.createServer(config);
        server.get("/config", (req, res) -> res.getText("configured"));
        startServerInThread(server);

        var response = sendRequest(port, "GET /config HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("configured");

        server.stop();
    }

    // ------------------------------------------------------------
    // Auxiliary methods
    // ------------------------------------------------------------
    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void startServerInThread(VtsServer server) {
        var t = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
        // waiting for launch
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String sendRequest(int port, String rawRequest) throws IOException {
        try (var socket = new Socket("localhost", port)) {
            socket.setSoTimeout(5000);
            var out = socket.getOutputStream();
            out.write(rawRequest.getBytes(StandardCharsets.US_ASCII));
            out.flush();

            // Give the server time to process the error and send a response
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            var in = socket.getInputStream();
            var response = new ByteArrayOutputStream();
            var buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                response.write(buffer, 0, read);
            }
            return response.toString(StandardCharsets.US_ASCII);
        }
    }
}
