import 'package:flutter/services.dart';
import 'package:flutter_media_plugin/exo_player_listener.dart';
import 'package:flutter_media_plugin/flutter_media_plugin.dart';
import 'package:flutter_media_plugin/media/video.dart';

enum TypeOfPlace {
  asset,
  network,
}

class VideoPlayer {
  final String playerId;
  final MethodChannel channel;

  bool _playWhenReady = false;
  int _playbackState = 1;
  int _repeatMode = 0;
  bool _shuffleModeEnabled = false;

  int _playbackPosition = 0;
  int _playbackLength = 0;
  int _bufferingPercent = 0;

  int _textureId;
  int _width;
  int _height;

  bool get playWhenReady => _playWhenReady;

  int get playbackState => _playbackState;

  int get repeatMode => _repeatMode;

  bool get shuffleModeEnabled => _shuffleModeEnabled;

  int get playbackPosition => _playbackPosition;

  int get playbackLength => _playbackLength;

  int get bufferingPercent => _bufferingPercent;

  int get textureId => _textureId;
  final Set<VideoExoPlayerListener> _exoPlayerListeners = Set();

  VideoPlayer({this.playerId, this.channel}) {
    _initialize();
  }

  void _initialize() {
    channel.invokeMethod('${FlutterMediaPlugin.VIDEO_METHOD_TYPE}/initialize');
  }

  void callMethod(String method, dynamic arguments) {
//    print("Video method: $method");
    switch (method) {
      case "onInitialized":

        if(arguments['textureId'] != null) {
          _textureId = arguments['textureId'];
        }
        break;
      case "onTextureIdChanged":
        _textureId = arguments['textureId'];
        for(VideoExoPlayerListener listener in _exoPlayerListeners) {
          listener.onTextureIdChanged(_textureId);
        }
        break;
      case "onSurfaceSizeChanged":
        _width = arguments['width'];
        _height = arguments['height'];
//        print("textureId $textureId");
        for (VideoExoPlayerListener listener in _exoPlayerListeners) {
          if (listener.onSurfaceSizeChanged != null) {
            listener.onSurfaceSizeChanged(_width, _height);
          }
        }
        break;
      default:
//        print("Video Method is not implemented");
        break;
    }
  }

  void addVideoExoPlayer(VideoExoPlayerListener videoExoPlayerListener) {
    _exoPlayerListeners.add(videoExoPlayerListener);
  }

  void removeVideoExoPlayer(VideoExoPlayerListener videoExoPlayerListener) {
    _exoPlayerListeners.remove(videoExoPlayerListener);
  }

  void addAndPlay(TypeOfPlace type, String url) {
    String path = type == TypeOfPlace.asset ? "asset" : "url";
    channel.invokeMethod(
      '${FlutterMediaPlugin.VIDEO_METHOD_TYPE}/addAndPlay',
      {
        path: url,
      },
    );
  }

  void play() {
    channel.invokeMethod('${FlutterMediaPlugin.VIDEO_METHOD_TYPE}/play');
  }

  void pause() {
    channel.invokeMethod('${FlutterMediaPlugin.VIDEO_METHOD_TYPE}/pause');
  }
}

class VideoExoPlayerListener extends ExoPlayerListener<Video> {
  final Function(int textureId) onTextureIdChanged;
  final Function(int width, int height) onSurfaceSizeChanged;

  VideoExoPlayerListener({
    onPlayerStateChanged,
    onPlaybackUpdate,
    onBufferedUpdate,
    onMediaPeriodCreated,
    onPlayerStatus,
    this.onSurfaceSizeChanged,
    this.onTextureIdChanged,
  }) : super(
          onPlayerStateChanged: onPlayerStateChanged,
          onPlaybackUpdate: onPlaybackUpdate,
          onBufferedUpdate: onBufferedUpdate,
          onMediaPeriodCreated: onMediaPeriodCreated,
          onPlayerStatus: onPlayerStatus,
        );
}
