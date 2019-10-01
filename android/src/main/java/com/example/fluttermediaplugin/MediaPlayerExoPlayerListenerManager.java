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

public class MediaPlayerExoPlayerListenerManager implements EventListener {
    private static String TAG = "MediaPlayerManager";
    private final Set<ExoPlayerListener> exoPlayerMediaListeners = new CopyOnWriteArraySet<>();
    private Handler playbackPollHandler;
    private Handler bufferingPollHandler;
    private boolean isPollingPlayback = false;
    private boolean isPollingBuffering = false;
    private SimpleExoPlayer simpleExoPlayer;

    MediaPlayerExoPlayerListenerManager(@NonNull SimpleExoPlayer simpleExoPlayer, @NonNull String handlerThreadName) {
        this.simpleExoPlayer = simpleExoPlayer;
        HandlerThread playbackHandlerThread = new HandlerThread(handlerThreadName + "playback");
        playbackHandlerThread.start();
        //playbackPollHandler = new Handler(playbackHandlerThread.getLooper());
        playbackPollHandler = new Handler(Looper.getMainLooper());

        HandlerThread bufferHandlerThread = new HandlerThread(handlerThreadName + "buffer");
        bufferHandlerThread.start();
        //bufferingPollHandler = new Handler(bufferHandlerThread.getLooper());
        bufferingPollHandler = new Handler(Looper.getMainLooper());
    }

    void addExoPlayerListener(@NonNull ExoPlayerListener exoPlayerMediaListener) {
        exoPlayerMediaListeners.add(exoPlayerMediaListener);
    }

    void removeExoPlayerListener(@NonNull ExoPlayerListener exoPlayerMediaListener) {
        exoPlayerMediaListeners.remove(exoPlayerMediaListener);
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
        for (ExoPlayerListener eventListener : exoPlayerMediaListeners) {
            eventListener.onTimelineChanged(timeline, manifest, reason);
        }
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        for (ExoPlayerListener eventListener : exoPlayerMediaListeners) {
            eventListener.onTracksChanged(trackGroups, trackSelections);
        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        if (isLoading) {
            startBufferPolling();
        }

        for (ExoPlayerListener eventListener : exoPlayerMediaListeners) {
            eventListener.onLoadingChanged(isLoading);
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, "PlayWhenReady : " + playWhenReady + ", PlaybackState : " + playbackState);
        for (ExoPlayerListener eventListener : exoPlayerMediaListeners) {
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

        for (ExoPlayerListener listener : exoPlayerMediaListeners)
            listener.onPlayerStatus("player state " + simpleExoPlayer.getPlaybackState() + ", " + simpleExoPlayer.getPlayWhenReady());
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        for (ExoPlayerListener eventListener : exoPlayerMediaListeners) {
            eventListener.onRepeatModeChanged(repeatMode);
        }
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        for (ExoPlayerListener eventListener : exoPlayerMediaListeners) {
            eventListener.onShuffleModeEnabledChanged(shuffleModeEnabled);
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        for (ExoPlayerListener eventListener : exoPlayerMediaListeners) {
            eventListener.onPlayerError(error);
        }
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        for (ExoPlayerListener eventListener : exoPlayerMediaListeners) {
            eventListener.onPositionDiscontinuity(reason);
        }
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        for (ExoPlayerListener eventListener : exoPlayerMediaListeners) {
            eventListener.onPlaybackParametersChanged(playbackParameters);
        }
    }

    @Override
    public void onSeekProcessed() {
        for (ExoPlayerListener eventListener : exoPlayerMediaListeners) {
            eventListener.onSeekProcessed();
        }
    }

//    public void onPlaylistChanged(Playlist playlist) {
//        for (ExoPlayerListener exoPlayerListener : exoPlayerMediaListeners) {
//            exoPlayerListener.onPlaylistChanged(playlist);
//        }
//    }

    void onMediaPeriodCreated(int windowIndex) {
        for (ExoPlayerListener listener : exoPlayerMediaListeners) {
            listener.onMediaPeriodCreated(windowIndex);
        }
    }

    void onPlaybackUpdate(long position, long audioLength) {
        for (ExoPlayerListener listener : exoPlayerMediaListeners) {
            listener.onPlaybackUpdate(position, audioLength);
        }
    }

    void onBufferedUpdate(int percent) {
        for (ExoPlayerListener listener : exoPlayerMediaListeners) {
            listener.onBufferedUpdate(percent);
        }
    }

    void onPlayerStatus(String message) {
        for (ExoPlayerListener listener : exoPlayerMediaListeners) listener.onPlayerStatus(message);
    }
}
