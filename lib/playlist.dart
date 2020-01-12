import 'package:flutter_media_plugin/media/media.dart';
import 'package:flutter_media_plugin/media/song.dart';
import 'package:flutter_media_plugin/utility.dart';

class Playlist<T extends Media> {
  String _playlistName;
  List<T> _mediaList;

  String get playlistName => _playlistName;

  List<T> get mediaList => _mediaList;

  Playlist(this._playlistName) {
    _mediaList = new List();
  }

  int getSize() {
    return _mediaList.length;
  }

  int addMedia(T media) {
    if(media == null) {
      return -1;
    }

    _mediaList.add(media);
    return _mediaList.length - 1;
  }

  bool addMediaAtIndex(int index, T media) {
    if (index > _mediaList.length || media == null) {
      return false;
    }

    _mediaList.insert(index, media);
    return true;
  }

  int removeMedia(T media) {
    if (media == null) {
      return -1;
    }

    int index = -1;
    for (int i = 0; i < _mediaList.length; i++) {
      if (_mediaList[i].key == media.key) {
        index = i;
        break;
      }
    }

    if (index >= 0) {
      if (removeMediaAtIndex(index)) {
        return index;
      }
      return -1;
    }

    return -1;
  }

  bool removeMediaAtIndex(int index) {
    if (index >= 0 && index < _mediaList.length) {
      _mediaList.removeAt(index);
      return true;
    }

    return false;
  }

  T getMediaAtIndex(int index) {
    if (index < 0) return null;
    if (index >= _mediaList.length) return null;
    return _mediaList[index];
  }

  int isContainInPlaylist(T media) {
    for (int i = 0; i < _mediaList.length; i++) {
      if (media.key == _mediaList[i].key) {
        return i;
      }
    }

    return -1;
  }

  static Playlist<Song> songsPlaylistFromMap(Map<String, dynamic> json) {
    Playlist<Song> playlist = Playlist(json[Utility.playlist_name].toString());
    List<dynamic> tempMediaList = json[Utility.media_playlist];

    if (tempMediaList != null) {
      for (Map<String, dynamic> map in tempMediaList) {
        Song media = Song.fromMap(map);
        playlist.addMedia(media);
      }
    }
    return playlist;
  }

  Map<String, dynamic> toJson() {
    List<dynamic> songObject = List();

    for (T media in _mediaList) {
      songObject.add(media.toJson());
    }

    return {
      Utility.playlist_name: _playlistName,
      Utility.media_playlist: songObject,
    };
  }
}
