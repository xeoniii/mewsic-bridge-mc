package mewsicbridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MewsicWebSocketServer extends WebSocketServer {
    private static final Logger LOGGER = LoggerFactory.getLogger("mewsic-bridge");

    public MewsicWebSocketServer(int port) {
        super(new InetSocketAddress("127.0.0.1", port));
        this.setDaemon(true);
        this.setConnectionLostTimeout(30);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        LOGGER.info("[MewsicBridge] Client connected: {}", conn.getRemoteSocketAddress());
        for (WebSocket existing : this.getConnections()) {
            if (existing == conn || !existing.isOpen()) continue;
            LOGGER.info("[MewsicBridge] Closing previous connection: {}", existing.getRemoteSocketAddress());
            existing.close(1000, "New client connected");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        LOGGER.info("[MewsicBridge] Client disconnected: {} (code={}, reason={})", conn.getRemoteSocketAddress(), code, reason);
        MusicState.getInstance().setTrack("", "", 0);
        MusicState.getInstance().setPlaying(false);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        LOGGER.debug("[MewsicBridge] Received: {}", message);
        try {
            this.handleMessage(message);
        } catch (Exception e) {
            LOGGER.warn("[MewsicBridge] Failed to parse message: {} — {}", message, e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.error("[MewsicBridge] WebSocket error: {}", ex.getMessage());
    }

    @Override
    public void onStart() {
        LOGGER.info("[MewsicBridge] WebSocket server started on ws://127.0.0.1:{}", this.getPort());
    }

    public void broadcastAction(String action) {
        String payload = "{\"action\":\"" + action + "\"}";
        this.broadcastPayload(payload, action);
    }

    public void broadcastActionWithPayload(String action, String key, String value) {
        String payload = "{\"action\":\"" + action + "\",\"" + key + "\":\"" + value + "\"}";
        this.broadcastPayload(payload, action);
    }

    private void broadcastPayload(String payload, String actionName) {
        Collection<WebSocket> connections = this.getConnections();
        if (connections.isEmpty()) {
            LOGGER.warn("[MewsicBridge] No clients connected — action '{}' was not sent.", actionName);
            return;
        }
        for (WebSocket ws : connections) {
            if (!ws.isOpen()) continue;
            ws.send(payload);
        }
        LOGGER.info("[MewsicBridge] Broadcasted action: {}", payload);
    }

    private void handleMessage(String jsonString) {
        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            if (!json.has("event")) {
                return;
            }
            String event = json.get("event").getAsString();
            MusicState state = MusicState.getInstance();
            switch (event) {
                case "track_changed": {
                    if (json.has("data") && !json.get("data").isJsonNull()) {
                        JsonObject data = json.getAsJsonObject("data");
                        String title = data.has("title") ? data.get("title").getAsString() : "";
                        String artist = data.has("artist") ? data.get("artist").getAsString() : "";
                        int duration = data.has("duration") ? data.get("duration").getAsInt() : 0;
                        state.setTrack(title, artist, duration);
                        state.setCurrentTime(0.0f);
                        LOGGER.info("[MewsicBridge] Track changed -> {} — {} ({}s)", title, artist, duration);
                        break;
                    }
                    state.setTrack("", "", 0);
                    state.setCurrentTime(0.0f);
                    LOGGER.info("[MewsicBridge] Track cleared");
                    break;
                }
                case "playback_state_changed": {
                    if (json.has("data") && !json.get("data").isJsonNull()) {
                        boolean isPlaying = json.get("data").getAsBoolean();
                        state.setPlaying(isPlaying);
                        LOGGER.info("[MewsicBridge] Playback state -> {}", isPlaying ? "playing" : "paused");
                    }
                    break;
                }
                case "playlist_changed": {
                    if (json.has("data") && !json.get("data").isJsonNull()) {
                        String playlist = json.get("data").getAsString();
                        state.setCurrentPlaylist(playlist);
                        LOGGER.info("[MewsicBridge] Playlist changed -> {}", playlist);
                    }
                    break;
                }
                case "shuffle_changed": {
                    if (json.has("data") && !json.get("data").isJsonNull()) {
                        boolean shuffle = json.get("data").getAsBoolean();
                        state.setShuffle(shuffle);
                        LOGGER.info("[MewsicBridge] Shuffle -> {}", shuffle);
                    }
                    break;
                }
                case "repeat_changed": {
                    if (json.has("data") && !json.get("data").isJsonNull()) {
                        String repeat = json.get("data").getAsString();
                        state.setRepeatMode(repeat);
                        LOGGER.info("[MewsicBridge] Repeat -> {}", repeat);
                    }
                    break;
                }
                case "library_sync": {
                    if (json.has("data") && json.get("data").isJsonArray()) {
                        JsonArray data = json.getAsJsonArray("data");
                        HashMap<String, String> tracks = new HashMap<>();
                        for (JsonElement el : data) {
                            if (!el.isJsonObject()) continue;
                            JsonObject obj = el.getAsJsonObject();
                            if (!obj.has("id") || !obj.has("title")) continue;
                            tracks.put(obj.get("title").getAsString(), obj.get("id").getAsString());
                        }
                        state.updateLibrary(tracks);
                        LOGGER.info("[MewsicBridge] Library synced ({} tracks)", tracks.size());
                    }
                    break;
                }
                case "playlists_sync": {
                    if (json.has("data") && json.get("data").isJsonArray()) {
                        JsonArray data = json.getAsJsonArray("data");
                        HashMap<String, String> playlists = new HashMap<>();
                        for (JsonElement el : data) {
                            if (!el.isJsonObject()) continue;
                            JsonObject obj = el.getAsJsonObject();
                            if (!obj.has("id") || !obj.has("name")) continue;
                            playlists.put(obj.get("name").getAsString(), obj.get("id").getAsString());
                        }
                        state.updatePlaylists(playlists);
                        LOGGER.info("[MewsicBridge] Playlists synced ({} playlists)", playlists.size());
                    }
                    break;
                }
                case "time_changed": {
                    if (json.has("data") && !json.get("data").isJsonNull()) {
                        float time = json.get("data").getAsFloat();
                        state.setCurrentTime(time);
                    }
                    break;
                }
                default: {
                    LOGGER.debug("[MewsicBridge] Unknown event: {}", event);
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[MewsicBridge] Error parsing JSON: {}", e.getMessage());
        }
    }
}
