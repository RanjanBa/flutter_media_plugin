package com.example.fluttermediaplugin;

import android.net.Uri;
import android.os.Handler;
import androidx.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class Playlist {
    private static final String TAG = "Playlist";
    private static String PLAYLIST_NAME = "playlistName";
    private String playlistName;
    private ConcatenatingMediaSource concatenatingMediaSource;
    private ArrayList<Song> songs;
    private CacheDataSourceFactory cacheDataSourceFactory;
    private SimpleExoPlayer simpleExoPlayer;

    Playlist(String playlistName, @NonNull SimpleExoPlayer simpleExoPlayer, @NonNull MediaSourceEventListener playlistEventListener, @NonNull DefaultDataSourceFactory dataSourceFactory) {
        this.playlistName = playlistName;
        this.simpleExoPlayer = simpleExoPlayer;
        songs = new ArrayList<>();

        cacheDataSourceFactory = new CacheDataSourceFactory(DownloadUtility.getDownloadCache(FlutterMediaPlugin.getInstance().getRegistrar().activeContext()), dataSourceFactory);
        concatenatingMediaSource = new ConcatenatingMediaSource();
        concatenatingMediaSource.addEventListener(new Handler(), playlistEventListener);
    }

    Song getSongAtIndex(int index) {
        if (getSize() <= 0 && index >= getSize())
            return null;
        return songs.get(index);
    }

    int getSize() {
        return songs.size() != concatenatingMediaSource.getSize() ? -1 : concatenatingMediaSource.getSize();
    }

    void skipToIndex(int index) {
        if (index >= concatenatingMediaSource.getSize()) {
            Log.w(TAG, "can't skip to index " + index);
            return;
        }
        simpleExoPlayer.seekTo(index, 0);
    }

    void skipToPrevious() {
        if (simpleExoPlayer.getPreviousWindowIndex() >= 0) {
            simpleExoPlayer.seekTo(simpleExoPlayer.getPreviousWindowIndex(), 0);
        }
    }

    void skipToNext() {
        if (simpleExoPlayer.getNextWindowIndex() >= 0) {
            simpleExoPlayer.seekTo(simpleExoPlayer.getNextWindowIndex(), 0);
        }
    }

    void prepare() {
        if (simpleExoPlayer.getPlaybackState() == Player.STATE_IDLE || simpleExoPlayer.getPlaybackState() == Player.STATE_ENDED) {
            simpleExoPlayer.prepare(concatenatingMediaSource);
        }
    }

    void addAndPlay(Song song) {
        prepare();
        //Log.d(TAG, "Song Added");
        addSong(song, new Runnable() {
            @Override
            public void run() {
                if (concatenatingMediaSource.getSize() > 0) {
                    simpleExoPlayer.seekTo(concatenatingMediaSource.getSize() - 1, C.TIME_UNSET);
                }
            }
        });
    }

    void addSong(Song song) {
        songs.add(song);
        Uri uri = Uri.parse(song.getUri());
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(uri);
        concatenatingMediaSource.addMediaSource(mediaSource);
    }

    private void addSong(Song song, @NonNull Runnable actionOnCompletion) {
        songs.add(song);
        Uri uri = Uri.parse(song.getUri());
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(uri);
        concatenatingMediaSource.addMediaSource(mediaSource, new Handler(), actionOnCompletion);
    }

    void addSong(int index, Song song) {
        if (index > songs.size() && index >= concatenatingMediaSource.getSize()) {
            Log.w(TAG, index + " is greater than size of songs : " + songs.size());
            return;
        }
        songs.add(index, song);
        Uri uri = Uri.parse(song.getUri());
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(uri);
        concatenatingMediaSource.addMediaSource(index, mediaSource);
    }

//    public void addSong(int index, Song song, @NonNull Runnable actionOnCompletion) {
//        if (index >= songs.size() && index >= concatenatingMediaSource.getSize()) {
//            Log.w(TAG, index + " is greater than size of songs : " + songs.size());
//            return;
//        }
//        songs.add(index, song);
//        Uri uri = Uri.parse(song.getUri());
//        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
//        concatenatingMediaSource.addMediaSource(index, mediaSource, new Handler(), actionOnCompletion);
//    }

    void addSongs(List<Song> songs) {
        for (int i = 0; i < songs.size(); i++) {
            this.songs.add(songs.get(i));
            Uri uri = Uri.parse(songs.get(i).getUri());
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(uri);
            concatenatingMediaSource.addMediaSource(mediaSource);
        }
    }

    void removeSong(Song song) {
        for (int i = 0; i < songs.size(); i++) {
            if (song.getKey().equals(songs.get(i).getKey())) {
                return;
            }
        }
    }

    void clear() {
        songs.clear();
        concatenatingMediaSource.clear();
    }

    static JSONObject toJson(Playlist playlist) {
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

//    public static Playlist fromJson(JSONObject jsonObject, @NonNull SimpleExoPlayer simpleExoPlayer, @NonNull PlaylistEventListener playlistEventListener, @NonNull DefaultDataSourceFactory dataSourceFactory, int playIndex) {
//        try {
//            String playlist_name = jsonObject.get(PLAYLIST_NAME).toString();
//            Playlist playlist = new Playlist(playlist_name, simpleExoPlayer, playlistEventListener, dataSourceFactory);
//            JSONArray jsonArray = jsonObject.getJSONArray("songs");
//
//            List<Song> songs = new ArrayList<>();
//            for (int i = 0; i < jsonArray.length(); i++) {
//                JSONObject json = jsonArray.getJSONObject(i);
//                Song song = Song.fromJson(json);
//                songs.add(song);
//            }
//            playlist.addSongs(songs);
//            return playlist;
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }

    static List<Song> songsFromPlaylistJson(JSONObject jsonObject) {
        try {
            JSONArray jsonArray = jsonObject.getJSONArray("songs");

            List<Song> songs = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                Song song = Song.fromJson(json);
                songs.add(song);
            }

            return songs;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
}
