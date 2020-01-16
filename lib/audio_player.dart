import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_media_plugin/flutter_media_plugin.dart';
import 'package:flutter_media_plugin/exo_player_listener.dart';
import 'package:flutter_media_plugin/playlist.dart';
import 'package:flutter_media_plugin/media/song.dart';
import 'package:flutter_media_plugin/utility.dart';

class AudioPlayer {
  final String playerId;
  final MethodChannel channel;

  bool _playWhenReady = false;
  int _playbackState = 1;
  int _repeatMode = 0;
  bool _shuffleModeEnabled = false;

  int _playbackPosition = 0;
  int _playbackLength = 0;
  int _bufferingPercent = 0;

  int _currentWindowIndex = -1;
  int _nextWindowIndex = -1;

  Playlist<Song> _currentPlaylist;

  final Set<ExoPlayerListener<Song>> _exoPlayerListeners = Set();

  bool get playWhenReady => _playWhenReady;

  int get playbackState => _playbackState;

  int get repeatMode => _repeatMode;

  bool get shuffleModeEnabled => _shuffleModeEnabled;

  int get playbackPosition => _playbackPosition;

  int get playbackLength => _playbackLength;

  int get bufferingPercent => _bufferingPercent;

  int get currentWindowIndex => _currentWindowIndex;

  int get nextWindowIndex => _nextWindowIndex;

  Song get currentPlayingSong {
    if (_currentPlaylist == null) {
      return null;
    }
    return _currentPlaylist.getMediaAtIndex(_currentWindowIndex);
  }

  int get playlistSize {
    if (_currentPlaylist == null) {
      return -1;
    }
    return _currentPlaylist.getSize();
  }

  String get playlistName {
    if (_currentPlaylist == null) {
      return "";
    }

    return _currentPlaylist.playlistName;
  }

  Song getSongAtIndex(int index) {
    if (_currentPlaylist == null) {
      return null;
    }
    return _currentPlaylist.getMediaAtIndex(index);
  }

  AudioPlayer({this.playerId, this.channel}) {
    _initialize();
  }

  void _initialize() {
    channel.invokeMethod('${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/initialize');
  }

  void callMethod(String method, dynamic arguments) {
//    print('Audio player method: $method');
    switch (method) {
      case "onInitialized":
        _playWhenReady = arguments['playWhenReady'];
        _playbackState = arguments['playbackState'];
        _repeatMode = arguments['repeatMode'];
        _shuffleModeEnabled = arguments['shuffleModeEnabled'];
        _currentWindowIndex = arguments['windowIndex'];
        _nextWindowIndex = arguments['nextWindowIndex'];

        String playlistString = arguments['playlist'];
        if (playlistString != null) {
          Map<String, dynamic> map = json.decode(playlistString);
          Playlist<Song> playlist = Playlist.songsPlaylistFromMap(map);
          _currentPlaylist = playlist;
        } else {
          _currentWindowIndex = -1;
          _nextWindowIndex = -1;
        }
        break;
      case "onMediaPeriodCreated":
        int windowIndex = arguments['windowIndex'];
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onMediaPeriodCreated(windowIndex);
        }
        break;
      case "onTracksChanged":
        Map<String, dynamic> songMap = Map.from(arguments['playingSong']);
        int windowIndex = arguments['windowIndex'];
        int nextWindowIndex = arguments['nextWindowIndex'];
        Song song = Song.fromMap(songMap);

        if (currentPlayingSong != null) {
          if (windowIndex == _currentWindowIndex &&
              currentPlayingSong.key == song.key) {
            return;
          }
        }

        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onTracksChanged(windowIndex, nextWindowIndex, song);
        }

        _currentWindowIndex = windowIndex;
        _nextWindowIndex = nextWindowIndex;
//        print("Audio Player: onTracksChanged windowIndex $windowIndex");
        break;
      case "onPlayerStateChanged":
        bool playWhenReady = arguments['playWhenReady'];
        int playbackState = arguments['playbackState'];

        if (_playbackState == ExoPlayerUtility.STATE_ENDED ||
            _playbackState == ExoPlayerUtility.STATE_IDLE) {
          _playbackPosition = 0;
          _playbackLength = 0;

          for (ExoPlayerListener listener in _exoPlayerListeners) {
            listener.onPlaybackUpdate(_playbackPosition, _playbackLength);
          }
          pause();
        }

        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onPlayerStateChanged(playWhenReady, playbackState);
        }

        if (_playbackState == ExoPlayerUtility.STATE_IDLE) {
          _currentWindowIndex = -1;
          _nextWindowIndex = -1;
        }

        _playWhenReady = playWhenReady;
        _playbackState = playbackState;
        break;
      case "onPlaybackUpdate":
        int position = arguments['position'];
        int audioLength = arguments['audioLength'];
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onPlaybackUpdate(position, audioLength);
        }

        _playbackPosition = position;
        _playbackLength = audioLength;
        break;
      case "onBufferedUpdate":
        int percent = arguments['percent'];
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onBufferingUpdate(percent);
        }
        _bufferingPercent = percent;
        break;
      case "onRepeatModeChanged":
        int repeatMode = arguments['repeatMode'];
        int nextWindowIndex = arguments['nextWindowIndex'];

        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onRepeatModeChanged(repeatMode, nextWindowIndex);
        }

        _repeatMode = repeatMode;
        _nextWindowIndex = nextWindowIndex;
        break;
      case "onShuffleModeEnabledChanged":
        bool shuffleModeEnabled = arguments['shuffleModeEnabled'];
        int nextWindowIndex = arguments['nextWindowIndex'];

        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onShuffleModeEnabledChanged(shuffleModeEnabled, nextWindowIndex);
        }

        _shuffleModeEnabled = shuffleModeEnabled;
        _nextWindowIndex = nextWindowIndex;
        break;
      case "onPlayerStatus":
        String message = arguments['message'];
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onPlayerStatus(message);
        }
        break;
      case "onPlaylistChanged":
        String playlistString = arguments['playlist'];
        Map<String, dynamic> map = json.decode(playlistString);
        Playlist<Song> playlist = Playlist.songsPlaylistFromMap(map);
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onPlaylistChanged(playlist);
        }
        if (playlist.getSize() <= 0) {
          _currentPlaylist = null;
          _currentWindowIndex = -1;
          _nextWindowIndex = -1;
        } else {
          _currentPlaylist = playlist;
        }
        break;
      case "onMediaAddedToPlaylist":
        String playlistName = arguments['playlistName'];
        int index = arguments['index'];
        Map<String, dynamic> songMap = Map.from(arguments['song']);
        Song song = Song.fromMap(songMap);
        if (_currentPlaylist.playlistName == playlistName) {
          for (ExoPlayerListener listener in _exoPlayerListeners) {
            listener.onMediaAddedToPlaylist(playlistName, index, song);
          }
          _currentPlaylist.addMediaAtIndex(index, song);
        } else {
          print("Audio Player: currentPlaylist name is not equal");
        }
        break;
      case "onMediaRemovedFromPlaylist":
        String playlistName = arguments['playlistName'];
        int index = arguments['index'];
        Map<String, dynamic> songMap = Map.from(arguments['song']);
        Song song = Song.fromMap(songMap);
        if (_currentPlaylist.playlistName == playlistName) {
          for (ExoPlayerListener listener in _exoPlayerListeners) {
            listener.onMediaRemovedFromPlaylist(playlistName, index, song);
          }
          _currentPlaylist.removeMediaAtIndex(index);
        } else {
          print("Audio Player: currentPlaylist name is not equal");
        }
        break;
      default:
        print("Audio Method is not implemented");
        break;
    }
  }

  void addExoPlayerListener(ExoPlayerListener exoPlayerListener) {
    _exoPlayerListeners.add(exoPlayerListener);
  }

  void removeExoPlayerListener(ExoPlayerListener exoPlayerListener) {
    _exoPlayerListeners.remove(exoPlayerListener);
  }

  void play() {
    channel.invokeMethod('${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/play');
  }

  void pause() {
    channel.invokeMethod("${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/pause");
  }

  void seekTo(int position) {
    channel.invokeMethod(
      "${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/seekTo",
      {
        'position': position,
      },
    );
  }

  void skipToNext() {
    channel.invokeMethod('${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/skipToNext');
  }

  void skipToPrevious() {
    channel
        .invokeMethod('${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/skipToPrevious');
  }

  void skipToIndex(int index) {
    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/skipToIndex',
      {
        'index': index,
      },
    );
  }

  void setRepeatMode(int repeatMode) {
    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/setRepeatMode',
      {
        'repeatMode': repeatMode,
      },
    );
  }

  void setShuffleModeEnabled(bool shuffleModeEnabled) {
    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/setShuffleModeEnabled',
      {
        'shuffleModeEnabled': shuffleModeEnabled,
      },
    );
  }

  void stop() {
    channel.invokeMethod("${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/stop");
  }

  void release() {
    channel.invokeMethod('${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/release');
  }

  Future<bool> playNext(Song song) async {
    Map<String, dynamic> songMap = song.toJson();
    if (songMap == null) {
      return false;
    }
    return channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/playNext',
      songMap,
    );
  }

// add song at last position
  Future<bool> addSong(Song song, {bool shouldPlay = false}) async {
    if (_currentPlaylist == null) {
      return false;
    }
    return await addSongAtIndex(_currentPlaylist.getSize(), song,
        shouldPlay: shouldPlay);
  }

  Future<bool> addSongAtIndex(int index, Song song,
      {bool shouldPlay = false}) async {
    Map<String, dynamic> songMap = song.toJson();
    if (songMap == null) {
      return false;
    }

    songMap.putIfAbsent('index', () => index);
    songMap.putIfAbsent("shouldPlay", () => shouldPlay ? 1 : 0);
    return await channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/addSongAtIndex',
      songMap,
    );
  }

  Future<bool> removeSongFromIndex(Song song, index) async {
    Map<String, dynamic> songMap = song.toJson();
    if (songMap == null) {
      return false;
    }
    songMap.putIfAbsent("index", () => index);
    return await channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/removeSongFromIndex',
      songMap,
    );
  }

  Future<void> setPlaylist(Playlist<Song> playlist) async {
    if (playlist == null) {
      return false;
    }
    String playlistString = json.encode(playlist);
    return await channel.invokeMethod(
      "${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/setPlaylist",
      {
        "playlist": playlistString,
      },
    );
  }
}
