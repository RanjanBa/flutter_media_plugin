package com.example.fluttermediaplugin;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import static com.google.android.exoplayer2.C.USAGE_MEDIA;

class AudioPlayer {
    private static final String TAG = "AudioPlayer";

    private AudioExoPlayerListener audioEventListener;
    private MediaPlayerExoPlayerListenerManager mediaPlayerExoPlayerListenerManager;
    private SimpleExoPlayer simpleExoPlayer;

    private boolean isShowingNotification = false;
    private Playlist playlist;
    Playlist getPlaylist() {
        return playlist;
    }

    Song getSongByIndex(int index) {
        if (playlist == null)
            return null;
        return playlist.getSongAtIndex(index);
    }

    SimpleExoPlayer getSimpleExoPlayer() {
        return simpleExoPlayer;
    }

    AudioPlayer(@NonNull Context context) {
        initSimpleExoPlayer(context);
        mediaPlayerExoPlayerListenerManager = new MediaPlayerExoPlayerListenerManager(simpleExoPlayer, "audioPlayer");
    }

    private void initSimpleExoPlayer(Context context) {
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(context, "audio_player");
        TrackSelector trackSelector = new DefaultTrackSelector();
        if (simpleExoPlayer != null) {
            simpleExoPlayer.stop();
            simpleExoPlayer.release();
        }

        simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build();

        simpleExoPlayer.setAudioAttributes(audioAttributes, true);

//        playerId = simpleExoPlayer.getAudioSessionId();

        MediaSourceEventListener playlistEventListener = new MediaSourceEventListener() {
            @Override
            public void onMediaPeriodCreated(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
                mediaPlayerExoPlayerListenerManager.onMediaPeriodCreated(windowIndex);
                Log.d(TAG + "CC", "onMediaPeriodCreated : " + windowIndex);
            }

            @Override
            public void onMediaPeriodReleased(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
                //Log.d(TAG + "CC", "on media Period Released : " + windowIndex + ",media period : " + mediaPeriodId.periodIndex);
            }

            @Override
            public void onLoadStarted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
                //Log.d(TAG + "CC", "on load started : " + windowIndex + ",media period : " + mediaPeriodId.periodIndex);
            }

            @Override
            public void onLoadCompleted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
                //Log.d(TAG + "CC", "on load Completed : " + windowIndex + ",media period : " + mediaPeriodId.periodIndex);
            }

            @Override
            public void onLoadCanceled(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
                //Log.d(TAG + "CC", "on load Canceled : " + windowIndex + ",media period : " + mediaPeriodId.periodIndex);
            }

            @Override
            public void onLoadError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
                //Log.d(TAG + "CC", "on load error : " + windowIndex + ",media period : " + mediaPeriodId.periodIndex);
            }

            @Override
            public void onReadingStarted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
                //Log.d(TAG + "CC", "on Reading Started : " + windowIndex + ",media period : " + mediaPeriodId.periodIndex);
            }

            @Override
            public void onUpstreamDiscarded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
                //Log.d(TAG + "CC", "on Upstream Discarded : " + windowIndex + ",media period : " + mediaPeriodId.periodIndex);
            }

            @Override
            public void onDownstreamFormatChanged(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
                //Log.d(TAG + "CC", "on Down stream discarded : " + windowIndex + ",media period : " + mediaPeriodId.periodIndex);
            }
        };
        playlist = new Playlist("currentPlaylist", simpleExoPlayer, playlistEventListener, dataSourceFactory);

        if (audioEventListener == null) {
            audioEventListener = new AudioExoPlayerListener();
        }
        simpleExoPlayer.addListener(audioEventListener);
    }

    void release() {
        if (simpleExoPlayer != null) {
            simpleExoPlayer.removeListener(audioEventListener);
            simpleExoPlayer.release();
        }

        this.playlist.clear();
        if (MediaPlayerNotificationService.getInstance() != null) {
            MediaPlayerNotificationService.getInstance().getPlayerNotificationManager().setPlayer(null);
        }
        audioEventListener = null;
    }

    void stop() {
        simpleExoPlayer.stop(false);
        mediaPlayerExoPlayerListenerManager.stopBufferingPolling();
        mediaPlayerExoPlayerListenerManager.stopPlaybackPolling();
        mediaPlayerExoPlayerListenerManager.onBufferedUpdate(0);
        mediaPlayerExoPlayerListenerManager.onPlaybackUpdate(0, 0);
        if (simpleExoPlayer != null) {
            mediaPlayerExoPlayerListenerManager.onPlayerStatus("stop player state " + simpleExoPlayer.getPlaybackState() + ", " + simpleExoPlayer.getPlayWhenReady() + ", playlist length : " + playlist.getSize());
            mediaPlayerExoPlayerListenerManager.onPlayerStateChanged(simpleExoPlayer.getPlayWhenReady(), simpleExoPlayer.getPlaybackState());
        }
        if (MediaPlayerNotificationService.getInstance() != null)
            MediaPlayerNotificationService.getInstance().stopService(true);
        playlist.clear();
    }

    void addExoPlayerListener(@NonNull ExoPlayerListener exoPlayerMediaListener) {
        mediaPlayerExoPlayerListenerManager.addExoPlayerListener(exoPlayerMediaListener);
    }

    void removeExoPlayerListener(@NonNull ExoPlayerListener exoPlayerMediaListener) {
        mediaPlayerExoPlayerListenerManager.addExoPlayerListener(exoPlayerMediaListener);
    }

    private void showAudioPlayerNotification() {
        if (simpleExoPlayer == null) {
            Log.d(TAG, "simple exo player is null");
            return;
        }

        if (isShowingNotification) {
            Log.d(TAG, "already showing notification");
            return;
        }

        Intent intent = new Intent(FlutterMediaPlugin.getInstance().getRegistrar().activeContext(), MediaPlayerNotificationService.class);
        Util.startForegroundService(FlutterMediaPlugin.getInstance().getRegistrar().activeContext(), intent);
    }

    void onNotificationStarted() {
        isShowingNotification = true;
    }

    void onNotificationDestroyed() {
        isShowingNotification = false;
        stop();
        playlist.clear();
    }

    void preparePlaylist() {
        playlist.prepare();
    }

    void clearPlaylist() {
        playlist.clear();
    }

    void play() {
        if (simpleExoPlayer.getPlaybackState() == Player.STATE_IDLE && simpleExoPlayer.getPlaybackState() == Player.STATE_ENDED) {
            preparePlaylist();
        }

        if (!simpleExoPlayer.getPlayWhenReady()) {
            simpleExoPlayer.setPlayWhenReady(true);
        } else {
            mediaPlayerExoPlayerListenerManager.onPlayerStatus("Already playing player state " + simpleExoPlayer.getPlaybackState() + ", " + simpleExoPlayer.getPlayWhenReady());
            mediaPlayerExoPlayerListenerManager.onPlayerStateChanged(simpleExoPlayer.getPlayWhenReady(), simpleExoPlayer.getPlaybackState());
        }
        Log.d(TAG, "Already playing");
    }

    void pause() {
        if (simpleExoPlayer.getPlayWhenReady()) {
            simpleExoPlayer.setPlayWhenReady(false);
        } else {
            mediaPlayerExoPlayerListenerManager.onPlayerStatus("Already paused, player state " + simpleExoPlayer.getPlaybackState() + ", " + simpleExoPlayer.getPlayWhenReady());
            mediaPlayerExoPlayerListenerManager.onPlayerStateChanged(simpleExoPlayer.getPlayWhenReady(), simpleExoPlayer.getPlaybackState());
            Log.d(TAG, "Already paused");
        }
    }

    void setRepeatMode(@Player.RepeatMode int repeatMode) {
        simpleExoPlayer.setRepeatMode(repeatMode);
    }

    int getRepeatMode() {
        return simpleExoPlayer.getRepeatMode();
    }

    void setShuffleModeEnabled(boolean shuffleModeEnabled) {
        simpleExoPlayer.setShuffleModeEnabled(shuffleModeEnabled);
    }

    boolean getShuffleModeEnabled() {
        return simpleExoPlayer.getShuffleModeEnabled();
    }

    void skipToIndex(int index) {
        if (simpleExoPlayer.getPlaybackState() == Player.STATE_ENDED || simpleExoPlayer.getPlaybackState() == Player.STATE_IDLE) {
            return;
        }
        playlist.skipToIndex(index);
    }

    void skipToNext() {
        if (simpleExoPlayer.getPlaybackState() == Player.STATE_ENDED || simpleExoPlayer.getPlaybackState() == Player.STATE_IDLE) {
            return;
        }
        playlist.skipToNext();
    }

    void skipToPrevious() {
        if (simpleExoPlayer.getPlaybackState() == Player.STATE_ENDED || simpleExoPlayer.getPlaybackState() == Player.STATE_IDLE) {
            return;
        }
        playlist.skipToPrevious();
    }

    void playNext(Song song) {
        playlist.addSong(simpleExoPlayer.getCurrentWindowIndex() + 1, song);
    }

    void addAndPlay(Song song) {
        playlist.addAndPlay(song);
    }

    void addSong(Song song) {
        playlist.addSong(song);
    }

    void addSongAtIndex(int index, Song song) {
        playlist.addSong(index, song);
    }

    void setPlaylist(String playlistStr) {
        try {
            JSONObject jsonObject = new JSONObject(playlistStr);
            List<Song> songs = Playlist.songsFromPlaylistJson(jsonObject);
            if (songs != null) {
                this.playlist.clear();
                this.playlist.addSongs(songs);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void removeSong(Song song) {
        playlist.removeSong(song);
    }

    void seekTo(long position) {
        if (simpleExoPlayer.getPlaybackState() == Player.STATE_ENDED || simpleExoPlayer.getPlaybackState() == Player.STATE_IDLE) {
            return;
        }
        if (audioLength() > position) {
            simpleExoPlayer.seekTo(position);
        }

        Log.d(TAG, "seek to " + position);
    }

    private long audioLength() {
        return simpleExoPlayer.getDuration();
    }

    private class AudioExoPlayerListener implements EventListener {
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
            if (MediaPlayerNotificationService.getInstance() != null) {
                if (MediaPlayerNotificationService.getInstance().getPlayerNotificationManager() != null) {
                    if (playWhenReady) {
                        MediaPlayerNotificationService.getInstance().getPlayerNotificationManager().setOngoing(true);
                    } else {
                        MediaPlayerNotificationService.getInstance().stopService(false);
                        MediaPlayerNotificationService.getInstance().getPlayerNotificationManager().setOngoing(false);
                    }
                }
            } else {
                if (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY) {
                    Log.d(TAG, "Media Notification is null " + playbackState);
                    showAudioPlayerNotification();
                }
            }

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
    }
}
