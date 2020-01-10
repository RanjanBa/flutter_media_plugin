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

  int get duration => _duration;

  VideoPlayer({this.playerId, this.channel}) {
    _initialize();
  }

  void _initialize() async {
    Map<dynamic, dynamic> arguments = await channel.invokeMethod('${FlutterMediaPlugin.VIDEO_METHOD_TYPE}/initialize');
//    print("initialize ${arguments.runtimeType}");
    if (arguments != null) {
      _textureId = arguments["textureId"];
    }
  }

  void callMethod(String method, dynamic arguments) {
//    print("Video method: $method");
    switch (method) {
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

enum TypeOfPlace {
  asset,
  network,
}

class VideoExoPlayerListener extends ExoPlayerListener {
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
