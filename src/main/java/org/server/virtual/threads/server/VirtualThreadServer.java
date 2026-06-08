package org.server.virtual.threads.server;

import org.server.virtual.threads.CirrusServer;
import org.server.virtual.threads.core.handler.RouteHandler;
import org.server.virtual.threads.core.model.HttpResponse;
import org.server.virtual.threads.core.model.HttpStatus;
import org.server.virtual.threads.core.router.Router;
import org.server.virtual.threads.server.config.ServerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


public class VirtualThreadServer implements CirrusServer {
    private static final Logger LOG = Logger.getLogger(VirtualThreadServer.class.getName());

    private final ServerConfig config;
    private final Router router;
    private final HttpParser parser;
    private final HttpResponseWriter writer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private int actualPort;

    public VirtualThreadServer(ServerConfig config, Router router) {
        this.config = config;
        this.router = router;
        this.parser = new HttpParser(config);
        this.writer = new HttpResponseWriter();
    }

    @Override
    public CirrusServer get(String path, RouteHandler handler) {
        router.get(path, handler);
        return this;
    }

    @Override
    public CirrusServer post(String path, RouteHandler handler) {
        router.post(path, handler);
        return this;
    }

    @Override
    public CirrusServer put(String path, RouteHandler handler) {
        router.put(path, handler);
        return this;
    }

    @Override
    public CirrusServer delete(String path, RouteHandler handler) {
        router.delete(path, handler);
        return this;
    }

    @Override
    public void start() throws IOException {
        serverSocket = new ServerSocket(config.getPort(), config.getBacklog());
        if (config.getSoTimeout() > 0) {
            serverSocket.setSoTimeout(config.getSoTimeout());
        }
        this.actualPort = serverSocket.getLocalPort();
        running.set(true);
        LOG.info("cirrus HTTP/1.0 server started on port " + actualPort);

        while (running.get()) {
            try {
                var clientSocket = serverSocket.accept();
                clientSocket.setTcpNoDelay(config.isTcpNoDelay());
                Thread.startVirtualThread(() -> handleClient(clientSocket));
            } catch (SocketTimeoutException e) {
                LOG.log(Level.WARNING, "Error Socket Timeout Exception connection: {0}", e.getMessage());
            } catch (IOException e) {
                if (running.get()) {
                    LOG.log(Level.WARNING, "Error accepting connection: {0}", e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket) {
            var request = parser.parse(clientSocket.getInputStream());
            var response = new HttpResponse();
            router.handle(request, response);
            writer.write(clientSocket.getOutputStream(), response);
        } catch (IOException e) {
            sendError(clientSocket, HttpStatus.BAD_REQUEST, String.format("%S %S", HttpStatus.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST.getReason()));
        } catch (Exception e) {
            sendError(clientSocket, HttpStatus.INTERNAL_ERROR, String.format("%S %S", HttpStatus.INTERNAL_ERROR.getCode(), HttpStatus.INTERNAL_ERROR.getReason()));
        }
    }

    private void sendError(Socket socket, HttpStatus status, String message) {
        try {
            var errorResponse = new HttpResponse()
                    .status(status)
                    .getText(message);
            writer.write(socket.getOutputStream(), errorResponse);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Send Error Exception: {0}", e.getMessage());
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Error closing server socket: {0}", e.getMessage());
            }
        }
    }

    @Override
    public int getPort() {
        return actualPort;
    }
}