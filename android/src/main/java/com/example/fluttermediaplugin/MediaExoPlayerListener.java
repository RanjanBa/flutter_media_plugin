package com.example.fluttermediaplugin;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MediaExoPlayerListener implements EventListener {
    private static String TAG = "MediaPlayerManager";
    private final Set<ExoPlayerListener> mediaExoPlayerListeners = new CopyOnWriteArraySet<>();
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

    void addExoPlayerListener(@NonNull ExoPlayerListener exoPlayerMediaListener) {
        mediaExoPlayerListeners.add(exoPlayerMediaListener);
    }

    void removeExoPlayerListener(@NonNull ExoPlayerListener exoPlayerMediaListener) {
        mediaExoPlayerListeners.remove(exoPlayerMediaListener);
    }

    void stopPlaybackPolling() {
        isPollingPlayback = false;
        if (simpleExoPlayer != null) {
            onBufferedUpdate(bufferedPercent());
        }
        playbackPollHandler.removeCallbacks(null);
    }

//    void clear() {
//        stopBufferingPolling();
//        stopPlaybackPolling();
//        this.playbackPollHandler.removeCallbacks(null);
//        this.bufferingPollHandler.removeCallbacks(null);
//    }

    void stopBufferingPolling() {
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

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
        for (ExoPlayerListener eventListener : mediaExoPlayerListeners) {
            eventListener.onTimelineChanged(timeline, manifest, reason);
        }
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        for (ExoPlayerListener eventListener : mediaExoPlayerListeners) {
            eventListener.onTracksChanged(trackGroups, trackSelections);
        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        if (isLoading) {
            startBufferPolling();
        }

        for (ExoPlayerListener eventListener : mediaExoPlayerListeners) {
            eventListener.onLoadingChanged(isLoading);
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, "PlayWhenReady : " + playWhenReady + ", PlaybackState : " + playbackState);
        for (ExoPlayerListener eventListener : mediaExoPlayerListeners) {
            eventListener.onPlayerStateChanged(playWhenReady, playbackState);
        }

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



        for (ExoPlayerListener listener : mediaExoPlayerListeners)
            listener.onPlayerStatus("player state " + simpleExoPlayer.getPlaybackState() + ", " + simpleExoPlayer.getPlayWhenReady());
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        for (ExoPlayerListener eventListener : mediaExoPlayerListeners) {
            eventListener.onRepeatModeChanged(repeatMode);
        }
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        for (ExoPlayerListener eventListener : mediaExoPlayerListeners) {
            eventListener.onShuffleModeEnabledChanged(shuffleModeEnabled);
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        for (ExoPlayerListener eventListener : mediaExoPlayerListeners) {
            eventListener.onPlayerError(error);
        }
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        for (ExoPlayerListener eventListener : mediaExoPlayerListeners) {
            eventListener.onPositionDiscontinuity(reason);
        }
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        for (ExoPlayerListener eventListener : mediaExoPlayerListeners) {
            eventListener.onPlaybackParametersChanged(playbackParameters);
        }
    }

    @Override
    public void onSeekProcessed() {
        for (ExoPlayerListener eventListener : mediaExoPlayerListeners) {
            eventListener.onSeekProcessed();
        }
    }

    void onMediaPeriodCreated(int windowIndex) {
        for (ExoPlayerListener listener : mediaExoPlayerListeners) {
            listener.onMediaPeriodCreated(windowIndex);
        }
    }

    void onPlaybackUpdate(long position, long audioLength) {
        for (ExoPlayerListener listener : mediaExoPlayerListeners) {
            listener.onPlaybackUpdate(position, audioLength);
        }
    }

    void onBufferedUpdate(int percent) {
        for (ExoPlayerListener listener : mediaExoPlayerListeners) {
            listener.onBufferedUpdate(percent);
        }
    }

    void onPlayerStatus(String message) {
        for (ExoPlayerListener listener : mediaExoPlayerListeners) listener.onPlayerStatus(message);
    }
}
