package org.server.virtual.threads.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.server.virtual.threads.Vts;
import org.server.virtual.threads.VtsServer;
import org.server.virtual.threads.server.config.ServerConfig;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("VirtualThreadServer SocketTimeout handling")
class VirtualThreadServerTimeoutTest {

    private VtsServer server;

    @Test
    @DisplayName("Server should survive SocketTimeoutException and continue")
    void serverHandlesSocketTimeout() throws Exception {
        var config = ServerConfig.builder()
                .port(0)
                .soTimeout(50)
                .build();
        server = Vts.createServer(config);
        server.get("/ping", (req, res) -> res.getText("pong"));

        var serverFailed = new AtomicBoolean(false);
        var serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                serverFailed.set(true);
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Give the server time to start and survive a couple of timeouts
        Thread.sleep(100);

        assertThat(serverFailed.get()).isFalse();

        int port = server.getPort();
        assertThat(port).isNotZero();

        try (var socket = new Socket("localhost", port)) {
            socket.getOutputStream().write("GET /ping HTTP/1.0\r\n\r\n".getBytes());
            socket.getOutputStream().flush();
            var buffer = new byte[1024];
            int read = socket.getInputStream().read(buffer);
            assertThat(read).isGreaterThan(0);
            var response = new String(buffer, 0, read);
            assertThat(response).contains("pong");
        }

        server.stop();
    }
}
