import 'package:flutter_media_plugin/media/media.dart';
import 'package:flutter_media_plugin/playlist.dart';

class ExoPlayerListener<T extends Media> {
  final Function(bool) _onLoadingChanged;
  final Function(bool, int) _onPlayerStateChanged;
  final Function(int, int) _onPlaybackUpdate;
  final Function(int) _onBufferedUpdate;
  final Function(int, T) _onMediaPeriodCreated;
  final Function(int) _onRepeatModeChanged;
  final Function(bool) _onShuffleModeEnabledChanged;
  final Function(String) _onPlayerStatus;
  final Function(Playlist<T>) _onPlaylistChanged;
  final Function(String, int, T) _onMediaAddedToPlaylist;
  final Function(String, int, T) _onSongRemovedFromPlaylist;

  ExoPlayerListener({
    onLoadingChanged,
    onPlayerStateChanged,
    onPlaybackUpdate,
    onBufferedUpdate,
    onMediaPeriodCreated,
    onRepeatModeChanged,
    onShuffleModeEnabledChanged,
    onPlayerStatus,
    onPlaylistChanged,
    onMediaAddedToPlaylist,
    onSongRemovedFromPlaylist,
  })  : _onLoadingChanged = onLoadingChanged,
        _onPlayerStateChanged = onPlayerStateChanged,
        _onPlaybackUpdate = onPlaybackUpdate,
        _onBufferedUpdate = onBufferedUpdate,
        _onMediaPeriodCreated = onMediaPeriodCreated,
        _onPlayerStatus = onPlayerStatus,
        _onRepeatModeChanged = onRepeatModeChanged,
        _onShuffleModeEnabledChanged = onShuffleModeEnabledChanged,
        _onPlaylistChanged = onPlaylistChanged,
        _onMediaAddedToPlaylist = onMediaAddedToPlaylist,
        _onSongRemovedFromPlaylist = onSongRemovedFromPlaylist;

//  void onTimelineChanged(Timeline timeline, Object manifest, int reason) {}

//  void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {}

  void onLoadingChanged(bool isLoading) {
    if (_onLoadingChanged != null) {
      _onLoadingChanged(isLoading);
    }
  }

  void onMediaPeriodCreated(int windowIndex, T media) {
    if (_onMediaPeriodCreated != null) {
      _onMediaPeriodCreated(windowIndex, media);
    }
  }

  void onPlayerStateChanged(bool playWhenReady, int playbackState) {
    if (_onPlayerStateChanged != null) {
      _onPlayerStateChanged(playWhenReady, playbackState);
    }
  }

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

  void onRepeatModeChanged(int repeatMode) {
    if (_onRepeatModeChanged != null) {
      _onRepeatModeChanged(repeatMode);
    }
  }

  void onShuffleModeEnabledChanged(bool shuffleModeEnabled) {
    if (_onShuffleModeEnabledChanged != null) {
      _onShuffleModeEnabledChanged(shuffleModeEnabled);
    }
  }

  void onPlayerStatus(String message) {
    if (_onPlayerStatus != null) {
      _onPlayerStatus(message);
    }
  }

  void onPlaylistChanged(Playlist<T> playlist) {
    if (_onPlaylistChanged != null) {
      _onPlaylistChanged(playlist);
    }
  }

  void onMediaAddedToPlaylist(String playlistName, int index, T media) {
    if (_onMediaAddedToPlaylist != null) {
      _onMediaAddedToPlaylist(playlistName, index, media);
    }
  }

  void onMediaRemovedFromPlaylist(String playlistName, int index, T media) {
    if (_onSongRemovedFromPlaylist != null) {
      _onSongRemovedFromPlaylist(playlistName, index, media);
    }
  }

//  void onPlayerError(ExoPlaybackException error){}

//  void onPositionDiscontinuity(int reason) {}

//  void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}

//  void onSeekProcessed() {}
}
