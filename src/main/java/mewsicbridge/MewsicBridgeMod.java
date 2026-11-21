package mewsicbridge;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MewsicBridgeMod implements ModInitializer {
    public static final String MOD_ID = "mewsic-bridge";
    public static final Logger LOGGER = LoggerFactory.getLogger("mewsic-bridge");
    public static final int WS_PORT = 3012;
    public static MewsicWebSocketServer wsServer;

    @Override
    public void onInitialize() {
        LOGGER.info("[MewsicBridge] Initializing Mewsic \u2194 Minecraft bridge...");
        wsServer = new MewsicWebSocketServer(3012);
        wsServer.start();
        LOGGER.info("[MewsicBridge] WebSocket server started on ws://127.0.0.1:{}", 3012);
    }

    public static void restartServer() {
        new Thread(() -> {
            if (wsServer != null) {
                try {
                    LOGGER.info("[MewsicBridge] Stopping WebSocket server...");
                    wsServer.stop(2000);
                } catch (InterruptedException e) {
                    LOGGER.error("[MewsicBridge] Interrupted while stopping server: {}", e.getMessage());
                } catch (Exception e) {
                    LOGGER.error("[MewsicBridge] Error stopping WebSocket server: {}", e.getMessage());
                }
            }
            try {
                Thread.sleep(500L);
                LOGGER.info("[MewsicBridge] Starting new WebSocket server on port {}...", 3012);
                wsServer = new MewsicWebSocketServer(3012);
                wsServer.setReuseAddr(true);
                wsServer.start();
            } catch (Exception e) {
                LOGGER.error("[MewsicBridge] Error starting WebSocket server: {}", e.getMessage());
            }
        }, "Mewsic-Restart-Thread").start();
    }
}
