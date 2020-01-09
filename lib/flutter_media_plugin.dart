import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_media_plugin/audio_player.dart';
import 'package:flutter_media_plugin/video_player.dart';

import 'download_listener.dart';

class FlutterMediaPlugin {
  static const String VIDEO_MEDIA_TYPE = "VIDEO_TYPE";
  static const String AUDIO_MEDIA_TYPE = "AUDIO_TYPE";
  static RegExp _regExp = new RegExp(r"([^/]+)/([^/]+)");
  static const MethodChannel _channel =
      const MethodChannel('flutter_media_plugin');

  static AudioPlayer _audioPlayer;
  static VideoPlayer _videoPlayer;
  static final Set<DownloadListener> _downloadListeners = Set();

  FlutterMediaPlugin() {
    _channel.setMethodCallHandler((MethodCall call) async {

      try {
        Match match = _regExp.firstMatch(call.method);

        if (match.groupCount >= 2) {
          String mediaType, method;
          mediaType = match.group(1);
          method = match.group(2);
//          if (method != "onBufferedUpdate" && method != "onPlaybackUpdate") {}
//          print("Type : $mediaType Method : $method");
          if (mediaType == AUDIO_MEDIA_TYPE) {
            if (_audioPlayer != null) {
              _audioPlayer.callMethod(method, call.arguments);
            }
          } else if (mediaType == VIDEO_MEDIA_TYPE) {
            if (_videoPlayer != null) {
              _videoPlayer.callMethod(method, call.arguments);
            }
          }
        }
      } catch (e) {
        switch(call.method) {
          case "onDownloadChanged":
            int state = call.arguments["state"];
            String id = call.arguments["id"];
            for (DownloadListener listener in _downloadListeners) {
              listener.onDownloadChanged(state, id);
            }
            break;
          default:
            print("Method not implemented");
            break;
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

  static void addDownloadListener(DownloadListener listener) {
    _downloadListeners.add(listener);
  }

  static void removeDownloadListener(DownloadListener listener) {
    _downloadListeners.remove(listener);
  }

  static void download(String url) {
    _channel.invokeMethod(
        'download',
        {
          C.song_url_tag: url,
        });
  }

  static void downloadRemove(String url) {
    _channel.invokeMethod(
        'downloadRemove',
        {
          C.song_url_tag: url,
        });
  }
  
  static Future<bool> isDownloaded(String url) async {
    return await _channel.invokeMethod(
        'isDownloaded',
        {
          C.song_url_tag: url
        });
  }
}
