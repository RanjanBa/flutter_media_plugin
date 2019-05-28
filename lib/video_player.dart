import 'package:flutter/services.dart';
import 'package:flutter_media_plugin/exo_player_listener.dart';
import 'package:flutter_media_plugin/flutter_media_plugin.dart';

class VideoPlayer {
  final String playerId;
  final MethodChannel channel;

  int _textureId;
  int _width;
  int _height;
  int _duration;

  int get textureId => _textureId;
  final Set<VideoExoPlayerListener> _exoPlayerListeners = Set();

  double get aspectRatio =>
      _width != null && _height != null && _height > 0 ? _width / _height : 1.0;

  int get duration => _duration;

  VideoPlayer({this.playerId, this.channel}) {
    channel.invokeMethod('${FlutterMediaPlugin.VIDEO_MEDIA_TYPE}/initialize');
  }

  void callMethod(String method, dynamic arguments) {
    switch (method) {
      case "videoInitialize":
        int textureId = arguments["textureId"];
        _width = arguments['width'];
        _height = arguments['height'];
        _duration = arguments['duration'];
        print("textureId $textureId");
        for (VideoExoPlayerListener listener in _exoPlayerListeners) {
          if (listener.onVideoInitialize != null) {
            listener.onVideoInitialize(textureId);
          }
        }
        break;
    }
  }

  void addVideoExoPlayer(VideoExoPlayerListener videoExoPlayerListener) {
    _exoPlayerListeners.add(videoExoPlayerListener);
  }

  void removeVideoExoPlayer(VideoExoPlayerListener videoExoPlayerListener) {
    _exoPlayerListeners.remove(videoExoPlayerListener);
  }

  void addAndPlay(TypeOfPlace type, String uri) {
    String path = type == TypeOfPlace.asset ? "asset" : "uri";
    channel.invokeMethod(
      '${FlutterMediaPlugin.VIDEO_MEDIA_TYPE}/addAndPlay',
      {
        path: uri,
      },
    );
  }

  void initSetTexture() {
    channel.invokeMethod('${FlutterMediaPlugin.VIDEO_MEDIA_TYPE}/initSetTexture');
  }

  void play() {
    channel.invokeMethod('${FlutterMediaPlugin.VIDEO_MEDIA_TYPE}/play');
  }

  void pause() {
    channel.invokeMethod('${FlutterMediaPlugin.VIDEO_MEDIA_TYPE}/pause');
  }
}

enum TypeOfPlace {
  asset,
  network,
}

class VideoExoPlayerListener extends ExoPlayerListener {
  final Function(int) onVideoInitialize;

  VideoExoPlayerListener({
    onPlayerStateChanged,
    onPlaybackUpdate,
    onBufferedUpdate,
    onMediaPeriodCreated,
    onPlayerStatus,
    onPlaylistChanged,
    this.onVideoInitialize,
  }) : super(
    onPlayerStateChanged: onPlayerStateChanged,
    onPlaybackUpdate: onPlaybackUpdate,
    onBufferedUpdate: onBufferedUpdate,
    onMediaPeriodCreated: onMediaPeriodCreated,
    onPlayerStatus: onPlayerStatus,
    onPlaylistChanged: onPlaylistChanged,
  );
}
