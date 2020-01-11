import 'package:flutter/cupertino.dart';
import 'package:flutter_media_plugin/song.dart';
import 'package:flutter_media_plugin/utility.dart';

class Playlist {
  String _playlistName;
  List<Song> _songs;

  final Set<PlaylistListener> _playlistListeners = Set();

  String get playlistName => _playlistName;

  List<Song> get songs => _songs;

  Playlist(this._playlistName) {
    _songs = new List();
  }

  int getSize() {
    if (_songs == null) {
      return 0;
    }
    return _songs.length;
  }

  void addPlaylistListener(PlaylistListener listener) {
    _playlistListeners.add(listener);
  }

  void removePlaylistListener(PlaylistListener listener) {
    _playlistListeners.remove(listener);
  }

  void addSong(Song song) {
    if (_songs == null) {
      _songs = new List();
    }
    _songs.add(song);
    for (PlaylistListener listener in _playlistListeners) {
      listener.onSongAdded(song, _songs.length - 1);
    }
  }

  void addSongAtIndex(int index, Song song) {
    if (_songs == null) {
      return;
    }

    if (index > _songs.length) {
      return;
    }

    _songs.insert(index, song);
    for (PlaylistListener listener in _playlistListeners) {
      listener.onSongAdded(song, index);
    }
  }

  void removeSong(Song song) {
    if (_songs == null) {
      return;
    }
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
    if (_songs == null) {
      return;
    }
    if (index >= 0 && index < _songs.length) {
      Song song = _songs.removeAt(index);
      for (PlaylistListener listener in _playlistListeners) {
        listener.onSongAdded(song, index);
      }
    }
  }

  Song getSongAtIndex(int index) {
    if (index < 0 || _songs == null) return null;
    if (index >= _songs.length) return null;
    return _songs[index];
  }

  int isContainInPlaylist(Song song) {
    if (_songs == null) {
      return -1;
    }

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

    if (songs != null) {
      for (Map<String, dynamic> map in songs) {
        Song song = Song.fromMap(map);
        playlist.addSong(song);
      }
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

class PlaylistListener {
  final Function(Song song, int index) _onSongAdded;
  final Function(Song song, int index) _onSongRemoved;

  PlaylistListener({onSongAdded, onSongRemove})
      : _onSongRemoved = onSongRemove,
        _onSongAdded = onSongAdded;

  void onSongAdded(Song song, int index) {
    if (_onSongAdded != null) {
      _onSongAdded(song, index);
    }
  }

  void onSongRemoved(Song song, int index) {
    if (_onSongRemoved != null) {
      _onSongRemoved(song, index);
    }
  }
}
