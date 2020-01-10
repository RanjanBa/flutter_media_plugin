package com.example.fluttermediaplugin;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.AssetDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.view.TextureRegistry;

import static com.example.fluttermediaplugin.FlutterMediaPlugin.VIDEO_METHOD_TYPE;
import static com.google.android.exoplayer2.C.CONTENT_TYPE_MOVIE;
import static com.google.android.exoplayer2.C.USAGE_MEDIA;
import static com.google.android.exoplayer2.Player.REPEAT_MODE_ALL;
import static com.google.android.exoplayer2.Player.REPEAT_MODE_OFF;

class VideoPlayer {
    private static final String VIDEO_EXO_PLAYER_LISTENER_THREAD_NAME = "video_player_thread_name";
    private static String TAG = "VideoPlayer";

    private VideoExoPlayerListener videoExoPlayerListener;

    private SimpleExoPlayer simpleExoPlayer;
    private Context context;
    private MethodChannel channel;

    private Surface surface;
    private TextureRegistry.SurfaceTextureEntry textureEntry;

    VideoPlayer(@NonNull Context context, TextureRegistry textures, @NonNull MethodChannel channel) {
        this.context = context;
        this.channel = channel;

        initializeSimpleExoPlayer(context);

        if(textures != null) {
            TextureRegistry.SurfaceTextureEntry handle = textures.createSurfaceTexture();
            if (handle != null) {
                textureEntry = handle;
                surface = new Surface(textureEntry.surfaceTexture());
                simpleExoPlayer.setVideoSurface(surface);
            }
        }
    }

    private void initializeSimpleExoPlayer(Context context) {
        if (simpleExoPlayer != null) {
            simpleExoPlayer.stop();
            simpleExoPlayer.release();
        }

        TrackSelector trackSelector = new DefaultTrackSelector();
        simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(USAGE_MEDIA)
                .setContentType(CONTENT_TYPE_MOVIE)
                .build();

        simpleExoPlayer.setAudioAttributes(audioAttributes, true);

        if (videoExoPlayerListener == null) {
            videoExoPlayerListener = new VideoExoPlayerListener();
        }
        simpleExoPlayer.addListener(videoExoPlayerListener);

        simpleExoPlayer.addVideoListener(new VideoListener() {
            @Override
            public void onSurfaceSizeChanged(int width, int height) {
                videoExoPlayerListener.onSurfaceSizeChanged(width, height);
            }
        });
    }

    private static boolean isFileOrAsset(Uri uri) {
        if (uri == null || uri.getScheme() == null) {
            return false;
        }
        String scheme = uri.getScheme();
        return scheme.equals("file") || scheme.equals("assets");
    }

    private MediaSource buildMediaSource(
            Uri uri, DataSource.Factory mediaDataSourceFactory, Context context) {
        int type = Util.inferContentType(uri.getLastPathSegment());
        Log.d(TAG, "type " + type);
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource.Factory(
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                        new DefaultDataSourceFactory(context, null, mediaDataSourceFactory))
                        .createMediaSource(uri);
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                        new DefaultDataSourceFactory(context, null, mediaDataSourceFactory))
                        .createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(mediaDataSourceFactory, new DefaultExtractorsFactory())
                        .createMediaSource(uri);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    void initialize(@NonNull MethodChannel.Result result) {
        Map<String, Object> reply = new HashMap<>();
        reply.put("textureId", textureEntry.id());
        result.success(reply);
    }

    void setChannel(MethodChannel channel) {
        this.channel = channel;
    }

    void addAndPlay(String url, TextureRegistry textures) {
        Uri uri = Uri.parse(url);
        Log.d(TAG, "Uri : " + uri);

        if(textures != null) {
            TextureRegistry.SurfaceTextureEntry handle = textures.createSurfaceTexture();
            if (handle != null && handle != textureEntry) {
                textureEntry = handle;
                surface = new Surface(textureEntry.surfaceTexture());
                simpleExoPlayer.setVideoSurface(surface);
                videoExoPlayerListener.onTextureIdChanged(textureEntry.id());
            }
        }

        DataSource.Factory dataSourceFactory;
        if (isFileOrAsset(uri)) {
            Log.d(TAG, "file is in file or asset");
            dataSourceFactory = new DataSource.Factory() {
                @Override
                public DataSource createDataSource() {
                    return new AssetDataSource(FlutterMediaPlugin.getInstance().getRegistrar().context());
                }
            };
        } else {
            Log.d(TAG, "file is in network");
            dataSourceFactory = new DefaultDataSourceFactory(context, "videoExoPlayer");
        }
        MediaSource mediaSource = buildMediaSource(uri, dataSourceFactory, context);
        simpleExoPlayer.prepare(mediaSource);
    }

    void play() {
        simpleExoPlayer.setPlayWhenReady(true);
    }

    void pause() {
        simpleExoPlayer.setPlayWhenReady(false);
    }

    void setLooping(boolean value) {
        simpleExoPlayer.setRepeatMode(value ? REPEAT_MODE_ALL : REPEAT_MODE_OFF);
    }

    void setVolume(double value) {
        float bracketedValue = (float) Math.max(0.0, Math.min(1.0, value));
        simpleExoPlayer.setVolume(bracketedValue);
    }

    void seekTo(int location) {
        simpleExoPlayer.seekTo(location);
    }

    long getPosition() {
        return simpleExoPlayer.getCurrentPosition();
    }

    void dispose() {
        textureEntry.release();
        if (surface != null) {
            surface.release();
        }

        if (simpleExoPlayer != null) {
            simpleExoPlayer.removeListener(videoExoPlayerListener);
            simpleExoPlayer.stop();
        }
    }

    private class VideoExoPlayerListener extends MediaExoPlayerListener {
        VideoExoPlayerListener() {
            super(simpleExoPlayer, VIDEO_EXO_PLAYER_LISTENER_THREAD_NAME);
        }

        void onTextureIdChanged(long id) {
            Map<String, Object> args = new HashMap<>();
            args.put("textureId", id);
            channel.invokeMethod(VIDEO_METHOD_TYPE + "/onTextureIdChanged", args);
        }

        void onSurfaceSizeChanged(int width, int height) {
            Map<String, Object> args = new HashMap<>();
            args.put("width", width);
            args.put("height", height);
            channel.invokeMethod(VIDEO_METHOD_TYPE + "/onSurfaceSizeChanged", args);
        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
            Log.d(TAG, "onTimelineChanged");
            super.onTimelineChanged(timeline, manifest, reason);
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            Log.d(TAG + "TRACK", "onTracksChanged " + trackGroups.length + ", " + trackSelections.length);
            super.onTracksChanged(trackGroups, trackSelections);
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            Log.d(TAG, "onLoadingChanged");
            super.onLoadingChanged(isLoading);
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            super.onPlayerStateChanged(playWhenReady, playbackState);

            Map<String, Object> args = new HashMap<>();
            args.put("playWhenReady", playWhenReady);
            args.put("playbackState", playbackState);
            String method = VIDEO_METHOD_TYPE + "/onPlayerStateChanged";
//                Log.d(TAG, "onPlayerStateChanged : " + playbackState + ", " + method);
            channel.invokeMethod(method, args);
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            super.onRepeatModeChanged(repeatMode);

            Map<String, Object> args = new HashMap<>();
            args.put("repeatMode", repeatMode);
            String method = VIDEO_METHOD_TYPE + "/onRepeatModeChanged";
//                Log.d(TAG, "onRepeatModeChanged : " + repeatMode + ", " + method);
            channel.invokeMethod(method, args);
        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            super.onShuffleModeEnabledChanged(shuffleModeEnabled);

            Map<String, Object> args = new HashMap<>();
            args.put("shuffleModeEnabled", shuffleModeEnabled);
            String method = VIDEO_METHOD_TYPE + "/onShuffleModeEnabledChanged";
//                Log.d(TAG, "onShuffleModeEnabledChanged : " + shuffleModeEnabled + ", " + method);
            channel.invokeMethod(method, args);
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            Log.d(TAG, "onPositionDiscontinuity");
            super.onPositionDiscontinuity(reason);
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            Log.d(TAG, "onPlaybackParametersChanged");
            super.onPlaybackParametersChanged(playbackParameters);
        }

        @Override
        public void onSeekProcessed() {
            Log.d(TAG, "onSeekProcessed");
            super.onSeekProcessed();
        }

        @Override
        public void onMediaPeriodCreated(int windowIndex) {
            Log.d(TAG, "onMediaPeriodCreated");

            super.onMediaPeriodCreated(windowIndex);

//            Map<String, Object> args = new HashMap<>();
//            args.put("windowIndex", windowIndex);
//            Song song = audioPlayer.getSongByIndex(windowIndex);
//            if (song == null)
//                return;
//
//            Map<String, Object> songMap = Song.toMap(song);
//            args.put("currentPlayingSong", songMap);
//            String method = VIDEO_METHOD_TYPE + "/onMediaPeriodCreated";
//            channel.invokeMethod(method, args);
        }

        @Override
        public void onPlaybackUpdate(long position, long audioLength) {
            super.onPlaybackUpdate(position, audioLength);

            //Log.d(TAG, "onPlaybackUpdate");
            Map<String, Object> args = new HashMap<>();
            args.put("position", position);
            args.put("audioLength", audioLength);
            String method = VIDEO_METHOD_TYPE + "/onPlaybackUpdate";
            channel.invokeMethod(method, args);
        }

        @Override
        public void onBufferedUpdate(int percent) {
            //Log.d(TAG, "onBufferedUpdate " + percent);
            super.onBufferedUpdate(percent);

            Map<String, Object> args = new HashMap<>();
            args.put("percent", percent);
            String method = VIDEO_METHOD_TYPE + "/onBufferedUpdate";
            channel.invokeMethod(method, args);
        }

        @Override
        public void onPlayerStatus(String message) {
            super.onPlayerStatus(message);

            Map<String, Object> args = new HashMap<>();
            args.put("message", message);
            String method = VIDEO_METHOD_TYPE + "/onPlayerStatus";
            channel.invokeMethod(method, args);
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.d(TAG, "onPlayerError");
            super.onPlayerError(error);
        }
    }
}
