package com.example.fluttermediaplugin;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.flutter.plugin.common.MethodChannel;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.fluttermediaplugin.FlutterMediaPlugin.AUDIO_METHOD_TYPE;
import static com.google.android.exoplayer2.C.USAGE_MEDIA;

class AudioPlayer {
    private static final String AUDIO_EXO_PLAYER_LISTENER_THREAD_NAME = "audio_player_thread_name";
    private static final String TAG = "AudioPlayer";

    private AudioExoPlayerListener audioExoPlayerListener;
    private SimpleExoPlayer simpleExoPlayer;
    private MethodChannel channel;

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

    AudioPlayer(@NonNull Context context, @NonNull MethodChannel channel) {
        this.channel = channel;
        initSimpleExoPlayer(context);
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
                audioExoPlayerListener.onMediaPeriodCreated(windowIndex);
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

        audioExoPlayerListener = new AudioExoPlayerListener();
        simpleExoPlayer.addListener(audioExoPlayerListener);
    }

    private void showAudioPlayerNotification() {
        if (simpleExoPlayer == null || isShowingNotification) {
            Log.d(TAG, "already showing notification");
            return;
        }

        Intent intent = new Intent(FlutterMediaPlugin.getInstance().getRegistrar().activeContext(), MediaPlayerNotificationService.class);
        Util.startForegroundService(FlutterMediaPlugin.getInstance().getRegistrar().activeContext(), intent);
    }

    void initialize(MethodChannel.Result result) {
        if (simpleExoPlayer == null) {
            return;
        }

        int playbackState = simpleExoPlayer.getPlaybackState();
        boolean playWhenReady = simpleExoPlayer.getPlayWhenReady();
        Map<String, Object> args = new HashMap<>();
        args.put("playWhenReady", playWhenReady);
        args.put("playbackState", playbackState);

        Song song = getSongByIndex(simpleExoPlayer.getCurrentWindowIndex());
        if (song == null) {
            args.put("currentPlayingSong", null);
        } else {
            Map<String, Object> songMap = Song.toMap(song);
            args.put("currentPlayingSong", songMap);
        }
        result.success(args);
    }

    void setChannel(MethodChannel channel) {
        this.channel = channel;
    }

    void release() {
        if (simpleExoPlayer != null) {
            simpleExoPlayer.removeListener(audioExoPlayerListener);
            simpleExoPlayer.release();
        }

        this.playlist.clear();
        if (MediaPlayerNotificationService.getInstance() != null) {
            MediaPlayerNotificationService.getInstance().getPlayerNotificationManager().setPlayer(null);
        }
        audioExoPlayerListener = null;
    }

    void stop() {
        simpleExoPlayer.stop(false);
        audioExoPlayerListener.clear();
        audioExoPlayerListener.onBufferedUpdate(0);
        audioExoPlayerListener.onPlaybackUpdate(0, 0);
        if (simpleExoPlayer != null) {
            audioExoPlayerListener.onPlayerStateChanged(simpleExoPlayer.getPlayWhenReady(), simpleExoPlayer.getPlaybackState());
            audioExoPlayerListener.onPlayerStatus("Audio is in stop state " + simpleExoPlayer.getPlaybackState() + ", " + simpleExoPlayer.getPlayWhenReady() + ", playlist length : " + playlist.getSize());
        }
        if (MediaPlayerNotificationService.getInstance() != null)
            MediaPlayerNotificationService.getInstance().stopService(true);
        playlist.clear();
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
        if (playlist.getSize() <= 0) {
            audioExoPlayerListener.onPlayerStatus("No audio playlist is present");
            if (isShowingNotification && MediaPlayerNotificationService.getInstance() != null) {
                MediaPlayerNotificationService.getInstance().stopService(true);
            }
            return;
        }

        if (simpleExoPlayer.getPlaybackState() == Player.STATE_IDLE || simpleExoPlayer.getPlaybackState() == Player.STATE_ENDED) {
            preparePlaylist();
        }

        if (!simpleExoPlayer.getPlayWhenReady()) {
            simpleExoPlayer.setPlayWhenReady(true);
        } else {
            audioExoPlayerListener.onPlayerStatus("Audio is already in playing state " + simpleExoPlayer.getPlaybackState() + ", " + simpleExoPlayer.getPlayWhenReady());
//            audioExoPlayerListener.onPlayerStateChanged(simpleExoPlayer.getPlayWhenReady(), simpleExoPlayer.getPlaybackState());
        }
    }

    void pause() {
        if (simpleExoPlayer.getPlayWhenReady()) {
            simpleExoPlayer.setPlayWhenReady(false);
        } else {
            audioExoPlayerListener.onPlayerStatus("Audio is already in paused state " + simpleExoPlayer.getPlaybackState() + ", " + simpleExoPlayer.getPlayWhenReady());
//            audioExoPlayerListener.onPlayerStateChanged(simpleExoPlayer.getPlayWhenReady(), simpleExoPlayer.getPlaybackState());
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

    private class AudioExoPlayerListener extends MediaExoPlayerListener {
        AudioExoPlayerListener() {
            super(simpleExoPlayer, AUDIO_EXO_PLAYER_LISTENER_THREAD_NAME);
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

            super.onPlayerStateChanged(playWhenReady, playbackState);

            Map<String, Object> args = new HashMap<>();
            args.put("playWhenReady", playWhenReady);
            args.put("playbackState", playbackState);
            String method = AUDIO_METHOD_TYPE + "/onPlayerStateChanged";
//                Log.d(TAG, "onPlayerStateChanged : " + playbackState + ", " + method);
            channel.invokeMethod(method, args);
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            super.onRepeatModeChanged(repeatMode);

            Map<String, Object> args = new HashMap<>();
            args.put("repeatMode", repeatMode);
            String method = AUDIO_METHOD_TYPE + "/onRepeatModeChanged";
//                Log.d(TAG, "onRepeatModeChanged : " + repeatMode + ", " + method);
            channel.invokeMethod(method, args);
        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            super.onShuffleModeEnabledChanged(shuffleModeEnabled);

            Map<String, Object> args = new HashMap<>();
            args.put("shuffleModeEnabled", shuffleModeEnabled);
            String method = AUDIO_METHOD_TYPE + "/onShuffleModeEnabledChanged";
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
            super.onMediaPeriodCreated(windowIndex);

            Log.d(TAG, "onMediaPeriodCreated");
            Map<String, Object> args = new HashMap<>();
            args.put("windowIndex", windowIndex);
            Song song = getSongByIndex(windowIndex);
            if (song == null)
                return;

            Map<String, Object> songMap = Song.toMap(song);
            args.put("currentPlayingSong", songMap);
            String method = AUDIO_METHOD_TYPE + "/onMediaPeriodCreated";
            channel.invokeMethod(method, args);
        }

        @Override
        public void onPlaybackUpdate(long position, long audioLength) {
            super.onPlaybackUpdate(position, audioLength);

            //Log.d(TAG, "onPlaybackUpdate");
            Map<String, Object> args = new HashMap<>();
            args.put("position", position);
            args.put("audioLength", audioLength);
            String method = AUDIO_METHOD_TYPE + "/onPlaybackUpdate";
//                Log.d(TAG, "Playback update");
            channel.invokeMethod(method, args);
        }

        @Override
        public void onBufferedUpdate(int percent) {
            super.onBufferedUpdate(percent);

            //Log.d(TAG, "onBufferedUpdate " + percent);
            Map<String, Object> args = new HashMap<>();
            args.put("percent", percent);
            String method = AUDIO_METHOD_TYPE + "/onBufferedUpdate";
            channel.invokeMethod(method, args);
        }

        @Override
        public void onPlayerStatus(String message) {
            super.onPlayerStatus(message);

            Map<String, Object> args = new HashMap<>();
            args.put("message", message);
            String method = AUDIO_METHOD_TYPE + "/onPlayerStatus";
            channel.invokeMethod(method, args);
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.d(TAG, "onPlayerError");

            super.onPlayerError(error);
        }
    }
}
