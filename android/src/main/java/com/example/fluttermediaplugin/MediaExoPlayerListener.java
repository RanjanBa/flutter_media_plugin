package com.example.fluttermediaplugin;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.fluttermediaplugin.Media.Media;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

public abstract class MediaExoPlayerListener<T extends Media> implements Player.EventListener {
    private static String TAG = "MediaPlayerManager";
    private Handler playbackPollHandler;
    private Handler bufferingPollHandler;
    private boolean isPollingPlayback = false;
    private boolean isPollingBuffering = false;
    private SimpleExoPlayer simpleExoPlayer;

    MediaExoPlayerListener(@NonNull SimpleExoPlayer simpleExoPlayer, @NonNull String handlerThreadName) {
        this.simpleExoPlayer = simpleExoPlayer;
        HandlerThread playbackHandlerThread = new HandlerThread(handlerThreadName + "playback");
        playbackHandlerThread.start();
        playbackPollHandler = new Handler(Looper.getMainLooper());

        HandlerThread bufferHandlerThread = new HandlerThread(handlerThreadName + "buffer");
        bufferHandlerThread.start();
        bufferingPollHandler = new Handler(Looper.getMainLooper());
    }

    private void stopPlaybackPolling() {
        isPollingPlayback = false;
        if (simpleExoPlayer != null) {
            onBufferedUpdate(bufferedPercent());
        }
        playbackPollHandler.removeCallbacks(null);
    }

    private void stopBufferingPolling() {
        isPollingBuffering = false;
        bufferingPollHandler.removeCallbacks(null);
    }

    private void startBufferPolling() {
        if (!isPollingBuffering) {
            isPollingBuffering = true;
            bufferingPollHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isPollingBuffering) {
                        onBufferedUpdate(bufferedPercent());
                        if (bufferedPercent() >= 100) {
                            stopBufferingPolling();
                            return;
                        }
                        bufferingPollHandler.postDelayed(this, 500);
                    }
                }
            }, 500);
        }
    }

    private void startPlaybackPolling() {
        if (!isPollingPlayback) {
            isPollingPlayback = true;
            playbackPollHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isPollingPlayback) {
                        onPlaybackUpdate(playbackPosition(), audioLength());
                        playbackPollHandler.postDelayed(this, 500);
                    }
                }
            }, 500);
        }
    }

    private int bufferedPercent() {
        return simpleExoPlayer.getBufferedPercentage();
    }

    private long playbackPosition() {
        return simpleExoPlayer.getCurrentPosition();
    }

    private long audioLength() {
        return simpleExoPlayer.getDuration();
    }

    void clear() {
        stopBufferingPolling();
        stopPlaybackPolling();
        this.playbackPollHandler.removeCallbacks(null);
        this.bufferingPollHandler.removeCallbacks(null);
    }

    void onPlaylistChanged(@NonNull Playlist<T> playlist) {
    }

    void onMediaAddedToPlaylist(String playlistName, int index, @NonNull T media) {
    }

    void onMediaRemovedFromPlaylist(String playlistName, int index, @NonNull T media) {
    }


    void onMediaPeriodCreated(int windowIndex) {
    }

    void onPlaybackUpdate(long position, long audioLength) {
    }

    void onBufferedUpdate(int percent) {
    }

    void onPlayerStatus(String message) {
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        if (isLoading) {
            startBufferPolling();
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, "PlayWhenReady : " + playWhenReady + ", PlaybackState : " + playbackState);

        if (playbackState == Player.STATE_BUFFERING) {
            startBufferPolling();
        }
        if (playbackState == Player.STATE_READY) {
            if (playWhenReady) {
                startPlaybackPolling();
            } else {
                stopPlaybackPolling();
            }
        } else {
            stopPlaybackPolling();
        }

        if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
            stopBufferingPolling();
        }

        onPlayerStatus("player state " + simpleExoPlayer.getPlaybackState() + ", " + simpleExoPlayer.getPlayWhenReady());
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
    }

    @Override
    public void onSeekProcessed() {
    }
}
