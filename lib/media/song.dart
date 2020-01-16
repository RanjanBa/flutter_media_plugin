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
    for (int i = 0; i < str.length; i++) {
      if (isSpaceFound) {
        str = str.substring(0, i) + str[i].toUpperCase() + str.substring(i + 1);
        isSpaceFound = false;
      }

      if (str[i] == ' ') {
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
      FieldUtility.song_key_tag: key,
      FieldUtility.song_title_tag: title,
      FieldUtility.song_artists_tag: artists,
      FieldUtility.song_album_tag: album,
      FieldUtility.song_album_art_url_tag: album_art_url,
      FieldUtility.song_url_tag: url,
    };
  }

  @override
  factory Song.fromMap(Map<String, dynamic> map) {
    String key = map[FieldUtility.song_key_tag];
    String title = map[FieldUtility.song_title_tag];
    String artists = map[FieldUtility.song_artists_tag];
    String album = map[FieldUtility.song_album_tag];
    String albumArtUrl = map[FieldUtility.song_album_art_url_tag];
    String url = map[FieldUtility.song_url_tag];
    Song song = Song(key, title, artists, album, albumArtUrl, url);
    return song;
  }
}
