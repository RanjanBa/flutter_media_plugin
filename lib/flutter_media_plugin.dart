import 'package:flutter/services.dart';
import 'package:flutter_media_plugin/audio_player.dart';
import 'package:flutter_media_plugin/video_player.dart';

class FlutterMediaPlugin {
  static const String VIDEO_MEDIA_TYPE = "VIDEO_TYPE";
  static const String AUDIO_MEDIA_TYPE = "AUDIO_TYPE";
  static RegExp _regExp = new RegExp(r"([^/]+)/([^/]+)");
  static const MethodChannel _channel =
      const MethodChannel('flutter_media_plugin');

  AudioPlayer _audioPlayer;
  VideoPlayer _videoPlayer;

  FlutterMediaPlugin.initialize() {
    _audioPlayer = new AudioPlayer(playerId: "audioPlayer", channel: _channel);
    _videoPlayer = new VideoPlayer(playerId: "videoPlayer", channel: _channel);
    _channel.setMethodCallHandler((MethodCall call) {
      print("Video Method : ${call.method}");
      Match match = _regExp.firstMatch(call.method);
      String mediaType, method;
      if (match.groupCount >= 2) {
        mediaType = match.group(1);
        method = match.group(2);
      } else {
        return;
      }
      if (mediaType == AUDIO_MEDIA_TYPE) {
        _audioPlayer.callMethod(method, call.arguments);
      } else if (mediaType == VIDEO_MEDIA_TYPE) {
        _videoPlayer.callMethod(method, call.arguments);
      }
    });
  }

  AudioPlayer get audioPlayer {
    return _audioPlayer;
  }

  VideoPlayer get videoPlayer {
    return _videoPlayer;
  }
}
