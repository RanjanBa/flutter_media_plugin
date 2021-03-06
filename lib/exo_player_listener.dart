import 'package:flutter_media_plugin/media/media.dart';
import 'package:flutter_media_plugin/playlist.dart';

class ExoPlayerListener<T extends Media> {
  final Function(int windowIndex) _onMediaPeriodCreated;
  final Function(int windowIndex, int nextWindowIndex, T media) _onTracksChanged;
  final Function(bool) _onLoadingChanged;
  final Function(bool, int) _onPlayerStateChanged;
  final Function(int, int) _onPlaybackUpdate;
  final Function(int) _onBufferedUpdate;
  final Function(int, int) _onRepeatModeChanged;
  final Function(bool, int) _onShuffleModeEnabledChanged;
  final Function(Playlist<T>) _onPlaylistChanged;
  final Function(String, int, T) _onMediaAddedToPlaylist;
  final Function(String, int, T) _onMediaRemovedFromPlaylist;
  final Function(String) _onPlayerStatus;

  ExoPlayerListener({
    Function(int) onMediaPeriodCreated,
    Function(int, int, T) onTracksChanged,
    Function(bool) onLoadingChanged,
    Function(bool, int) onPlayerStateChanged,
    Function(int, int) onPlaybackUpdate,
    Function(int) onBufferedUpdate,
    Function(int, int) onRepeatModeChanged,
    Function(bool, int) onShuffleModeEnabledChanged,
    Function(Playlist<T>) onPlaylistChanged,
    Function(String, int, T) onMediaAddedToPlaylist,
    Function(String, int, T) onMediaRemovedFromPlaylist,
    Function(String) onPlayerStatus,
  })  : _onMediaPeriodCreated = onMediaPeriodCreated,
        _onTracksChanged = onTracksChanged,
        _onLoadingChanged = onLoadingChanged,
        _onPlayerStateChanged = onPlayerStateChanged,
        _onPlaybackUpdate = onPlaybackUpdate,
        _onBufferedUpdate = onBufferedUpdate,
        _onRepeatModeChanged = onRepeatModeChanged,
        _onShuffleModeEnabledChanged = onShuffleModeEnabledChanged,
        _onPlaylistChanged = onPlaylistChanged,
        _onMediaAddedToPlaylist = onMediaAddedToPlaylist,
        _onMediaRemovedFromPlaylist = onMediaRemovedFromPlaylist,
        _onPlayerStatus = onPlayerStatus;

//  void onTimelineChanged(Timeline timeline, Object manifest, int reason) {}

  void onTracksChanged(int windowIndex, int nextWindowIndex, T song) {
    if (_onTracksChanged != null) {
      _onTracksChanged(windowIndex, nextWindowIndex, song);
    }
  }

  void onMediaPeriodCreated(int windowIndex) {
    if (_onMediaPeriodCreated != null) {
      _onMediaPeriodCreated(windowIndex);
    }
  }

  void onLoadingChanged(bool isLoading) {
    if (_onLoadingChanged != null) {
      _onLoadingChanged(isLoading);
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

  void onRepeatModeChanged(int repeatMode, int nextWindowIndex) {
    if (_onRepeatModeChanged != null) {
      _onRepeatModeChanged(repeatMode, nextWindowIndex);
    }
  }

  void onShuffleModeEnabledChanged(bool shuffleModeEnabled, int nextWindowIndex) {
    if (_onShuffleModeEnabledChanged != null) {
      _onShuffleModeEnabledChanged(shuffleModeEnabled, nextWindowIndex);
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
    if (_onMediaRemovedFromPlaylist != null) {
      _onMediaRemovedFromPlaylist(playlistName, index, media);
    }
  }

  void onPlayerStatus(String message) {
    if (_onPlayerStatus != null) {
      _onPlayerStatus(message);
    }
  }

//  void onPlayerError(ExoPlaybackException error){}

//  void onPositionDiscontinuity(int reason) {}

//  void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}

//  void onSeekProcessed() {}
}
