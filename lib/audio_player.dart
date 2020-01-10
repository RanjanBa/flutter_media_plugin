import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_media_plugin/flutter_media_plugin.dart';
import 'package:flutter_media_plugin/exo_player_listener.dart';
import 'package:flutter_media_plugin/playlist.dart';
import 'dart:convert';

import 'package:flutter_media_plugin/song.dart';
import 'package:flutter_media_plugin/utility.dart';

class AudioPlayer {
  final String playerId;
  final MethodChannel channel;

  bool _playWhenReady = false;
  int _playbackState = 1;
  int _playbackPosition = 0;
  int _playbackLength = 0;
  int _bufferingPercent = 0;
  Song _currentPlayingSong;

  final Set<ExoPlayerListener> _exoPlayerListeners = Set();

  bool get playWhenReady => _playWhenReady;

  int get playbackState => _playbackState;

  int get playbackPosition => _playbackPosition;

  int get playbackLength => _playbackLength;

  int get bufferingPercent => _bufferingPercent;

  Song get currentPlayingSong => _currentPlayingSong;

  AudioPlayer({this.playerId, this.channel}) {
    _initialize();
  }

  void _initialize() async {
    Map<dynamic, dynamic> arguments = await channel
        .invokeMethod('${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/initialize');
    print("initialize ${arguments.runtimeType}");
    if (arguments != null) {
      _playWhenReady = arguments["playWhenReady"];
      _playbackState = arguments["playbackState"];

      Map<String, dynamic> songMap = Map.from(arguments["currentPlayingSong"]);
      if (songMap != null) {
        Song song = Song.fromMap(songMap);
        if (song != null) _currentPlayingSong = song;
      }
    }
  }

  void callMethod(String method, dynamic arguments) {
    switch (method) {
      case "onMediaPeriodCreated":
        int windowIndex = arguments["windowIndex"];
//        print('mediaPeriod created ${arguments["currentPlayingSong"].runtimeType}');
        Map<String, dynamic> songMap = Map.from(arguments["currentPlayingSong"]);
        if (songMap != null) {
          Song song = Song.fromMap(songMap);
          _currentPlayingSong = song;
        }

        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onMediaPeriodCreated(windowIndex);
        }
        break;
      case "onPlayerStateChanged":
        bool playWhenReady = arguments["playWhenReady"];
        int playbackState = arguments["playbackState"];
//        print("onPlayerStateChanged, $playWhenReady $playbackState");
        _playWhenReady = playWhenReady;
        _playbackState = playbackState;
        if (_playbackState == Utility.STATE_IDLE) {
          _currentPlayingSong = null;
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
//        print("flutter : update playback $position");
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onPlaybackUpdate(position, audioLength);
        }
        break;
      case "onBufferedUpdate":
        int percent = arguments['percent'];
        //print("flutter : ${call.method} $percent");
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onBufferingUpdate(percent);
        }
        break;
      case "onPlayerStatus":
        String message = arguments['message'];
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onPlayerStatus(message);
        }
        break;
      case "onRepeatModeChanged":
        int repeatMode = arguments['repeatMode'];
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onRepeatModeChanged(repeatMode);
        }
        break;
      case "onShuffleModeEnabledChanged":
        bool shuffleModeEnabled = arguments['shuffleModeEnabled'];
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onShuffleModeEnabledChanged(shuffleModeEnabled);
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

  void stop() {
    channel.invokeMethod("${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/stop");
  }

  void playNext(Song song) {
    Map<String, String> songMap = song.toJson();
    if (songMap == null) {
      return;
    }
    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/playNext',
      songMap,
    );
  }

  Future<void> addSong(Song song, {bool shouldPlay = false}) async{
    Map<String, dynamic> songMap = song.toJson();
    if (songMap == null) {
      return;
    }

    songMap.putIfAbsent("shouldPlay", () => shouldPlay);
    await channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/addSong',
      songMap,
    );
  }

  Future<void> addSongAtIndex(int index, Song song) async {
    Map<String, dynamic> songMap = song.toJson();
    if (songMap == null) {
      return;
    }

    songMap.putIfAbsent('index', () => index);
    await channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/addSongAtIndex',
      songMap,
    );
  }

  Future<void> setPlaylist(Playlist playlist) async {
    String playlistString = json.encode(playlist);
    await channel.invokeMethod(
      "${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/setPlaylist",
      {
        "playlist": playlistString,
      },
    );
  }

  Future<Playlist> getPlaylist() async {
    String playlistString = await channel
        .invokeMethod("${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/getPlaylist");
    Map<String, dynamic> map = json.decode(playlistString);
    Playlist playlist = Playlist.fromMap(map);
    return playlist;
  }

  void removeSong(Song song) {
    Map<String, dynamic> songMap = song.toJson();
    if (songMap == null) {
      return;
    }

    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/removeSong',
      songMap,
    );
  }

  void seekTo(int position) {
    channel.invokeMethod(
      "${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/seekTo",
      {
        'position': position,
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

  Future<int> getRepeatMode() async {
    int mode = await channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/getRepeatMode',
    );
    return mode;
  }

  void setShuffleModeEnabled(bool shuffleModeEnabled) {
    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/setShuffleModeEnabled',
      {
        'shuffleModeEnabled': shuffleModeEnabled,
      },
    );
  }

  Future<bool> getShuffleModeEnabled() async {
    bool shuffleModeEnabled = await channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/getShuffleModeEnabled',
    );
    return shuffleModeEnabled;
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

  Future<void> clearPlaylist() async {
    await channel
        .invokeMethod('${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/clearPlaylist');
  }

  void release() {
    channel.invokeMethod('${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/release');
  }
}
