import 'package:flutter_media_plugin/playlist.dart';

class ExoPlayerListener {
  final Function(bool, int) _onPlayerStateChanged;
  final Function(int, int) _onPlaybackUpdate;
  final Function(int) _onBufferedUpdate;
  final Function(int) _onMediaPeriodCreated;
  final Function(String) _onPlayerStatus;
  final Function(Playlist) _onPlaylistChanged;

  ExoPlayerListener(
      {onPlayerStateChanged,
        onPlaybackUpdate,
        onBufferedUpdate,
        onMediaPeriodCreated,
        onPlayerStatus,
        onPlaylistChanged})
      : _onPlayerStateChanged = onPlayerStateChanged,
        _onPlaybackUpdate = onPlaybackUpdate,
        _onBufferedUpdate = onBufferedUpdate,
        _onMediaPeriodCreated = onMediaPeriodCreated,
        _onPlayerStatus = onPlayerStatus,
        _onPlaylistChanged = onPlaylistChanged;

//  void onTimelineChanged(Timeline timeline, Object manifest, int reason) {}

//  void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {}

//    void onLoadingChanged(boolean isLoading) {}

  void onPlayerStateChanged(bool playWhenReady, int playbackState) {
    if (_onPlayerStateChanged != null) {
      _onPlayerStateChanged(playWhenReady, playbackState);
    }
  }

//      void onRepeatModeChanged(int repeatMode){}

//      void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {}

//      void onPlayerError(ExoPlaybackException error){}

//      void onPositionDiscontinuity(int reason){}

//      void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}

//      void onSeekProcessed(){}

  void onPlaybackUpdate(int position, int audioLength) {
    if (_onPlaybackUpdate != null) {
      _onPlaybackUpdate(position, audioLength);
    }
  }

  void onBufferingUpdate(int percent) {
    if (_onBufferedUpdate != null) {
      _onBufferedUpdate(percent);
    }
  }

  void onMediaPeriodCreated(int windowIndex) {
    if (_onMediaPeriodCreated != null) {
      _onMediaPeriodCreated(windowIndex);
    }
  }

  void onPlayerStatus(String message) {
    if (_onPlayerStatus != null) {
      _onPlayerStatus(message);
    }
  }

  void onPlaylistChanged(Playlist playlist) {
    if (_onPlaylistChanged != null) {
      _onPlaylistChanged(playlist);
    }
  }
}