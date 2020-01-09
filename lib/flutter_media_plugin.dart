import 'package:flutter/services.dart';
import 'package:flutter_media_plugin/audio_player.dart';
import 'package:flutter_media_plugin/video_player.dart';
import 'package:flutter_media_plugin/download_manager.dart';

class FlutterMediaPlugin {
  static const String VIDEO_METHOD_TYPE = "VIDEO_TYPE";
  static const String AUDIO_METHOD_TYPE = "AUDIO_TYPE";
  static const String DOWNLOAD_METHOD_TYPE = "DOWNLOAD_TYPE";

  static RegExp _regExp = new RegExp(r"([^/]+)/([^/]+)");
  static const MethodChannel _channel =
      const MethodChannel('flutter_media_plugin');

  static AudioPlayer _audioPlayer;
  static VideoPlayer _videoPlayer;
  static DownloadManager _downloadManager;

  FlutterMediaPlugin() {
    _channel.setMethodCallHandler((MethodCall call) async {
      try {
        Match match = _regExp.firstMatch(call.method);

        if (match.groupCount >= 2) {
          String methodType, method;
          methodType = match.group(1);
          method = match.group(2);
          if (methodType == AUDIO_METHOD_TYPE) {
            if (_audioPlayer != null) {
              _audioPlayer.callMethod(method, call.arguments);
            }
          } else if (methodType == VIDEO_METHOD_TYPE) {
            if (_videoPlayer != null) {
              _videoPlayer.callMethod(method, call.arguments);
            }
          } else if (methodType == DOWNLOAD_METHOD_TYPE) {
            if (_downloadManager != null) {
              _downloadManager.callMethod(method, call.arguments);
            }
          }
        }
      } catch (e) {
        print(e.toString());
      }
    });
  }

  static AudioPlayer get audioPlayer {
    if (_audioPlayer != null) {
      return _audioPlayer;
    }

    return _audioPlayer =
        new AudioPlayer(playerId: "audioPlayer", channel: _channel);
  }

  static VideoPlayer get videoPlayer {
    if (_videoPlayer != null) {
      return _videoPlayer;
    }

    return _videoPlayer =
        new VideoPlayer(playerId: "videoPlayer", channel: _channel);
  }

  static DownloadManager get downloadManager {
    if (_downloadManager != null) {
      return _downloadManager;
    }

    return _downloadManager = new DownloadManager(channel: _channel);
  }
}
