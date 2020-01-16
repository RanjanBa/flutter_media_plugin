import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter_media_plugin/download.dart';
import 'package:flutter_media_plugin/flutter_media_plugin.dart';
import 'package:flutter_media_plugin/media/media.dart';
import 'package:flutter_media_plugin/media/song.dart';
import 'package:flutter_media_plugin/media/video.dart';
import 'package:flutter_media_plugin/utility.dart';

class DownloadManager {
  final MethodChannel channel;

//  final Map<>
  final Set<DownloadManagerListener> _downloadManagerListeners = Set();

  List<Download<Song>> _downloadedSongs = List();
  List<Download<Video>> _downloadedVideos = List();

  int get downloadedSongSize => _downloadedSongs.length;

  int get downloadedVideoSize => _downloadedVideos.length;

  Download<Song> downloadedSongAtIndex(int index) {
    if (index < 0 || index >= _downloadedSongs.length) {
      return null;
    }

    return _downloadedSongs[index];
  }

  Download<Video> downloadedVideoAtIndex(int index) {
    if (index < 0 || index >= _downloadedVideos.length) {
      return null;
    }

    return _downloadedVideos[index];
  }

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
    switch (method) {
      case "onInitialized":
        print("download Initialized");
        _downloadedSongs.clear();
        _downloadedVideos.clear();

        List<Object> list = List.from(arguments);
        list.forEach((object) {
          Map<String, Object> map = Map.from(object);
          String mediaType = map[FieldUtility.media_type];
          if (mediaType == MediaType.song.toString().split('.')[1]) {
            Song song = Song.fromMap(map);
            int state = map['state'];
            int bytesDownloaded = map['bytesDownloaded'];
            double percent = map['percent'];

            _downloadedSongs
                .add(Download<Song>(song, state, bytesDownloaded, percent));
          } else if (mediaType == MediaType.video.toString().split('.')[1]) {}
        });
        for (DownloadManagerListener listener in _downloadManagerListeners) {
          listener.onInitialized();
        }
        break;
      case "onDownloadChangedOrAdded":
        int state = arguments['state'];
        int bytesDownloaded = arguments['bytesDownloaded'];
        double percent = arguments['percent'];

        Map<String, dynamic> map = Map.from(arguments['song']);

        if (map[FieldUtility.media_type] ==
            MediaType.song.toString().split('.')[1]) {
          Song song = Song.fromMap(map);
          bool isFound = false;
          for (int i = 0; i < _downloadedSongs.length; i++) {
            if (_downloadedSongs[i].media.key == song.key) {
              _downloadedSongs[i].updateDownloadState(state);
              _downloadedSongs[i].updateBytesDownloaded(bytesDownloaded);
              _downloadedSongs[i].updatePercentDownloaded(percent);
              isFound = true;
              break;
            }
          }

          if (!isFound) {
            Download<Song> download =
                Download(song, state, bytesDownloaded, percent);
            _downloadedSongs.add(download);
            int index = _downloadedSongs.length - 1;
            for (DownloadManagerListener listener
                in _downloadManagerListeners) {
              listener.onDownloadAdded(index, download);
            }
          }
        } else if (map[FieldUtility.media_type] ==
            MediaType.video.toString().split('.')[1]) {
          // TODO:
        }
        break;
      case "":
        break;
      case "onDownloadRemoved":
        Map<String, dynamic> map = Map.from(arguments);

        if (map[FieldUtility.media_type] ==
            MediaType.song.toString().split('.')[1]) {
          Song song = Song.fromMap(map);
          for (int i = 0; i < _downloadedSongs.length; i++) {
            if (_downloadedSongs[i].media.key == song.key) {
              Download<Media> download = _downloadedSongs.removeAt(i);
              for (DownloadManagerListener listener
                  in _downloadManagerListeners) {
                listener.onDownloadRemoved(i, download);
              }
              break;
            }
          }
        } else if (map[FieldUtility.media_type] ==
            MediaType.video.toString().split('.')[1]) {
          // TODO:
        }
        break;
      default:
        print("Download manager method is not implemented");
        break;
    }
  }

  void addDownloadListener(DownloadManagerListener listener) {
    _downloadManagerListeners.add(listener);
  }

  void removeDownloadListener(DownloadManagerListener listener) {
    _downloadManagerListeners.remove(listener);
  }

  void download(MediaType mediaType, Media media) {
    Map<String, dynamic> mediaMap = media.toJson();
//    print(mediaMap);
    mediaMap.putIfAbsent(
        FieldUtility.media_type, () => mediaType.toString().split('.')[1]);
    channel.invokeMethod(
        '${FlutterMediaPlugin.DOWNLOAD_METHOD_TYPE}/download', mediaMap);
  }

  void downloadRemove(MediaType mediaType, Media media) {
    Map<String, dynamic> mediaMap = media.toJson();
//    print(mediaMap);
    mediaMap.putIfAbsent(
        FieldUtility.media_type, () => mediaType.toString().split('.')[1]);
    channel.invokeMethod(
        '${FlutterMediaPlugin.DOWNLOAD_METHOD_TYPE}/downloadRemove', mediaMap);
  }

  Future<bool> isDownloaded(MediaType mediaType, Media media) async {
    Map<String, dynamic> mediaMap = media.toJson();
//    print(mediaMap);
    mediaMap.putIfAbsent(
        FieldUtility.media_type, () => mediaType.toString().split('.')[1]);
    return await channel.invokeMethod(
        '${FlutterMediaPlugin.DOWNLOAD_METHOD_TYPE}/isDownloaded', mediaMap);
  }
}

class DownloadManagerListener {
  final Function() _onInitialized;
  final Function(int, Download<Media>) _onDownloadAdded;
  final Function(int, Download<Media>) _onDownloadRemoved;

  DownloadManagerListener({
    Function() onInitialized,
    Function(int index, Download<Media>) onDownloadAdded,
    Function(int index, Download<Media>) onDownloadRemoved,
  })  : _onInitialized = onInitialized,
        _onDownloadAdded = onDownloadAdded,
        _onDownloadRemoved = onDownloadRemoved;

  void onInitialized() {
    if (_onInitialized != null) {
      _onInitialized();
    }
  }

  void onDownloadAdded(int index, Download<Media> download) {
    if (_onDownloadAdded != null) {
      _onDownloadAdded(index, download);
    }
  }

  void onDownloadRemoved(int index, Download<Media> download) {
    if (_onDownloadRemoved != null) {
      _onDownloadRemoved(index, download);
    }
  }
}
