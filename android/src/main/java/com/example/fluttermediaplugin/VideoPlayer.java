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
import com.google.android.exoplayer2.Player.EventListener;
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

import io.flutter.view.TextureRegistry;

import static com.google.android.exoplayer2.C.CONTENT_TYPE_MOVIE;
import static com.google.android.exoplayer2.C.USAGE_MEDIA;
import static com.google.android.exoplayer2.Player.REPEAT_MODE_ALL;
import static com.google.android.exoplayer2.Player.REPEAT_MODE_OFF;

class VideoPlayer {
    private static String TAG = "VideoPlayer";

    private VideoExoPlayerListener videoExoPlayerListener;
    private MediaPlayerExoPlayerListenerManager mediaPlayerExoPlayerListenerManager;

    private SimpleExoPlayer simpleExoPlayer;
    private Context context;

    private Surface surface;
    private TextureRegistry.SurfaceTextureEntry textureEntry;

    VideoPlayer(Context context) {
        this.context = context;
        initializeSimpleExoPlayer(context);
        mediaPlayerExoPlayerListenerManager = new MediaPlayerExoPlayerListenerManager(simpleExoPlayer, "videoPlayer");
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
                videoExoPlayerListener.videoInitialize(textureEntry.id(), height, width, simpleExoPlayer.getDuration());
            }
        });
    }

    void addExoPlayerListener(ExoPlayerListener exoPlayerListener) {
        mediaPlayerExoPlayerListenerManager.addExoPlayerListener(exoPlayerListener);
    }

    void removeExoPlayerListener(ExoPlayerListener exoPlayerListener) {
        mediaPlayerExoPlayerListenerManager.removeExoPlayerListener(exoPlayerListener);
    }

    void addAndPlay(String stringUri, TextureRegistry.SurfaceTextureEntry textureEntry) {
        this.textureEntry = textureEntry;
        Uri uri = Uri.parse(stringUri);

        Log.d(TAG, "Uri : " + uri);

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

    void setupVideoPlayer(
            @NonNull TextureRegistry.SurfaceTextureEntry textureEntry) {
        this.textureEntry = textureEntry;
        surface = new Surface(textureEntry.surfaceTexture());
        simpleExoPlayer.setVideoSurface(surface);

        if (simpleExoPlayer.getVideoFormat() != null) {
            Format videoFormat = simpleExoPlayer.getVideoFormat();
            assert videoFormat != null;
            int width = videoFormat.width;
            int height = videoFormat.height;
            int rotationDegrees = videoFormat.rotationDegrees;
            // Switch the width/height if video was taken in portrait mode
            if (rotationDegrees == 90 || rotationDegrees == 270) {
                width = simpleExoPlayer.getVideoFormat().height;
                height = simpleExoPlayer.getVideoFormat().width;
            }
            videoExoPlayerListener.videoInitialize(textureEntry.id(), height, width, simpleExoPlayer.getDuration());
        }
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

    private class VideoExoPlayerListener implements EventListener {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
            mediaPlayerExoPlayerListenerManager.onTimelineChanged(timeline, manifest, reason);
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            mediaPlayerExoPlayerListenerManager.onTracksChanged(trackGroups, trackSelections);
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            mediaPlayerExoPlayerListenerManager.onLoadingChanged(isLoading);
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            mediaPlayerExoPlayerListenerManager.onPlayerStateChanged(playWhenReady, playbackState);
            mediaPlayerExoPlayerListenerManager.onPlayerStatus("player state " + simpleExoPlayer.getPlaybackState() + ", " + simpleExoPlayer.getPlayWhenReady());
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            mediaPlayerExoPlayerListenerManager.onRepeatModeChanged(repeatMode);
        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            mediaPlayerExoPlayerListenerManager.onShuffleModeEnabledChanged(shuffleModeEnabled);
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            mediaPlayerExoPlayerListenerManager.onPlayerError(error);
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            mediaPlayerExoPlayerListenerManager.onPositionDiscontinuity(reason);
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            mediaPlayerExoPlayerListenerManager.onPlaybackParametersChanged(playbackParameters);
        }

        @Override
        public void onSeekProcessed() {
            mediaPlayerExoPlayerListenerManager.onSeekProcessed();
        }

        void videoInitialize(long textureId, int height, int width, long duration) {
            FlutterMediaPlugin.getInstance().videoInitialize(textureId, height, width, duration);
        }
    }
}
