package com.example.fluttermediaplugin;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.AssetDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.flutter.view.TextureRegistry;

import static com.google.android.exoplayer2.Player.REPEAT_MODE_ALL;
import static com.google.android.exoplayer2.Player.REPEAT_MODE_OFF;

public class VideoPlayer {
    private static String TAG = "VideoPlayer";

    private VideoExoPlayerListener videoExoPlayerListener;
    private Context context;

    private Surface surface;
    private TextureRegistry.SurfaceTextureEntry textureEntry;

    VideoPlayer(Context context) {
        this.context = context;
    }

    public void addVideoEventListener() {
        if (videoExoPlayerListener == null) {
            videoExoPlayerListener = new VideoExoPlayerListener();
        }
        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().addListener(videoExoPlayerListener);
    }

    public void removeVideoEventListener() {
        if (videoExoPlayerListener != null)
            FlutterMediaPlugin.getInstance().getSimpleExoPlayer().addListener(videoExoPlayerListener);
    }

//    public void addExoPlayerListener(ExoPlayerListener exoPlayerListener) {
//        mediaPlayerExoPlayerListenerManager.addExoPlayerListener(exoPlayerListener);
//    }
//
//    public void removeExoPlayerListener(ExoPlayerListener exoPlayerListener) {
//        mediaPlayerExoPlayerListenerManager.removeExoPlayerListener(exoPlayerListener);
//    }

    public void initialize(String stringUri) {
        Uri uri = Uri.parse(stringUri);
        Log.d(TAG, "Uri : " + uri);

        DataSource.Factory dataSourceFactory;
        if (isFileOrAsset(uri)) {
            dataSourceFactory = new DataSource.Factory() {
                @Override
                public DataSource createDataSource() {
                    return new AssetDataSource(FlutterMediaPlugin.getInstance().getRegistrar().context());
                }
            };
        } else {
            dataSourceFactory =
                    new DefaultHttpDataSourceFactory(
                            "ExoPlayer",
                            null,
                            DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                            DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                            true);
        }
        MediaSource mediaSource = buildMediaSource(uri, dataSourceFactory, context);
        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().prepare(mediaSource);
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
                return new ExtractorMediaSource.Factory(mediaDataSourceFactory)
                        .setExtractorsFactory(new DefaultExtractorsFactory())
                        .createMediaSource(uri);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    public void setupVideoPlayer(
            TextureRegistry.SurfaceTextureEntry textureEntry) {

        surface = new Surface(textureEntry.surfaceTexture());
        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().setVideoSurface(surface);

        if (FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getVideoFormat() != null) {
            Format videoFormat = FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getVideoFormat();
            assert videoFormat != null;
            int width = videoFormat.width;
            int height = videoFormat.height;
            int rotationDegrees = videoFormat.rotationDegrees;
            // Switch the width/height if video was taken in portrait mode
            if (rotationDegrees == 90 || rotationDegrees == 270) {
                width = FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getVideoFormat().height;
                height = FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getVideoFormat().width;
            }

            FlutterMediaPlugin.getInstance().videoInitialize(textureEntry.id(), height, width);
        }
    }

    void play() {
        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().setPlayWhenReady(true);
    }

    void pause() {
        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().setPlayWhenReady(false);
    }

    void setLooping(boolean value) {
        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().setRepeatMode(value ? REPEAT_MODE_ALL : REPEAT_MODE_OFF);
    }

    void setVolume(double value) {
        float bracketedValue = (float) Math.max(0.0, Math.min(1.0, value));
        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().setVolume(bracketedValue);
    }

    void seekTo(int location) {
        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().seekTo(location);
    }

    long getPosition() {
        return FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getCurrentPosition();
    }

    void dispose() {
        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().stop();
        textureEntry.release();
        if (surface != null) {
            surface.release();
        }
        if (FlutterMediaPlugin.getInstance().getSimpleExoPlayer() != null) {
            FlutterMediaPlugin.getInstance().getSimpleExoPlayer().release();
        }
    }

    private void onDestroy() {
    }
//
//    @Override
//    public void onMethodCall(MethodCall call, Result result) {
//        TextureRegistry textures = registrar.textures();
//        if (textures == null) {
//            result.error("no_activity", "video_player plugin requires a foreground activity", null);
//            return;
//        }
//        switch (call.method) {
//            case "init":
//                disposeAllPlayers();
//                break;
//            case "create":
//            {
//
//            }
//            default:
//            {
//                long textureId = ((Number) call.argument("textureId")).longValue();
//                VideoPlayer player = videoPlayers.get(textureId);
//                if (player == null) {
//                    result.error(
//                            "Unknown textureId",
//                            "No video player associated with texture id " + textureId,
//                            null);
//                    return;
//                }
//                break;
//            }
//        }
//    }

    private class VideoExoPlayerListener implements EventListener {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onTimelineChanged(timeline, manifest, reason);
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onTracksChanged(trackGroups, trackSelections);
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onLoadingChanged(isLoading);
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onPlayerStateChanged(playWhenReady, playbackState);
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onPlayerStatus("player state " + FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() + ", " + FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlayWhenReady());
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onRepeatModeChanged(repeatMode);
        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onShuffleModeEnabledChanged(shuffleModeEnabled);
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onPlayerError(error);
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onPositionDiscontinuity(reason);
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onPlaybackParametersChanged(playbackParameters);
        }

        @Override
        public void onSeekProcessed() {
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onSeekProcessed();
        }
    }
}
