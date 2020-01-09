import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter_media_plugin/flutter_media_plugin.dart';

class DownloadManager {
  final Set<DownloadListener> _downloadListeners = Set();

  final MethodChannel channel;

  DownloadManager({this.channel}) {
//    _initialize();
  }

  void _initialize() async {
    Map<dynamic, dynamic> arguments = await channel
        .invokeMethod('${FlutterMediaPlugin.DOWNLOAD_METHOD_TYPE}/initialize');
    print("initialize ${arguments.runtimeType}");
    if (arguments != null) {
      // TODO:
      // get all downloaded songs or videos
    }
  }

  void callMethod(String method, dynamic arguments) {
    switch(method) {
      case "onDownloadChanged":
        int state = arguments["state"];
        String id = arguments["url"];
        for (DownloadListener listener in _downloadListeners) {
          listener.onDownloadChanged(state, id);
        }
        break;
      default:
        print("Download manager method is not implemented");
        break;
    }
  }

  void addDownloadListener(DownloadListener listener) {
    _downloadListeners.add(listener);
  }

  void removeDownloadListener(DownloadListener listener) {
    _downloadListeners.remove(listener);
  }

  void download(String url) {
    channel.invokeMethod(
        '${FlutterMediaPlugin.DOWNLOAD_METHOD_TYPE}/download',
        {
          'url': url,
        });
  }

  void downloadRemove(String url) {
    channel.invokeMethod(
        '${FlutterMediaPlugin.DOWNLOAD_METHOD_TYPE}/downloadRemove',
        {
          'url': url,
        });
  }

  Future<bool> isDownloaded(String url) async {
    return await channel.invokeMethod(
        '${FlutterMediaPlugin.DOWNLOAD_METHOD_TYPE}/isDownloaded',
        {
          'url': url
        });
  }
}

class DownloadListener {
  final Function(int, String) _onDownloadChanged;
  DownloadListener({onDownloadChanged}): _onDownloadChanged = onDownloadChanged;

  void onDownloadChanged(int state, String id) {
    if (_onDownloadChanged != null) {
      _onDownloadChanged(state, id);
    }
  }
}