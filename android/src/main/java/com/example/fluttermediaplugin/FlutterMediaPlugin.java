package com.example.fluttermediaplugin;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.TextureRegistry;

/**
 * FlutterMediaPlugin
 */
public class FlutterMediaPlugin implements MethodCallHandler {
    private static final String TAG = "FlutterMediaPlugin";
    private static Pattern METHOD_NAME_MATCH = Pattern.compile("([^/]+)/([^/]+)");

    private static String VIDEO_MEDIA_TYPE = "VIDEO_TYPE";
    private static String AUDIO_MEDIA_TYPE = "AUDIO_TYPE";

    private static FlutterMediaPlugin instance;

    private Registrar registrar;
    private AudioPlayer audioPlayer;
    private VideoPlayer videoPlayer;

    private boolean isPlayingAudio = true;
    private MethodChannel channel;

    private ExoPlayerListener audioExoPlayerListener;
    private ExoPlayerListener videoExoPlayerListener;

    public static FlutterMediaPlugin getInstance() {
        if (instance == null) {
            Log.e(TAG, "Flutter Media plugin instance is null");
        }
        return instance;
    }

    public Registrar getRegistrar() {
        return registrar;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    private FlutterMediaPlugin(Registrar _registrar) {
        this.registrar = _registrar;
        MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_media_plugin");

        channel.setMethodCallHandler(this);
        this.channel = channel;
    }

    private void initializeAudioPlayer() {
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(registrar.context()).build();
        ImageLoader.getInstance().init(config);

        audioPlayer = new AudioPlayer(registrar.activeContext());

        audioExoPlayerListener = GetExoPlayerListener(true);
        audioPlayer.addExoPlayerListener(audioExoPlayerListener);
    }

    private void sendAudioInitialization() {
        if (audioPlayer == null || audioPlayer.getSimpleExoPlayer() == null) {
            Map<String, Object> args = new HashMap<>();
            args.put("message", "Simple ExoPlayer is null");
            String method;
            method = AUDIO_MEDIA_TYPE;
            method += "/onPlayerStatus";
            channel.invokeMethod(method, args);
            return;
        }

        Log.d(TAG, "Json onPlaylistChanged");
        JSONObject jsonObject = Playlist.toJson(instance.audioPlayer.getPlaylist());
        if (jsonObject != null) {
            String json = jsonObject.toString();
            Map<String, Object> args = new HashMap<>();
            args.put("playlist", json);
            String method;
            method = AUDIO_MEDIA_TYPE;
            method += "/onPlaylistChanged";
            channel.invokeMethod(method, args);
        } else {
            Log.d(TAG, "Json object playlist is null");
        }

        {
            int playbackState = audioPlayer.getSimpleExoPlayer().getPlaybackState();
            boolean playWhenReady = audioPlayer.getSimpleExoPlayer().getPlayWhenReady();
            Log.d(TAG, "onPlayerStateChanged : " + playbackState);
            Map<String, Object> args = new HashMap<>();
            args.put("playWhenReady", playWhenReady);
            args.put("playbackState", playbackState);
            String method;
            method = AUDIO_MEDIA_TYPE;
            method += "/onPlayerStateChanged";
            channel.invokeMethod(method, args);
        }
        {
            Log.d(TAG, "onMediaPeriodCreated");
            Map<String, Object> args = new HashMap<>();
            args.put("windowIndex", audioPlayer.getSimpleExoPlayer().getCurrentWindowIndex());
            String method;
            method = AUDIO_MEDIA_TYPE;
            method += "/onMediaPeriodCreated";
            channel.invokeMethod(method, args);
        }
    }

    private void initializeVideoPlayer() {
        videoPlayer = new VideoPlayer(registrar.activeContext());

        videoExoPlayerListener = GetExoPlayerListener(false);
        videoPlayer.addExoPlayerListener(videoExoPlayerListener);
    }

    private ExoPlayerListener GetExoPlayerListener(final boolean isAudio) {
        return new ExoPlayerListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
                Log.d(TAG, "onTimelineChanged");
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                Log.d(TAG + "TRACK", "onTracksChanged " + trackGroups.length + ", " + trackSelections.length);
            }

            @Override
            public void onLoadingChanged(boolean isLoading) {
                Log.d(TAG, "onLoadingChanged");
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Map<String, Object> args = new HashMap<>();
                args.put("playWhenReady", playWhenReady);
                args.put("playbackState", playbackState);
                String method;
                if (isAudio) {
                    method = AUDIO_MEDIA_TYPE;
                } else {
                    method = VIDEO_MEDIA_TYPE;
                }
                method += "/onPlayerStateChanged";
                Log.d(TAG, "onPlayerStateChanged : " + playbackState + ", " + method);
                channel.invokeMethod(method, args);
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {
                Log.d(TAG, "onRepeatModeChanged");
            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
                Log.d(TAG, "onShuffleModeEnabledChanged");
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.d(TAG, "onPlayerError");
            }

            @Override
            public void onPositionDiscontinuity(int reason) {
                Log.d(TAG, "onPositionDiscontinuity");
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                Log.d(TAG, "onPlaybackParametersChanged");
            }

            @Override
            public void onSeekProcessed() {
                Log.d(TAG, "onSeekProcessed");
            }

            @Override
            public void onMediaPeriodCreated(int windowIndex) {
                Log.d(TAG, "onMediaPeriodCreated");
                Map<String, Object> args = new HashMap<>();
                args.put("windowIndex", windowIndex);

                String method;
                if (isAudio) {
                    method = AUDIO_MEDIA_TYPE;
                } else {
                    method = VIDEO_MEDIA_TYPE;
                }
                method += "/onMediaPeriodCreated";
                channel.invokeMethod(method, args);
            }

            @Override
            public void onPlaybackUpdate(long position, long audioLength) {
                //Log.d(TAG, "onPlaybackUpdate");
                Map<String, Object> args = new HashMap<>();
                args.put("position", position);
                args.put("audioLength", audioLength);
                String method;
                if (isAudio) {
                    method = AUDIO_MEDIA_TYPE;
                } else {
                    method = VIDEO_MEDIA_TYPE;
                }
                method += "/onPlaybackUpdate";
                channel.invokeMethod(method, args);
            }

            @Override
            public void onPlaylistChanged(Playlist playlist) {
                Log.d(TAG, "Json onPlaylistChanged");
                JSONObject jsonObject = Playlist.toJson(playlist);
                if (jsonObject != null) {
                    String json = jsonObject.toString();
                    Map<String, Object> args = new HashMap<>();
                    args.put("playlist", json);
                    String method;
                    if (isAudio) {
                        method = AUDIO_MEDIA_TYPE;
                    } else {
                        method = VIDEO_MEDIA_TYPE;
                    }
                    method += "/onPlaylistChanged";
                    channel.invokeMethod(method, args);
                } else {
                    Log.d(TAG, "Json object playlist is null");
                }
            }

            @Override
            public void onBufferedUpdate(int percent) {
                //Log.d(TAG, "onBufferedUpdate " + percent);
                Map<String, Object> args = new HashMap<>();
                args.put("percent", percent);
                String method;
                if (isAudio) {
                    method = AUDIO_MEDIA_TYPE;
                } else {
                    method = VIDEO_MEDIA_TYPE;
                }
                method += "/onBufferedUpdate";
                channel.invokeMethod(method, args);
            }

            @Override
            public void onPlayerStatus(String message) {
                Map<String, Object> args = new HashMap<>();
                args.put("message", message);
                String method;
                if (isAudio) {
                    method = AUDIO_MEDIA_TYPE;
                } else {
                    method = VIDEO_MEDIA_TYPE;
                }
                method += "/onPlayerStatus";
                channel.invokeMethod(method, args);
            }
        };
    }

    public void videoInitialize(long textureId, int height, int width, long duration) {
        Map<String, Object> reply = new HashMap<>();
        reply.put("textureId", textureId);
        reply.put("width", width);
        reply.put("height", height);
        reply.put("duration", duration);
        channel.invokeMethod(VIDEO_MEDIA_TYPE + "/videoInitialize", reply);
    }

    /**
     * Plugin registration.
     */

    public static void registerWith(@NonNull Registrar _registrar) {
        if (instance == null) instance = new FlutterMediaPlugin(_registrar);
        else {
            instance.registrar = _registrar;
            MethodChannel channel = new MethodChannel(_registrar.messenger(), "flutter_media_plugin");
            channel.setMethodCallHandler(instance);
            instance.channel = channel;

            if (instance.audioPlayer != null) {
                instance.audioPlayer.removeExoPlayerListener(instance.audioExoPlayerListener);

                instance.audioExoPlayerListener = instance.GetExoPlayerListener(true);
                instance.audioPlayer.addExoPlayerListener(instance.audioExoPlayerListener);
            }

            if (instance.videoPlayer != null) {
                instance.videoPlayer.removeExoPlayerListener(instance.videoExoPlayerListener);
                instance.videoExoPlayerListener = instance.GetExoPlayerListener(false);
                instance.videoPlayer.addExoPlayerListener(instance.videoExoPlayerListener);
            }
        }
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        try {
            MediaMethodCall mediaMethodCall = parseMethodName(call.method);
            Log.d(TAG, mediaMethodCall.toString());
            if (mediaMethodCall.mediaType.equals(AUDIO_MEDIA_TYPE)) {
                if (mediaMethodCall.command.equals("initialize")) {
                    initializeAudioPlayer();
                    return;
                }

                if (audioPlayer == null) {
                    Log.d(TAG, "AudioPlayer is null");
                    return;
                }
                switch (mediaMethodCall.command) {
                    case "play":
                        Log.d(TAG, "play");
                        audioPlayer.play();
                        videoPlayer.pause();
                        break;
                    case "pause":
                        Log.d(TAG, "pause");
                        audioPlayer.pause();
                        break;
                    case "seekTo":
                        //noinspection ConstantConditions
                        int position = call.argument("position");
                        audioPlayer.seekTo(position);
                        break;
                    case "addAndPlay": {
                        String key = call.argument(Song.song_key_tag);
                        String title = call.argument(Song.song_title_tag);
                        String artist = call.argument(Song.song_artist_tag);
                        String album = call.argument(Song.song_album_tag);
                        String album_art_uri = call.argument(Song.song_album_art_uri_tag);
                        String uri = call.argument(Song.song_uri_tag);
                        Song song = new Song(key, title, artist, album, album_art_uri, uri);
                        audioPlayer.addAndPlay(song);
                        audioPlayer.play();
                        break;
                    }
                    case "addSong": {
                        String key = call.argument(Song.song_key_tag);
                        String title = call.argument(Song.song_title_tag);
                        String artist = call.argument(Song.song_artist_tag);
                        String album = call.argument(Song.song_album_tag);
                        String album_art_uri = call.argument(Song.song_album_art_uri_tag);
                        String uri = call.argument(Song.song_uri_tag);
                        Song song = new Song(key, title, artist, album, album_art_uri, uri);
                        audioPlayer.addSong(song);
                        break;
                    }
                    case "addSongAtIndex": {
                        //noinspection ConstantConditions
                        int index = call.argument("index");
                        String key = call.argument(Song.song_key_tag);
                        String title = call.argument(Song.song_title_tag);
                        String artist = call.argument(Song.song_artist_tag);
                        String album = call.argument(Song.song_album_tag);
                        String album_art_uri = call.argument(Song.song_album_art_uri_tag);
                        String uri = call.argument(Song.song_uri_tag);
                        Song song = new Song(key, title, artist, album, album_art_uri, uri);
                        audioPlayer.addSongAtIndex(index, song);
                        break;
                    }
                    case "removeSong": {
                        String key = call.argument(Song.song_key_tag);
                        String title = call.argument(Song.song_title_tag);
                        String artist = call.argument(Song.song_artist_tag);
                        String album = call.argument(Song.song_album_tag);
                        String album_art_uri = call.argument(Song.song_album_art_uri_tag);
                        String uri = call.argument(Song.song_uri_tag);
                        Song song = new Song(key, title, artist, album, album_art_uri, uri);
                        audioPlayer.removeSong(song);
                        break;
                    }
                    case "setPlaylistAndSongIndex": {
                        String playlistStr = call.argument("playlist");
                        //noinspection ConstantConditions
                        int index = call.argument("playIndex");
                        audioPlayer.setPlaylist(playlistStr, index);
                        audioPlayer.preparePlaylist();
                        break;
                    }
                    case "clearPlaylist":
                        audioPlayer.clearPlaylist();
                        break;
                    case "setRepeatMode":
                        //noinspection ConstantConditions
                        int repeatMode = call.argument("repeatMode");
                        audioPlayer.setRepeatMode(repeatMode);
                        break;
                    case "skipToNext":
                        audioPlayer.skipToNext();
                        break;
                    case "skipToPrevious":
                        audioPlayer.skipToPrevious();
                        break;
                    case "skipToIndex": {
                        //noinspection ConstantConditions
                        int index = call.argument("index");
                        audioPlayer.skipToIndex(index);
                        break;
                    }
                    case "release":
                        audioPlayer.release();
                        break;
                }
            } else if (mediaMethodCall.mediaType.equals(VIDEO_MEDIA_TYPE)) {
                if (mediaMethodCall.command.equals("initialize")) {
                    initializeVideoPlayer();
                    return;
                }
                if (videoPlayer == null) {
                    Log.d(TAG, "VideoPlayer is null");
                    return;
                }

                switch (mediaMethodCall.command) {
                    case "addAndPlay": {
                        TextureRegistry textures = registrar.textures();
                        if (textures == null) {
                            result.error("no_activity", "video_player plugin requires a foreground activity", null);
                            return;
                        }

                        TextureRegistry.SurfaceTextureEntry handle = textures.createSurfaceTexture();
                        if (handle == null) {
                            return;
                        }

                        String uri = call.argument("uri");
                        String asset = call.argument("asset");
                        if (uri == null && asset != null) {
                            String assetLookupKey = registrar.lookupKeyForAsset(asset);
                            Log.d(TAG, "asset : " + assetLookupKey);
                            videoPlayer.addAndPlay("assets:///" + assetLookupKey, handle);
                        } else if (uri != null && asset == null) {
                            videoPlayer.addAndPlay(uri, handle);
                        }
                        break;
                    }
                    case "initSetTexture": {
                        TextureRegistry textures = registrar.textures();
                        if (textures == null) {
                            result.error("no_activity", "video_player plugin requires a foreground activity", null);
                            return;
                        }

                        TextureRegistry.SurfaceTextureEntry handle = textures.createSurfaceTexture();
                        if (handle != null) {
                            videoPlayer.setupVideoPlayer(handle);
                        }
                        break;
                    }
                    case "play":
                        videoPlayer.play();
                        audioPlayer.pause();
                        break;
                    case "pause":
                        videoPlayer.pause();
                        break;
                    default:
                        result.notImplemented();
                        break;
                }
            } else {
                result.error("method call media type ", "type of " + mediaMethodCall.mediaType + " is not equal.", null);
            }
        } catch (IllegalArgumentException e) {
            result.notImplemented();
        }
    }

    private MediaMethodCall parseMethodName(@NonNull String methodName) {
        Matcher matcher = METHOD_NAME_MATCH.matcher(methodName);

        if (matcher.matches()) {
            String mediaType = matcher.group(1);
            String command = matcher.group(2);
            return new MediaMethodCall(mediaType, command);
        } else {
            Log.d(TAG, "Match not found");
            throw new IllegalArgumentException("Invalid audio player message: " + methodName);
        }
    }

    private static class MediaMethodCall {
        final String mediaType;
        final String command;

        private MediaMethodCall(@NonNull String mediaType, @NonNull String command) {
            this.mediaType = mediaType;
            this.command = command;
        }

        @Override
        public String toString() {
            return String.format("MediaMethodCall - mediaType %s, Command: %s", mediaType, command);
        }
    }
}