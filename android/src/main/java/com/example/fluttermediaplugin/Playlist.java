package com.example.fluttermediaplugin;

import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class Playlist {
    private static final String TAG = "Playlist";
    private static String PLAYLIST_NAME = "playlistName";

    private String playlistName;
    private ConcatenatingMediaSource concatenatingMediaSource;
    private ArrayList<Song> songs;
    private DefaultDataSourceFactory dataSourceFactory;
    private PlaylistEventListener playlistEventListener;

    public Playlist(String playlistName, @NonNull PlaylistEventListener playlistEventListener, @NonNull DefaultDataSourceFactory dataSourceFactory) {
        this.playlistName = playlistName;
        songs = new ArrayList<>();
        this.playlistEventListener = playlistEventListener;
        concatenatingMediaSource = new ConcatenatingMediaSource();
        concatenatingMediaSource.addEventListener(new Handler(), playlistEventListener);
        this.dataSourceFactory = dataSourceFactory;
    }

    public Song getSongAtIndex(int index) {
        if (getSize() <= 0 && index >= getSize())
            return null;

        return songs.get(index);
    }

    public int getSize() {
        return songs.size() != concatenatingMediaSource.getSize() ? -1 : concatenatingMediaSource.getSize();
    }

    public void skipToIndex(int index) {
        if (index >= concatenatingMediaSource.getSize()) {
            Log.w(TAG, "can't skip to index " + index);
            return;
        }
        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().seekTo(index, 0);
    }

    public void skipToPrevious() {
        if (FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPreviousWindowIndex() >= 0) {
            FlutterMediaPlugin.getInstance().getSimpleExoPlayer().seekTo(FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPreviousWindowIndex(), 0);
        }
    }

    public void skipToNext() {
        if (FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getNextWindowIndex() >= 0) {
            FlutterMediaPlugin.getInstance().getSimpleExoPlayer().seekTo(FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getNextWindowIndex(), 0);
        }
    }

    public void prepare() {
        if (FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() == Player.STATE_IDLE || FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() == Player.STATE_ENDED) {
            FlutterMediaPlugin.getInstance().getSimpleExoPlayer().prepare(concatenatingMediaSource);
        }
    }

    public void addAndPlay(Song song) {
        prepare();
        //Log.d(TAG, "Song Added");
        addSong(song, new Runnable() {
            @Override
            public void run() {
                FlutterMediaPlugin.getInstance().getSimpleExoPlayer().seekTo(concatenatingMediaSource.getSize() - 1, C.TIME_UNSET);
            }
        });
    }

    public void addSong(Song song) {
        songs.add(song);
        Uri uri = Uri.parse(song.getUri());
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
        concatenatingMediaSource.addMediaSource(mediaSource);

        playlistEventListener.onPlaylistChanged(this);
    }

    public void addSong(Song song, @NonNull Runnable actionOnCompletion) {
        songs.add(song);
        Uri uri = Uri.parse(song.getUri());
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
        concatenatingMediaSource.addMediaSource(mediaSource, new Handler(), actionOnCompletion);
        playlistEventListener.onPlaylistChanged(this);
    }

    public void addSong(int index, Song song) {
        if (index >= songs.size() && index >= concatenatingMediaSource.getSize()) {
            Log.w(TAG, index + " is greater than size of songs : " + songs.size());
            return;
        }
        songs.add(index, song);
        Uri uri = Uri.parse(song.getUri());
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
        concatenatingMediaSource.addMediaSource(index, mediaSource);
        playlistEventListener.onPlaylistChanged(this);
    }

    public void addSong(int index, Song song, @NonNull Runnable actionOnCompletion) {
        if (index >= songs.size() && index >= concatenatingMediaSource.getSize()) {
            Log.w(TAG, index + " is greater than size of songs : " + songs.size());
            return;
        }
        songs.add(index, song);
        Uri uri = Uri.parse(song.getUri());
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
        concatenatingMediaSource.addMediaSource(index, mediaSource, new Handler(), actionOnCompletion);
        playlistEventListener.onPlaylistChanged(this);
    }

    public void addSongs(List<Song> songs, final int playIndex) {
        for (int i = 0; i < songs.size(); i++) {
            this.songs.add(songs.get(i));
            Uri uri = Uri.parse(songs.get(i).getUri());
            MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            if (i == songs.size() - 1 && playIndex < songs.size() && playIndex >= 0) {
                concatenatingMediaSource.addMediaSource(mediaSource, new Handler(), new Runnable() {
                    @Override
                    public void run() {
                        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().seekTo(playIndex, C.TIME_UNSET);
                    }
                });
            } else {
                concatenatingMediaSource.addMediaSource(mediaSource);
            }
        }

        playlistEventListener.onPlaylistChanged(this);
    }

    public int removeSong(Song song) {
        for (int i = 0; i < songs.size(); i++) {
            if (song.getKey() == songs.get(i).getKey()) {
                return i;
            }
        }

        playlistEventListener.onPlaylistChanged(this);
        return -1;
    }

    public void clear() {
        songs.clear();
        concatenatingMediaSource.clear();
        playlistEventListener.onPlaylistChanged(this);
    }

    public static JSONObject toJson(Playlist playlist) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(PLAYLIST_NAME, playlist.playlistName);
            JSONArray jsonArraySongs = new JSONArray();
            for (Song song : playlist.songs) {
                JSONObject json = Song.toJson(song);
                jsonArraySongs.put(json);
            }
            jsonObject.put("songs", jsonArraySongs);
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Playlist fromJson(JSONObject jsonObject, @NonNull SimpleExoPlayer simpleExoPlayer, @NonNull PlaylistEventListener playlistEventListener, @NonNull DefaultDataSourceFactory dataSourceFactory, int playIndex) {
        try {
            String playlist_name = jsonObject.get(PLAYLIST_NAME).toString();
            Playlist playlist = new Playlist(playlist_name, playlistEventListener, dataSourceFactory);
            JSONArray jsonArray = jsonObject.getJSONArray("songs");

            List<Song> songs = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                Song song = Song.fromJson(json);
                songs.add(song);
            }
            playlist.addSongs(songs, playIndex);
            return playlist;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public interface PlaylistEventListener extends MediaSourceEventListener {
        public void onPlaylistChanged(Playlist playlist);
    }
}
