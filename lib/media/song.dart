import 'package:flutter_media_plugin/media/media.dart';
import 'package:flutter_media_plugin/utility.dart';

class Song implements Media {
  final String _key;
  final String _title;
  final String _artists;
  final String _album;

  // ignore: non_constant_identifier_names
  final String _album_art_url;
  final String _url;

  static String _capitalizeEveryWord(String str) {
    bool isSpaceFound = true;
    for(int i = 0; i < str.length; i++) {
      if(isSpaceFound) {
        str = str.substring(0, i) + str[i].toUpperCase() + str.substring(i + 1);
        isSpaceFound = false;
      }

      if(str[i] == ' ') {
        isSpaceFound = true;
      }
    }

    return str;
  }

  String get key => _key;

  String get title => _capitalizeEveryWord(_title);

  String get artists => _capitalizeEveryWord(_artists);

  String get album => _capitalizeEveryWord(_album);

  // ignore: non_constant_identifier_names
  String get album_art_url => _album_art_url;

  String get url => _url;

  Song(
      String key,
      String title,
      String artists,
      String album,
      // ignore: non_constant_identifier_names
      String album_art_url,
      String url)
      : _key = key,
        _title = title,
        _artists = artists,
        _album = album,
        _album_art_url = album_art_url,
        _url = url;

  @override
  Map<String, dynamic> toJson() {
    return {
      Utility.song_key_tag: key,
      Utility.song_title_tag: title,
      Utility.song_artists_tag: artists,
      Utility.song_album_tag: album,
      Utility.song_album_art_url_tag: album_art_url,
      Utility.song_url_tag: url,
    };
  }

  @override
  factory Song.fromMap(Map<String, dynamic> map) {
    String key = map[Utility.song_key_tag];
    String title = map[Utility.song_title_tag];
    String artists = map[Utility.song_artists_tag];
    String album = map[Utility.song_album_tag];
    String albumArtUrl = map[Utility.song_album_art_url_tag];
    String url = map[Utility.song_url_tag];
    Song song = Song(key, title, artists, album, albumArtUrl, url);
    return song;
  }
}
