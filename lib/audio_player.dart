import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_media_plugin/flutter_media_plugin.dart';
import 'package:flutter_media_plugin/exo_player_listener.dart';
import 'package:flutter_media_plugin/playlist.dart';
import 'dart:convert';

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

  int _currentIndex;
  Song _currentSong;
  Playlist<Song> _currentPlaylist;

  final Set<ExoPlayerListener<Song>> _exoPlayerListeners = Set();

  bool get playWhenReady => _playWhenReady;

  int get playbackState => _playbackState;

  int get repeatMode => _repeatMode;

  bool get shuffleModeEnabled => _shuffleModeEnabled;

  int get playbackPosition => _playbackPosition;

  int get playbackLength => _playbackLength;

  int get bufferingPercent => _bufferingPercent;

  int get currentPlayingSongIndex => _currentIndex;

  Song get currentPlayingSong => _currentSong;

  Playlist<Song> get currentPlayingPlaylist => _currentPlaylist;

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

        Map<String, dynamic> songMap =
            Map.from(arguments['currentPlayingSong']);
        if (songMap != null) {
          Song song = Song.fromMap(songMap);
          if (song != null) _currentSong = song;
        }
        break;
      case "onMediaPeriodCreated":
        Map<String, dynamic> songMap =
            Map.from(arguments["currentPlayingSong"]);
        if (songMap != null) {
          int windowIndex = arguments["windowIndex"];
          _currentIndex = windowIndex;
          Song song = Song.fromMap(songMap);
          _currentSong = song;
          for (ExoPlayerListener listener in _exoPlayerListeners) {
            listener.onMediaPeriodCreated(windowIndex, song);
          }
        } else {
          _currentSong = null;
        }
        break;
      case "onPlayerStateChanged":
        bool playWhenReady = arguments["playWhenReady"];
        int playbackState = arguments["playbackState"];
        _playWhenReady = playWhenReady;
        _playbackState = playbackState;
        if (_playbackState == Utility.STATE_IDLE) {
          _currentSong = null;
        }
        if (_playbackState == Utility.STATE_ENDED ||
            _playbackState == Utility.STATE_IDLE) {
          _playbackPosition = 0;
          _playbackLength = 0;
          pause();
        }

        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onPlayerStateChanged(playWhenReady, playbackState);
        }
        break;
      case "onPlaybackUpdate":
        int position = arguments['position'];
        int audioLength = arguments['audioLength'];
        _playbackLength = audioLength;
        _playbackPosition = position;
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onPlaybackUpdate(position, audioLength);
        }
        break;
      case "onBufferedUpdate":
        _bufferingPercent = arguments['percent'];
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onBufferingUpdate(_bufferingPercent);
        }
        break;
      case "onRepeatModeChanged":
        _repeatMode = arguments['repeatMode'];
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onRepeatModeChanged(_repeatMode);
        }
        break;
      case "onShuffleModeEnabledChanged":
        _shuffleModeEnabled = arguments['shuffleModeEnabled'];
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onShuffleModeEnabledChanged(_shuffleModeEnabled);
        }
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
        _currentPlaylist = playlist;
        break;
      case "onMediaAddedToPlaylist":
        String playlistName = arguments['playlistName'];
        int index = arguments['index'];
        Map<String, dynamic> songMap = Map.from(arguments['song']);
        Song song = Song.fromMap(songMap);
        if (_currentPlaylist.playlistName == playlistName) {
          _currentPlaylist.addMediaAtIndex(index, song);
          for (ExoPlayerListener listener in _exoPlayerListeners) {
            listener.onMediaAddedToPlaylist(playlistName, index, song);
          }
        } else {
          print("Audio Player: currentPlaylist name is not equal");
        }
        break;
      case "onMediaRemovedFromPlaylist":
        String playlistName = arguments['playlistName'];
        int index = arguments['index'];
        Map<String, dynamic> songMap = Map.from(arguments['song']);
        Song song = Song.fromMap(songMap);

        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onMediaRemovedFromPlaylist(playlistName, index, song);
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
