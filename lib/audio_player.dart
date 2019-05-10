import 'package:flutter/services.dart';
import 'package:flutter_media_plugin/flutter_media_plugin.dart';
import 'package:flutter_media_plugin/exo_player_listener.dart';
import 'package:flutter_media_plugin/playlist.dart';
import 'package:meta/meta.dart';
import 'dart:convert';

class AudioPlayer {
  final String playerId;
  final MethodChannel channel;

  final RegExp _regExp = new RegExp(r"([^/]+)/([^/]+)");

  bool _playWhenReady = false;
  int _playbackState = 1;
  int _playbackPosition = 0;
  int _playbackLength = 0;
  int _bufferingPercent = 0;

  final Set<ExoPlayerListener> _exoPlayerListeners = Set();
  Playlist _playlist = new Playlist("currentPlaylist");
  int _windowIndex = -1;

  bool get playWhenReady => _playWhenReady;

  int get playbackState => _playbackState;

  int get playbackPosition => _playbackPosition;

  int get playbackLength => _playbackLength;

  int get bufferingPercent => _bufferingPercent;

  Playlist get playlist => _playlist;

  int get windowIndex => _windowIndex;

  AudioPlayer({this.playerId, this.channel}) {
    initialize();
  }

  void callMethod(String method, dynamic arguments) {
    switch (method) {
      case "onMediaPeriodCreated":
        int windowIndex = arguments["windowIndex"];
        print("onMediaPeriodCreated $windowIndex");
        _windowIndex = windowIndex;
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onMediaPeriodCreated(windowIndex);
        }
        break;
      case "onPlayerStateChanged":
        bool playWhenReady = arguments["playWhenReady"];
        int playbackState = arguments["playbackState"];
        //print("onPlayerStateChanged, $playWhenReady $playbackState");
        _playWhenReady = playWhenReady;
        _playbackState = playbackState;
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onPlayerStateChanged(playWhenReady, playbackState);
        }
        break;
      case "onPlaybackUpdate":
        int position = arguments['position'];
        int audioLength = arguments['audioLength'];
        _playbackLength = audioLength;
        _playbackPosition = position;
        //print("flutter : ${call.method} $position");
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
      case "onPlaylistChanged":
        String str = arguments['playlist'];
        Map<String, dynamic> map = json.decode(str);
        Playlist newPlaylist = Playlist.fromJson(map);
        // print("playlist Name : " +
        //     newPlaylist._playlistName +
        //     ", songs : ${newPlaylist.getSize()}");
        _playlist = newPlaylist;
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onPlaylistChanged(newPlaylist);
        }
        break;
      case "onPlayerStatus":
        String message = arguments['message'];
        for (ExoPlayerListener listener in _exoPlayerListeners) {
          listener.onPlayerStatus(message);
        }
        break;
      default:
        break;
    }
  }

  void initialize() {
    channel.invokeMethod('${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/initialize');
  }

  void addExoPlayerListener(ExoPlayerListener exoPlayerListener) {
    _exoPlayerListeners.add(exoPlayerListener);
  }

  void removeExoPlayerListener(ExoPlayerListener exoPlayerListener) {
    _exoPlayerListeners.remove(exoPlayerListener);
  }

  void play() {
    channel.invokeMethod('${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/play');
  }

  void pause() {
    channel.invokeMethod("${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/pause");
  }

  void stop() {
    channel.invokeMethod("${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/stop");
  }

  void addAndPlay(Song song) {
    channel.invokeMethod(
      "${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/addAndPlay",
      {
        C.song_key_tag: song.key,
        C.song_title_tag: song.title,
        C.song_artist_tag: song.artist,
        C.song_album_tag: song.album,
        C.song_album_art_uri_tag: song.album_art_uri,
        C.song_uri_tag: song.uri,
      },
    );
  }

  void addSong(Song song) {
    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/addSong',
      {
        C.song_key_tag: song.key,
        C.song_title_tag: song.title,
        C.song_artist_tag: song.artist,
        C.song_album_tag: song.album,
        C.song_album_art_uri_tag: song.album_art_uri,
        C.song_uri_tag: song.uri,
      },
    );
  }

  void addSongAtIndex(int index, Song song) {
    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/addSongAtIndex',
      {
        'index': index,
        C.song_key_tag: song.key,
        C.song_title_tag: song.title,
        C.song_artist_tag: song.artist,
        C.song_album_tag: song.album,
        C.song_album_art_uri_tag: song.album_art_uri,
        C.song_uri_tag: song.uri,
      },
    );
  }

  void setPlaylistAndSongIndex(Playlist playlist, int playIndex) {
    String s = json.encode(playlist);
    channel.invokeMethod(
      "${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/setPlaylistAndSongIndex",
      {
        "playlist": s,
        "playIndex": playIndex,
      },
    );
  }

  void removeSong(Song song) {
    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/removeSong',
      {
        C.song_key_tag: song.key,
        C.song_title_tag: song.title,
        C.song_artist_tag: song.artist,
        C.song_album_tag: song.album,
        C.song_album_art_uri_tag: song.album_art_uri,
        C.song_uri_tag: song.uri,
      },
    );
  }

  void seekTo(int position) {
    channel.invokeMethod(
      "${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/seekTo",
      {
        'position': position,
      },
    );
  }

  void setRepeatMode(int repeatMode) {
    if (repeatMode >= 0 && repeatMode <= 2) {
      channel.invokeMethod(
        '${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/setRepeatMode',
        {
          'repeatMode': repeatMode,
        },
      );
    }
  }

  void skipToNext() {
    channel.invokeMethod('${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/skipToNext');
  }

  void skipToPrevious() {
    channel.invokeMethod('${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/skipToPrevious');
  }

  void skipToIndex(int index) {
    channel.invokeMethod(
      '${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/skipToIndex',
      {
        'index': index,
      },
    );
  }

  void clearPlaylist() {
    channel.invokeMethod('${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/clearPlaylist');
  }

  void release() {
    channel.invokeMethod('${FlutterMediaPlugin.AUDIO_MEDIA_TYPE}/release');
  }
}

class Song {
  final String key;
  final String title;
  final String artist;
  final String album;
  final String album_art_uri; // ignore: non_constant_identifier_names
  final String uri;

  Song({
    @required this.key,
    @required this.title,
    @required this.artist,
    @required this.album,
    @required this.album_art_uri, // ignore: non_constant_identifier_names
    @required this.uri,
  });

  Map<String, dynamic> toJson() {
    return {
      C.song_key_tag: key,
      C.song_title_tag: title,
      C.song_artist_tag: artist,
      C.song_album_tag: album,
      C.song_album_art_uri_tag: album_art_uri,
      C.song_uri_tag: uri,
    };
  }

  factory Song.fromJson(Map<String, dynamic> map) {
    String key = map[C.song_key_tag];
    String title = map[C.song_title_tag];
    String artist = map[C.song_artist_tag];
    String album = map[C.song_album_tag];
    String albumArtUri = map[C.song_album_art_uri_tag];
    String uri = map[C.song_uri_tag];
    Song song = Song(
        key: key,
        title: title,
        artist: artist,
        album: album,
        album_art_uri: albumArtUri,
        uri: uri);
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
  static String song_artist_tag = "artist";

  // ignore: non_constant_identifier_names
  static String song_album_tag = "album";

  // ignore: non_constant_identifier_names
  static String song_album_art_uri_tag = "album_art_uri";

  // ignore: non_constant_identifier_names
  static String song_uri_tag = "uri";

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
