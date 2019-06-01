import 'package:flutter/services.dart';
import 'package:flutter_media_plugin/audio_player.dart';
import 'package:flutter_media_plugin/video_player.dart';

class FlutterMediaPlugin {
  static const String VIDEO_MEDIA_TYPE = "VIDEO_TYPE";
  static const String AUDIO_MEDIA_TYPE = "AUDIO_TYPE";
  static RegExp _regExp = new RegExp(r"([^/]+)/([^/]+)");
  static const MethodChannel _channel =
      const MethodChannel('flutter_media_plugin');

  static AudioPlayer _audioPlayer;
  static VideoPlayer _videoPlayer;

  FlutterMediaPlugin() {
    _channel.setMethodCallHandler((MethodCall call) {
      Match match = _regExp.firstMatch(call.method);
      String mediaType, method;
      if (match.groupCount >= 2) {
        mediaType = match.group(1);
        method = match.group(2);
      } else {
        return;
      }
      if (method != "onBufferedUpdate" && method != "onPlaybackUpdate")
        print("Type : $mediaType Method : $method");
      if (mediaType == AUDIO_MEDIA_TYPE) {
        if (_audioPlayer != null) {
          _audioPlayer.callMethod(method, call.arguments);
        }
      } else if (mediaType == VIDEO_MEDIA_TYPE) {
        if (_videoPlayer != null) {
          _videoPlayer.callMethod(method, call.arguments);
        }
      }
    });
  }

  static AudioPlayer get audioPlayer {
    return _audioPlayer =
        new AudioPlayer(playerId: "audioPlayer", channel: _channel);
  }

  static VideoPlayer get videoPlayer {
    return _videoPlayer =
        new VideoPlayer(playerId: "videoPlayer", channel: _channel);
  }
}
