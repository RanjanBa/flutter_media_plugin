import 'package:flutter_media_plugin/audio_player.dart';
import 'package:flutter_media_plugin/song.dart';
import 'package:flutter_media_plugin/utility.dart';

class Playlist {
  String _playlistName;
  List<Song> _songs;

  String get playlistName => _playlistName;

  List<Song> get songs => _songs;

  Playlist(this._playlistName) {
    _songs = List();
  }

  int getSize() {
    if(_song == null) {
      return 0;
    }
    return _songs.length;
  }

  void addSong(Song song) {
    _songs.add(song);
  }

  void addSongAtIndex(int index, Song song) {
    if (index > _songs.length) {
      return;
    }

    _songs.insert(index, song);
  }

  void removeSong(Song song) {
    int index = -1;
    for (int i = 0; i < _songs.length; i++) {
      if (_songs[i].key == song.key) {
        index = i;
        break;
      }
    }

    if (index >= 0) {
      removeSongAtIndex(index);
    }
  }

  void removeSongAtIndex(int index) {
    if(index >= 0 && index < _songs.length) {
      _songs.removeAt(index);
    }
  }

  Song getSongAtIndex(int index) {
    if (index < 0) return null;
    if (index >= _songs.length) return null;
    return _songs[index];
  }

  int isContainInPlaylist(Song song) {
    for (int i = 0; i < _songs.length; i++) {
      if (song.key == _songs[i].key) {
        return i;
      }
    }

    return -1;
  }

  factory Playlist.fromMap(Map<String, dynamic> json) {
    Playlist playlist = Playlist(json[Utility.playlist_name].toString());
    List<dynamic> songs = json[Utility.media_playlist];

    for (Map<String, dynamic> map in songs) {
      Song song = Song.fromMap(map);
      playlist.addSong(song);
    }
    return playlist;
  }

  Map<String, dynamic> toJson() {
    List<dynamic> songObject = List();

    for (Song song in _songs) {
      songObject.add(song.toJson());
    }

    return {
      Utility.playlist_name: _playlistName,
      Utility.media_playlist: songObject,
    };
  }
}
