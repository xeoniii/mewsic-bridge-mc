package mewsicbridge;

import java.util.HashMap;
import java.util.Map;

public class MusicState {
    private static MusicState instance;
    private String title = "";
    private String artist = "";
    private boolean playing = false;
    private float currentTime = 0.0f;
    private int duration = 0;
    private boolean shuffle = false;
    private String repeatMode = "off";
    private String currentPlaylist = "library";
    private final Map<String, String> libraryTracks = new HashMap<>();
    private final Map<String, String> playlists = new HashMap<>();

    private MusicState() {
    }

    public static synchronized MusicState getInstance() {
        if (instance == null) {
            instance = new MusicState();
        }
        return instance;
    }

    public synchronized String getTitle() {
        return this.title;
    }

    public synchronized String getArtist() {
        return this.artist;
    }

    public synchronized boolean isPlaying() {
        return this.playing;
    }

    public synchronized float getCurrentTime() {
        return this.currentTime;
    }

    public synchronized int getDuration() {
        return this.duration;
    }

    public synchronized boolean isShuffle() {
        return this.shuffle;
    }

    public synchronized String getRepeatMode() {
        return this.repeatMode;
    }

    public synchronized String getCurrentPlaylist() {
        return this.currentPlaylist;
    }

    public synchronized Map<String, String> getLibraryTracks() {
        return this.libraryTracks;
    }

    public synchronized Map<String, String> getPlaylists() {
        return this.playlists;
    }

    public synchronized void setTrack(String title, String artist, int duration) {
        this.title = title != null ? title : "";
        this.artist = artist != null ? artist : "";
        this.duration = duration;
    }

    public synchronized void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public synchronized void setCurrentTime(float time) {
        this.currentTime = time;
    }

    public synchronized void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public synchronized void setRepeatMode(String repeatMode) {
        this.repeatMode = repeatMode != null ? repeatMode : "off";
    }

    public synchronized void setCurrentPlaylist(String playlist) {
        this.currentPlaylist = playlist != null ? playlist : "library";
    }

    public synchronized void updateLibrary(Map<String, String> tracks) {
        this.libraryTracks.clear();
        if (tracks != null) {
            this.libraryTracks.putAll(tracks);
        }
    }

    public synchronized void updatePlaylists(Map<String, String> playlists) {
        this.playlists.clear();
        if (playlists != null) {
            this.playlists.putAll(playlists);
        }
    }
}
