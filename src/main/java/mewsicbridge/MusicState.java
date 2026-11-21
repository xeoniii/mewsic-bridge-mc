package mewsicbridge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MusicState {
    private static final MusicState INSTANCE = new MusicState();

    private volatile String title = "";
    private volatile String artist = "";
    private volatile boolean playing = false;
    private volatile float currentTime = 0.0f;
    private volatile int duration = 0;
    private volatile boolean shuffle = false;
    private volatile String repeatMode = "off";
    private volatile String currentPlaylist = "library";
    private final Map<String, String> libraryTracks = new ConcurrentHashMap<>();
    private final Map<String, String> playlists = new ConcurrentHashMap<>();

    private volatile int metadataVersion = 0;

    private MusicState() {
    }

    public static MusicState getInstance() {
        return INSTANCE;
    }

    public int getMetadataVersion() {
        return this.metadataVersion;
    }

    public String getTitle() {
        return this.title;
    }

    public String getArtist() {
        return this.artist;
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public float getCurrentTime() {
        return this.currentTime;
    }

    public int getDuration() {
        return this.duration;
    }

    public boolean isShuffle() {
        return this.shuffle;
    }

    public String getRepeatMode() {
        return this.repeatMode;
    }

    public String getCurrentPlaylist() {
        return this.currentPlaylist;
    }

    public Map<String, String> getLibraryTracks() {
        return this.libraryTracks;
    }

    public Map<String, String> getPlaylists() {
        return this.playlists;
    }

    public void setTrack(String title, String artist, int duration) {
        this.title = title != null ? title : "";
        this.artist = artist != null ? artist : "";
        this.duration = duration;
        this.metadataVersion++;
    }

    public void setPlaying(boolean playing) {
        if (this.playing != playing) {
            this.playing = playing;
            this.metadataVersion++;
        }
    }

    public void setCurrentTime(float time) {
        this.currentTime = time;
    }

    public void setShuffle(boolean shuffle) {
        if (this.shuffle != shuffle) {
            this.shuffle = shuffle;
            this.metadataVersion++;
        }
    }

    public void setRepeatMode(String repeatMode) {
        this.repeatMode = repeatMode != null ? repeatMode : "off";
        this.metadataVersion++;
    }

    public void setCurrentPlaylist(String playlist) {
        this.currentPlaylist = playlist != null ? playlist : "library";
        this.metadataVersion++;
    }

    public void updateLibrary(Map<String, String> tracks) {
        this.libraryTracks.clear();
        if (tracks != null) {
            this.libraryTracks.putAll(tracks);
        }
        this.metadataVersion++;
    }

    public void updatePlaylists(Map<String, String> playlists) {
        this.playlists.clear();
        if (playlists != null) {
            this.playlists.putAll(playlists);
        }
        this.metadataVersion++;
    }
}
