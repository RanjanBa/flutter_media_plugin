class Utility {
  // ignore: non_constant_identifier_names
  static String song_key_tag = "id";
  // ignore: non_constant_identifier_names
  static String song_title_tag = "title";
  // ignore: non_constant_identifier_names
  static String song_artists_tag = "artists";
  // ignore: non_constant_identifier_names
  static String song_album_tag = "album";
  // ignore: non_constant_identifier_names
  static String song_album_art_url_tag = "artUrl";
  // ignore: non_constant_identifier_names
  static String song_url_tag = "url";

  // ignore: non_constant_identifier_names
  static String playlist_name = "playlistName";
  // ignore: non_constant_identifier_names
  static String media_playlist = "mediaPlaylist";


  // ignore: non_constant_identifier_names
  static int REPEAT_MODE_OFF = 0;
  /// "Repeat One" mode to repeat the currently playing window infinitely.
  // ignore: non_constant_identifier_names
  static int REPEAT_MODE_ONE = 1;
  /// "Repeat All" mode to repeat the entire timeline infinitely.
  // ignore: non_constant_identifier_names
  static int REPEAT_MODE_ALL = 2;

  // ignore: non_constant_identifier_names
  static int STATE_IDLE = 1;
  /// The player is not able to immediately play from its current position. This state typically
  /// occurs when more data needs to be loaded.
  // ignore: non_constant_identifier_names
  static int STATE_BUFFERING = 2;
  /// The player is able to immediately play from its current position. The player will be playing if
  /// {@link #getPlayWhenReady()} is true, and paused otherwise.
  // ignore: non_constant_identifier_names
  static int STATE_READY = 3;
  /// The player has finished playing the media.
  // ignore: non_constant_identifier_names
  static int STATE_ENDED = 4;
}