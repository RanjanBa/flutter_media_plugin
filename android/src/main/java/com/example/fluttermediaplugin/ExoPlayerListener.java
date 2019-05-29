package com.example.fluttermediaplugin;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

public interface ExoPlayerListener extends Player.EventListener {
    @Override
    void onTimelineChanged(Timeline timeline, Object manifest, int reason);

    @Override
    void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections);

    @Override
    void onLoadingChanged(boolean isLoading);

    @Override
    void onPlayerStateChanged(boolean playWhenReady, int playbackState);

    @Override
    void onRepeatModeChanged(int repeatMode);

    @Override
    void onShuffleModeEnabledChanged(boolean shuffleModeEnabled);

    @Override
    void onPlayerError(ExoPlaybackException error);

    @Override
    void onPositionDiscontinuity(int reason);

    @Override
    void onPlaybackParametersChanged(PlaybackParameters playbackParameters);

    @Override
    void onSeekProcessed();

//    void onPlaylistChanged(Playlist playlist);

    void onMediaPeriodCreated(int windowIndex);

    void onPlaybackUpdate(long position, long audioLength);

    void onBufferedUpdate(int percent);

    void onPlayerStatus(String message);
}
