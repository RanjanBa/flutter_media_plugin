import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_media_plugin/flutter_media_plugin.dart';
import 'package:flutter_media_plugin/exo_player_listener.dart';
import 'package:flutter_media_plugin/playlist.dart';
import 'package:meta/meta.dart';
import 'dart:convert';

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

      Map<dynamic, dynamic> songArgs = arguments["currentPlayingSong"];
      if (songArgs != null) {
        Map<String, dynamic> songMap = new Map();
        songMap[C.song_key_tag] = songArgs[C.song_key_tag].toString();
        songMap[C.song_title_tag] = songArgs[C.song_title_tag].toString();
        songMap[C.song_artists_tag] = songArgs[C.song_artists_tag].toString();
        songMap[C.song_album_tag] = songArgs[C.song_album_tag].toString();
        songMap[C.song_album_art_url_tag] =
            songArgs[C.song_album_art_url_tag].toString();
        songMap[C.song_url_tag] = songArgs[C.song_url_tag].toString();

        Song song = Song.fromMap(songMap);
        if (song != null) _currentPlayingSong = song;
      }

//      print("playWhenReady $_playWhenReady; playbackState $_playbackState");
    }
  }

  void callMethod(String method, dynamic arguments) {
    switch (method) {
      case "onMediaPeriodCreated":
        int windowIndex = arguments["windowIndex"];
        Map<String, dynamic> songMap = new Map();
        songMap[C.song_key_tag] =
            arguments["currentPlayingSong"][C.song_key_tag].toString();
        songMap[C.song_title_tag] =
            arguments["currentPlayingSong"][C.song_title_tag].toString();
        songMap[C.song_artists_tag] =
            arguments["currentPlayingSong"][C.song_artists_tag].toString();
        songMap[C.song_album_tag] =
            arguments["currentPlayingSong"][C.song_album_tag].toString();
        songMap[C.song_album_art_url_tag] = arguments["currentPlayingSong"]
                [C.song_album_art_url_tag]
            .toString();
        songMap[C.song_url_tag] =
            arguments["currentPlayingSong"][C.song_url_tag].toString();

        Song song = Song.fromMap(songMap);
        if (song != null) _currentPlayingSong = song;

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
        if (_playbackState == C.STATE_IDLE) {
          _currentPlayingSong = null;
        }
        if (_playbackState == C.STATE_ENDED || _playbackState == C.STATE_IDLE) {
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

  void addAndPlay(Song song) {
    channel.invokeMethod(
      "${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/addAndPlay",
      {
        C.song_key_tag: song.key,
        C.song_title_tag: song.title,
        C.song_artists_tag: song.artists,
        C.song_album_tag: song.album,
        C.song_album_art_url_tag: song.album_art_url,
        C.song_url_tag: song.url,
      },
    );
  }

  void addSong(Song song) {
    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/addSong',
      {
        C.song_key_tag: song.key,
        C.song_title_tag: song.title,
        C.song_artists_tag: song.artists,
        C.song_album_tag: song.album,
        C.song_album_art_url_tag: song.album_art_url,
        C.song_url_tag: song.url,
      },
    );
  }

  void playNext(Song song) {
    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/playNext',
      {
        C.song_key_tag: song.key,
        C.song_title_tag: song.title,
        C.song_artists_tag: song.artists,
        C.song_album_tag: song.album,
        C.song_album_art_url_tag: song.album_art_url,
        C.song_url_tag: song.url,
      },
    );
  }

  void addSongAtIndex(int index, Song song) {
    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/addSongAtIndex',
      {
        'index': index,
        C.song_key_tag: song.key,
        C.song_title_tag: song.title,
        C.song_artists_tag: song.artists,
        C.song_album_tag: song.album,
        C.song_album_art_url_tag: song.album_art_url,
        C.song_url_tag: song.url,
      },
    );
  }

  Future<void> setPlaylist(Playlist playlist) async {
    String s = json.encode(playlist);
    await channel.invokeMethod(
      "${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/setPlaylist",
      {
        "playlist": s,
      },
    );
  }

  Future<Playlist> getPlaylist() async {
    String s = await channel
        .invokeMethod("${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/getPlaylist");
    Map<String, dynamic> map = json.decode(s);
    Playlist playlist = Playlist.fromMap(map);
    return playlist;
  }

  void removeSong(Song song) {
    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/removeSong',
      {
        C.song_key_tag: song.key,
        C.song_title_tag: song.title,
        C.song_artists_tag: song.artists,
        C.song_album_tag: song.album,
        C.song_album_art_url_tag: song.album_art_url,
        C.song_url_tag: song.url,
      },
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
    if (repeatMode >= 0 && repeatMode <= 2) {
      channel.invokeMethod(
        '${FlutterMediaPlugin.AUDIO_METHOD_TYPE}/setRepeatMode',
        {
          'repeatMode': repeatMode,
        },
      );
    }
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

class Song {
  final String key;
  final String title;
  final String artists;
  final String album;
  // ignore: non_constant_identifier_names
  final String album_art_url;
  final String url;

  Song({
    @required this.key,
    @required this.title,
    @required this.artists,
    @required this.album,
    @required this.album_art_url,
    @required this.url,
  });

  Map<String, dynamic> toJson() {
    return {
      C.song_key_tag: key,
      C.song_title_tag: title,
      C.song_artists_tag: artists,
      C.song_album_tag: album,
      C.song_album_art_url_tag: album_art_url,
      C.song_url_tag: url,
    };
  }

  factory Song.fromMap(Map<String, dynamic> map) {
    String key = map[C.song_key_tag];
    String title = map[C.song_title_tag];
    String artists = map[C.song_artists_tag];
    String album = map[C.song_album_tag];
    String albumArtUrl = map[C.song_album_art_url_tag];
    String url = map[C.song_url_tag];
    Song song = Song(
        key: key,
        title: title,
        artists: artists,
        album: album,
        album_art_url: albumArtUrl,
        url: url);
    return song;
  }
}

class C {
  // ignore: non_constant_identifier_names
  static String playlist_name = "playlistName";

  // ignore: non_constant_identifier_names
  static String song_key_tag = "key";

  // ignore: non_constant_identifier_names
  static String song_title_tag = "title";

  // ignore: non_constant_identifier_names
  static String song_artists_tag = "artists";

  // ignore: non_constant_identifier_names
  static String song_album_tag = "album";

  // ignore: non_constant_identifier_names
  static String song_album_art_url_tag = "artUrl";

  // ignore: non_constant_identifier_names
  static String song_url_tag = "url";

  // ignore: non_constant_identifier_names
  static int REPEAT_MODE_OFF = 0;

  /// "Repeat One" mode to repeat the currently playing window infinitely.
  // ignore: non_constant_identifier_names
  static int REPEAT_MODE_ONE = 1;

  /// "Repeat All" mode to repeat the entire timeline infinitely.
  // ignore: non_constant_identifier_names
  static int REPEAT_MODE_ALL = 2;

  // ignore: non_constant_identifier_names
  static int STATE_IDLE = 1;

  /// The player is not able to immediately play from its current position. This state typically
  /// occurs when more data needs to be loaded.
  // ignore: non_constant_identifier_names
  static int STATE_BUFFERING = 2;

  /// The player is able to immediately play from its current position. The player will be playing if
  /// {@link #getPlayWhenReady()} is true, and paused otherwise.
  // ignore: non_constant_identifier_names
  static int STATE_READY = 3;

  /// The player has finished playing the media.
  // ignore: non_constant_identifier_names
  static int STATE_ENDED = 4;
}
