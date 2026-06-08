package org.server.virtual.threads;

import org.server.virtual.threads.core.router.TrieRouter;
import org.server.virtual.threads.server.VirtualThreadServer;
import org.server.virtual.threads.server.config.ServerConfig;

public class Cirrus {
    public static CirrusServer createServer(int port) {
        var config = ServerConfig.builder().port(port).build();
        return new VirtualThreadServer(config, new TrieRouter());
    }

    public static CirrusServer createServer(ServerConfig config) {
        return new VirtualThreadServer(config, new TrieRouter());
    }
}
