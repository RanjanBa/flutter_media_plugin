import 'package:flutter_media_plugin/utility.dart';
import 'package:meta/meta.dart';

class Song {
  final String key;
  final String title;
  final String artists;
  final String album;
  // ignore: non_constant_identifier_names
  final String album_art_url;
  final String url;

  Song({
    @required this.key,
    @required this.title,
    @required this.artists,
    @required this.album,
    // ignore: non_constant_identifier_names
    @required this.album_art_url,
    @required this.url,
  });

  Map<String, String> toJson() {
    return {
      Utility.song_key_tag: key,
      Utility.song_title_tag: title,
      Utility.song_artists_tag: artists,
      Utility.song_album_tag: album,
      Utility.song_album_art_url_tag: album_art_url,
      Utility.song_url_tag: url,
    };
  }

  factory Song.fromMap(Map<String, dynamic> map) {
    String key = map[Utility.song_key_tag];
    String title = map[Utility.song_title_tag];
    String artists = map[Utility.song_artists_tag];
    String album = map[Utility.song_album_tag];
    String albumArtUrl = map[Utility.song_album_art_url_tag];
    String url = map[Utility.song_url_tag];
    Song song = Song(
        key: key,
        title: title,
        artists: artists,
        album: album,
        album_art_url: albumArtUrl,
        url: url);
    return song;
  }
}
