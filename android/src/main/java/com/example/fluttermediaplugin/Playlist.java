package com.example.fluttermediaplugin;

import android.net.Uri;
import android.os.Handler;

import androidx.annotation.NonNull;
import io.flutter.plugin.common.MethodChannel;

import android.util.Log;

import com.example.fluttermediaplugin.Media.Media;
import com.example.fluttermediaplugin.Media.Song;
import com.example.fluttermediaplugin.Media.Video;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class Playlist<T extends Media> {
    private static final String TAG = "Playlist";
    private static String MEDIA_PLAYLIST = "mediaPlaylist";
    private static String PLAYLIST_NAME = "playlistName";

    private String playlistName;
    private ArrayList<T> mediaList;

    private SimpleExoPlayer simpleExoPlayer;
    private ConcatenatingMediaSource concatenatingMediaSource;
    private CacheDataSourceFactory cacheDataSourceFactory;

    Playlist(@NonNull String playlistName, @NonNull SimpleExoPlayer simpleExoPlayer, @NonNull MediaSourceEventListener playlistEventListener, @NonNull DefaultDataSourceFactory dataSourceFactory) {
        this.playlistName = playlistName;
        this.simpleExoPlayer = simpleExoPlayer;
        cacheDataSourceFactory = new CacheDataSourceFactory(DownloadManager.getDownloadCache(FlutterMediaPlugin.getInstance().getRegistrar().activeContext()), dataSourceFactory);
        concatenatingMediaSource = new ConcatenatingMediaSource();
        concatenatingMediaSource.addEventListener(new Handler(), playlistEventListener);
        mediaList = new ArrayList<>();
    }

    void prepare(@NonNull ArrayList<T> mediaList, final MethodChannel.Result result) {
        for (int i = 0; i < mediaList.size(); i++) {
            if(i == mediaList.size() - 1) {
                addMedia(mediaList.get(i), new Runnable() {
                    @Override
                    public void run() {
                        result.success(true);
                    }
                });
            }
            else {
                addMedia(mediaList.get(i));
            }
        }

        simpleExoPlayer.prepare(concatenatingMediaSource);
    }

    int getSize() {
        return mediaList.size();
    }

    String getPlaylistName() {
        return playlistName;
    }

    T getMediaAtIndex(int index) {
        if (getSize() <= 0 && index >= getSize())
            return null;
        return mediaList.get(index);
    }

    void skipToIndex(int index) {
        if (index >= concatenatingMediaSource.getSize() || index < 0) {
            Log.w(TAG, "can't skip to index: " + index + ", MediaPlaylist size: " + mediaList.size());
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

    private void addMedia(@NonNull T media) {
        Uri uri = Uri.parse(media.getUrl());
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(uri);
        concatenatingMediaSource.addMediaSource(mediaSource);
        mediaList.add(media);
    }

    private void addMedia(@NonNull T media, @NonNull Runnable actionOnCompletion) {
        Uri uri = Uri.parse(media.getUrl());
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(uri);
        concatenatingMediaSource.addMediaSource(mediaSource, new Handler(), actionOnCompletion);
        mediaList.add(media);
    }

    boolean addMediaAtIndex(int index,@NonNull T media) {
        if ((index > mediaList.size() && index > concatenatingMediaSource.getSize()) || index < 0) {
            Log.e(TAG, index + " is out of bound. MediaList size: " + mediaList.size());
            return false;
        }

        Uri uri = Uri.parse(media.getUrl());
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(uri);
        concatenatingMediaSource.addMediaSource(index, mediaSource);
        mediaList.add(index, media);
        return true;
    }

    boolean addMediaAtIndex(int index, @NonNull T media, @NonNull Runnable actionOnCompletion) {
        if ((index > mediaList.size() && index > concatenatingMediaSource.getSize()) || index < 0) {
            Log.e(TAG, index + " is out of bound. MediaList size: " + mediaList.size());
            return false;
        }

        Uri uri = Uri.parse(media.getUrl());
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(uri);
        concatenatingMediaSource.addMediaSource(index, mediaSource, new Handler(), actionOnCompletion);
        mediaList.add(index, media);
        return true;
    }

    boolean removeMediaAtIndex(@NonNull T media, int index) {
        if(index >= 0 && index < mediaList.size() && index < concatenatingMediaSource.getSize()) {
            if(mediaList.get(index).getKey().equals(media.getKey())) {
                mediaList.remove(index);
                concatenatingMediaSource.removeMediaSource(index);
                return true;
            }
        }

        return false;
    }

    void clear() {
        mediaList.clear();
        concatenatingMediaSource.clear();
    }

    JSONObject toJson() {
        try {
            JSONArray jsonArrayMediaList = new JSONArray();
            for (T media : mediaList) {
                if(media instanceof Song) {
                    JSONObject jsonSong = new JSONObject(((Song) media).toMap());
                    jsonArrayMediaList.put(jsonSong);
                }
                else if(media instanceof Video) {
                    Log.d(TAG, "Under development");
                }
                else {
                    Log.d(TAG, "Under development");
                }
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put(PLAYLIST_NAME, playlistName);
            jsonObject.put(MEDIA_PLAYLIST, jsonArrayMediaList);
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    static String playlistNameFromJson(JSONObject jsonObject) {
        try {
            return jsonObject.get(PLAYLIST_NAME).toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    static ArrayList<Song> songsFromJson(JSONObject jsonObject) {
        try {
            JSONArray jsonArray = jsonObject.getJSONArray(MEDIA_PLAYLIST);

            ArrayList<Song> songs = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                Map<String, String> map = new HashMap<>();

                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = (String) json.get(key);
                    map.put(key, value);
                }

                Song song = Song.fromMap(map);
                songs.add(song);
            }

            return songs;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
}
