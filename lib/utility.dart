class FieldUtility {
  // ignore: non_constant_identifier_names
  static const final String song_key_tag = "id";

  // ignore: non_constant_identifier_names
  static const final String song_title_tag = "title";

  // ignore: non_constant_identifier_names
  static const final String song_artists_tag = "artists";

  // ignore: non_constant_identifier_names
  static const final String song_album_tag = "album";

  // ignore: non_constant_identifier_names
  static const final String song_album_art_url_tag = "artUrl";

  // ignore: non_constant_identifier_names
  static const final String song_url_tag = "url";

  // ignore: non_constant_identifier_names
  static const final String playlist_name = "playlistName";

  // ignore: non_constant_identifier_names
  static const final String media_playlist = "mediaPlaylist";

  // ignore: non_constant_identifier_names
  static const final String media_type = "mediaType";
}

class ExoPlayerUtility {
  // ignore: non_constant_identifier_names
  static const final int REPEAT_MODE_OFF = 0;

  /// "Repeat One" mode to repeat the currently playing window infinitely.
  // ignore: non_constant_identifier_names
  static const final int REPEAT_MODE_ONE = 1;

  /// "Repeat All" mode to repeat the entire timeline infinitely.
  // ignore: non_constant_identifier_names
  static const final int REPEAT_MODE_ALL = 2;

  // ignore: non_constant_identifier_names
  static const final int STATE_IDLE = 1;

  /// The player is not able to immediately play from its current position. This state typically
  /// occurs when more data needs to be loaded.
  // ignore: non_constant_identifier_names
  static const int STATE_BUFFERING = 2;

  /// The player is able to immediately play from its current position. The player will be playing if
  /// {@link #getPlayWhenReady()} is true, and paused otherwise.
  // ignore: non_constant_identifier_names
  static const int STATE_READY = 3;

  /// The player has finished playing the media.
  // ignore: non_constant_identifier_names
  static const final int STATE_ENDED = 4;
}

class DownloadUtility {
  // ignore: non_constant_identifier_names
  static const final int STATE_QUEUED = 0;

  /// The download is stopped for a specified {@link #stopReason}. */
  // ignore: non_constant_identifier_names
  static const final int STATE_STOPPED = 1;

  /// The download is currently started. */
  // ignore: non_constant_identifier_names
  static const final int STATE_DOWNLOADING = 2;

  /// The download completed. */
  // ignore: non_constant_identifier_names
  static const final int STATE_COMPLETED = 3;

  /// The download failed. */
  // ignore: non_constant_identifier_names
  static const final int STATE_FAILED = 4;

  /// The download is being removed. */
  // ignore: non_constant_identifier_names
  static const final int STATE_REMOVING = 5;

  /// The download will restart after all downloaded data is removed. */
  // ignore: non_constant_identifier_names
  static const final int STATE_RESTARTING = 7;
}
