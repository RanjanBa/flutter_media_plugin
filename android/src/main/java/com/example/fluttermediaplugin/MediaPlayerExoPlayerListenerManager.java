package com.example.fluttermediaplugin;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
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

    public MediaPlayerExoPlayerListenerManager(@NonNull String handlerThreadName) {
        HandlerThread playbackHandlerThread = new HandlerThread(handlerThreadName + "playback");
        playbackHandlerThread.start();
        //playbackPollHandler = new Handler(playbackHandlerThread.getLooper());
        playbackPollHandler = new Handler(Looper.getMainLooper());

        HandlerThread bufferHandlerThread = new HandlerThread(handlerThreadName + "buffer");
        bufferHandlerThread.start();
        //bufferingPollHandler = new Handler(bufferHandlerThread.getLooper());
        bufferingPollHandler = new Handler(Looper.getMainLooper());
    }

    public void addExoPlayerListener(@NonNull ExoPlayerListener exoPlayerMediaListener) {
        exoPlayerMediaListeners.add(exoPlayerMediaListener);
    }

    public void removeExoPlayerListener(@NonNull ExoPlayerListener exoPlayerMediaListener) {
        exoPlayerMediaListeners.remove(exoPlayerMediaListener);
    }

    public void stopPlaybackPolling() {
        isPollingPlayback = false;
        if (FlutterMediaPlugin.getInstance().getSimpleExoPlayer() != null) {
            for (ExoPlayerListener listener : exoPlayerMediaListeners) {
                listener.onBufferedUpdate(
                        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getBufferedPercentage() //bufferedPercent()
                );
            }
        }
        playbackPollHandler.removeCallbacks(null);
    }

    public void clear() {
        stopBufferingPolling();
        stopPlaybackPolling();
        this.playbackPollHandler.removeCallbacks(null);
        this.bufferingPollHandler.removeCallbacks(null);
    }

    public void stopBufferingPolling() {
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
//                        for (ExoPlayerListener listener : exoPlayerMediaListeners) {
//                            listener.onBufferedUpdate(
//                                    bufferedPercent()
//                            );
//                        }

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
//                        for (ExoPlayerListener listener : exoPlayerMediaListeners) {
//                            listener.onPlaybackUpdate(
//                                    playbackPosition(),
//                                    audioLength()
//                            );
//                        }

                        playbackPollHandler.postDelayed(this, 500);
                    }
                }
            }, 500);
        }
    }

    private int bufferedPercent() {
        return FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getBufferedPercentage();
    }

    private long playbackPosition() {
        return FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getCurrentPosition();
    }

    private long audioLength() {
        return FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getDuration();
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

//            if (playbackState == Player.STATE_READY) {
//                if (playWhenReady) {
//                    if (requestAudioFocus() != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//                        pause();
//                    }
//                    startPlaybackPolling();
//                } else {
//                    stopPlaybackPolling();
//                }
//            } else {
//                stopPlaybackPolling();
//            }

        if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
            stopBufferingPolling();
        }

        for (ExoPlayerListener listener : exoPlayerMediaListeners)
            listener.onPlayerStatus("player state " + FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() + ", " + FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlayWhenReady());
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

    public void onPlaylistChanged(Playlist playlist) {
        for (ExoPlayerListener exoPlayerListener : exoPlayerMediaListeners) {
            exoPlayerListener.onPlaylistChanged(playlist);
        }
    }

    public void onMediaPeriodCreated(int windowIndex) {
        for (ExoPlayerListener listener : exoPlayerMediaListeners) {
            listener.onMediaPeriodCreated(windowIndex);
        }
    }

    public void onPlaybackUpdate(long position, long audioLength) {
        for (ExoPlayerListener listener : exoPlayerMediaListeners) {
            listener.onPlaybackUpdate(position, audioLength);
        }
    }

    public void onBufferedUpdate(int percent) {
        for (ExoPlayerListener listener : exoPlayerMediaListeners) {
            listener.onBufferedUpdate(percent);
        }
    }

    public void onPlayerStatus(String message) {
        for (ExoPlayerListener listener : exoPlayerMediaListeners) listener.onPlayerStatus(message);
    }
}
