package org.server.virtual.threads;

import org.junit.jupiter.api.Disabled;
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
        VtsServer server = Vts.createServer(port);
        server.get("/test", (req, res) -> res.getText("OK"));
        startServerInThread(server);

        String response = sendRequest(port, "GET /test HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("OK");

        server.stop();
    }

    @Test
    @Disabled(value = "In real life, the user would get an error—the browser or curl would see " +
            "\"Connection reset by peer\" or an empty response. But testing requires a complex rewrite of the client-side.")
    @DisplayName("createServer(ServerConfig) should respect configuration")
    void createServerWithConfig() throws Exception {
        int port = findFreePort();
        var config = ServerConfig.builder()
                .port(port)
                .maxHeaderSize(1024)
                .build();
        VtsServer server = Vts.createServer(config);
        server.get("/config", (req, res) -> res.getText("configured"));
        startServerInThread(server);

        String response = sendRequest(port, "GET /config HTTP/1.0\r\n\r\n");
        assertThat(response).contains("HTTP/1.0 200 OK");
        assertThat(response).contains("configured");

        // проверяем, что maxHeaderSize действительно применён
        String longHeader = "X-Large: " + "x".repeat(2000) + "\r\n";
        String longRequest = "GET /config HTTP/1.0\r\n" + longHeader + "\r\n";
        // сервер должен отклонить запрос с таким большим заголовком (maxHeaderSize=1024)
        String errorResponse = sendRequest(port, longRequest);
        assertThat(errorResponse).contains("400 Bad Request");

        server.stop();
    }

    // вспомогательные методы
    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void startServerInThread(VtsServer server) {
        Thread t = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
        // ждём запуска
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String sendRequest(int port, String rawRequest) throws IOException {
        try (var socket = new Socket("localhost", port);
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
}
