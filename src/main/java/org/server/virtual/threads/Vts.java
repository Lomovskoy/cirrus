package org.server.virtual.threads;

import org.server.virtual.threads.core.router.TrieRouter;
import org.server.virtual.threads.server.VirtualThreadServer;
import org.server.virtual.threads.server.config.ServerConfig;

public class Vts {
    public static VtsServer createServer(int port) {
        var config = ServerConfig.builder().port(port).build();
        return new VirtualThreadServer(config, new TrieRouter());
    }

    public static VtsServer createServer(ServerConfig config) {
        return new VirtualThreadServer(config, new TrieRouter());
    }
}
