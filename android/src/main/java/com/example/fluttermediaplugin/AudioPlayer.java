package com.example.fluttermediaplugin;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

public class AudioPlayer {
    private static final String TAG = "AudioPlayer";
//    private static int playerId = -1;

    private AudioExoPlayerListener audioEventListener;
    //private SimpleExoPlayer simpleExoPlayer;
//    private AudioManager audioManager;


    //    private AudioManager.OnAudioFocusChangeListener afChangeListener;
//    private boolean isPlayingBeforeInterrupted = false;
    private boolean isShowingNotification = false;
    private DefaultDataSourceFactory dataSourceFactory;
    private Playlist.PlaylistEventListener playlistEventListener;
    private Playlist playlist;

    public Playlist getPlaylist() {
        return playlist;
    }

    public Song getSongByIndex(int index) {
        if (playlist == null)
            return null;

        return playlist.getSongAtIndex(index);
    }

    public AudioPlayer(@NonNull Context context) {//, @NonNull SimpleExoPlayer simpleExoPlayer) {
//        this.simpleExoPlayer = simpleExoPlayer;
        initSimpleExoPlayer(context);

//        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

//        afChangeListener =
//                new AudioManager.OnAudioFocusChangeListener() {
//                    public void onAudioFocusChange(int focusChange) {
//                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
//                            // Permanent loss of audio focus
//                            if (isPlaying()) {
//                                pause();
//                            }
//                        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
//                            // Pause playback
//                            isPlayingBeforeInterrupted = isPlaying();
//                            pause();
//                        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
//                            // Lower the volume, keep playing
//                            isPlayingBeforeInterrupted = isPlaying();
//                            pause();
//                        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
//                            // Your app has been granted audio focus again
//                            // Raise volume to normal, restart playback if necessary
//                            if (isPlayingBeforeInterrupted) {
//                                play();
//                            }
//                        }
//                    }
//                };
    }

    private void initSimpleExoPlayer(Context context) {
        dataSourceFactory = new DefaultDataSourceFactory(context, "audio_player");
//        mediaPlayerExoPlayerListenerManager = new MediaPlayerExoPlayerListenerManager("audioPlayer");
//        playerId = simpleExoPlayer.getAudioSessionId();

        playlistEventListener = new Playlist.PlaylistEventListener() {
            @Override
            public void onPlaylistChanged(Playlist playlist) {
                FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onPlaylistChanged(playlist);
            }

            @Override
            public void onMediaPeriodCreated(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
                FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onMediaPeriodCreated(windowIndex);
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
        playlist = new Playlist("currentPlaylist", playlistEventListener, dataSourceFactory);
    }

    public void addAudioEventListener() {
        if (audioEventListener == null) {
            audioEventListener = new AudioExoPlayerListener();
        }

        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().addListener(audioEventListener);
    }

//    public void removeAudioEventListener() {
//        if (audioEventListener == null) {
//            return;
//        }
//        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().removeListener(audioEventListener);
//        if (mediaPlayerExoPlayerListenerManager != null) {
//            mediaPlayerExoPlayerListenerManager.stopBufferingPolling();
//            mediaPlayerExoPlayerListenerManager.stopPlaybackPolling();
//        }
//    }

    public void release() {
//        if (mediaPlayerExoPlayerListenerManager != null) {
//            mediaPlayerExoPlayerListenerManager.clear();
//        }

        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().release();
//        this.mediaPlayerExoPlayerListenerManager = null;
        this.playlist.clear();
        if (MediaPlayerNotificationService.getInstance() != null) {
            MediaPlayerNotificationService.getInstance().getPlayerNotificationManager().setPlayer(null);
        }
        audioEventListener = null;
    }

    private void stop() {
        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().stop(false);
        if (FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager() != null) {
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().stopBufferingPolling();
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().stopPlaybackPolling();
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onPlayerStatus("stop player state " + FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() + ", " + FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlayWhenReady() + ", playlist length : " + playlist.getSize());
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onPlaybackUpdate(0, 0);
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onBufferedUpdate(0);
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onPlayerStateChanged(FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlayWhenReady(), FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState());
        }
        playlist.clear();
    }

//    public void addExoPlayerListener(@NonNull ExoPlayerListener exoPlayerMediaListener) {
//        FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().addExoPlayerListener(exoPlayerMediaListener);
//    }
//
//    public void removeExoPlayerListener(@NonNull ExoPlayerListener exoPlayerMediaListener) {
//        FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().addExoPlayerListener(exoPlayerMediaListener);
//    }

    private void showAudioPlayerNotification() {
        if (FlutterMediaPlugin.getInstance().getSimpleExoPlayer() == null) {
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

    public void onNotificationStarted() {
        isShowingNotification = true;
    }

    public void onNotificationDestroyed() {
        isShowingNotification = false;
        stop();
        playlist.clear();
    }

    public void preparePlaylist() {
        playlist.prepare();
    }

    public void clearPlaylist() {
        playlist.clear();
    }

    public void play() {
        if (FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() == Player.STATE_IDLE && FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() == Player.STATE_ENDED) {
            preparePlaylist();
        }

        if (!FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlayWhenReady()) {
            FlutterMediaPlugin.getInstance().getSimpleExoPlayer().setPlayWhenReady(true);
        } else {
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onPlayerStatus("Already playing player state " + FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() + ", " + FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlayWhenReady());
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onPlayerStateChanged(FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlayWhenReady(), FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState());
        }
        Log.d(TAG, "Already playing");
    }

    public void pause() {
        if (FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlayWhenReady()) {
            FlutterMediaPlugin.getInstance().getSimpleExoPlayer().setPlayWhenReady(false);
        } else {
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onPlayerStatus("Already paused, player state " + FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() + ", " + FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlayWhenReady());
            FlutterMediaPlugin.getInstance().getMediaPlayerExoPlayerListenerManager().onPlayerStateChanged(FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlayWhenReady(), FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState());
            Log.d(TAG, "Already paused");
        }
    }

    public void setRepeatMode(@Player.RepeatMode int repeatMode) {
        FlutterMediaPlugin.getInstance().getSimpleExoPlayer().setRepeatMode(repeatMode);
    }

    public void skipToIndex(int index) {
        if (FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() == Player.STATE_ENDED || FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() == Player.STATE_IDLE) {
            return;
        }
        playlist.skipToIndex(index);
    }

    public void skipToNext() {
        if (FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() == Player.STATE_ENDED || FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() == Player.STATE_IDLE) {
            return;
        }
        playlist.skipToNext();
    }

    public void skipToPrevious() {
        if (FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() == Player.STATE_ENDED || FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() == Player.STATE_IDLE) {
            return;
        }
        playlist.skipToPrevious();
    }

    public void addAndPlay(Song song) {
        playlist.addAndPlay(song);
    }

    public void addSong(Song song) {
        playlist.addSong(song);
    }

    public void addSongAtIndex(int index, Song song) {
        playlist.addSong(index, song);
    }

    public void setPlaylist(String playlistStr, int playIndex) {
        try {
            JSONObject jsonObject = new JSONObject(playlistStr);
            if (playlistEventListener != null && dataSourceFactory != null && FlutterMediaPlugin.getInstance().getSimpleExoPlayer() != null)
                this.playlist = Playlist.fromJson(jsonObject, FlutterMediaPlugin.getInstance().getSimpleExoPlayer(), playlistEventListener, dataSourceFactory, playIndex);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void removeSong(Song song) {
        playlist.removeSong(song);
    }

    public void seekTo(long position) {
        if (FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() == Player.STATE_ENDED || FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getPlaybackState() == Player.STATE_IDLE) {
            return;
        }
        if (audioLength() > position) {
            FlutterMediaPlugin.getInstance().getSimpleExoPlayer().seekTo(position);
        }

        Log.d(TAG, "seek to " + position);
    }

//    private int requestAudioFocus() {
//        int res = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, // Music streaming
//                AudioManager.AUDIOFOCUS_GAIN);
//        return res;
//    }

    private long audioLength() {
        return FlutterMediaPlugin.getInstance().getSimpleExoPlayer().getDuration();
    }

//    private boolean isPlaying() {
//        return simpleExoPlayer.getPlayWhenReady();
//    }

//    private class AudioExoPlayerMediaListener implements MediaPlayerExoPlayerListenerManager.AudioExoPlayerListener {
//    }

//    @Override
//    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

//    }

    private class AudioExoPlayerListener implements EventListener {
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
            if (MediaPlayerNotificationService.getInstance() != null) {
                if (MediaPlayerNotificationService.getInstance().getPlayerNotificationManager() != null) {
                    if (playWhenReady) {
                        MediaPlayerNotificationService.getInstance().getPlayerNotificationManager().setOngoing(true);
                    } else {
                        MediaPlayerNotificationService.getInstance().getPlayerNotificationManager().setOngoing(false);
                    }
                }
            } else {
                if (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY) {
                    Log.d(TAG, "Media Notification is null" + playbackState);
                    showAudioPlayerNotification();
                }
            }


            if (playbackState == Player.STATE_ENDED && MediaPlayerNotificationService.getInstance() != null) {
                if (isShowingNotification) {
                    MediaPlayerNotificationService.getInstance().stopService(true);
                }
            }

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

