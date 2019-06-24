class ExoPlayerListener {
  final Function(bool, int) _onPlayerStateChanged;
  final Function(int, int) _onPlaybackUpdate;
  final Function(int) _onBufferedUpdate;
  final Function(int) _onMediaPeriodCreated;
  final Function(String) _onPlayerStatus;
  final Function(int) _onRepeatModeChanged;
  final Function(bool) _onShuffleModeEnabledChanged;

  ExoPlayerListener(
      {onPlayerStateChanged,
        onPlaybackUpdate,
        onBufferedUpdate,
        onMediaPeriodCreated,
        onPlayerStatus,
        onRepeatModeChanged,
        onShuffleModeEnabledChanged})
      : _onPlayerStateChanged = onPlayerStateChanged,
        _onPlaybackUpdate = onPlaybackUpdate,
        _onBufferedUpdate = onBufferedUpdate,
        _onMediaPeriodCreated = onMediaPeriodCreated,
        _onPlayerStatus = onPlayerStatus,
        _onRepeatModeChanged = onRepeatModeChanged,
        _onShuffleModeEnabledChanged = onShuffleModeEnabledChanged;

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

  void onRepeatModeChanged(int repeatMode) {
    if (_onRepeatModeChanged != null) {
      _onRepeatModeChanged(repeatMode);
    }
  }

  void onShuffleModeEnabledChanged(bool shuffleModeEnabled) {
    if(_onShuffleModeEnabledChanged != null) {
      _onShuffleModeEnabledChanged(shuffleModeEnabled);
    }
  }
}