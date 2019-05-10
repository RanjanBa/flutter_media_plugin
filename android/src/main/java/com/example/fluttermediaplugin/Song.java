package com.example.fluttermediaplugin;

import org.json.JSONException;
import org.json.JSONObject;

public class Song {
    private String key;
    private String title;
    private String artist;
    private String album;
    private String album_art_uri;
    private String uri;

    static String song_key_tag = "key";
    static String song_title_tag = "title";
    static String song_artist_tag = "artist";
    static String song_album_tag = "album";
    static String song_album_art_uri_tag = "album_art_uri";
    static String song_uri_tag = "uri";

    public Song(String key, String title, String artist, String album, String album_art_uri, String uri) {
        this.key = key;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.album_art_uri = album_art_uri;
        this.uri = uri;
    }

    public String getKey() {
        return key;
    }

    public String getUri() {
        return uri;
    }

    public String getAlbumArtUri() {
        return album_art_uri;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public static JSONObject toJson(Song song) {
        JSONObject objectMap = new JSONObject();

        try {
            objectMap.put(song_key_tag, song.getKey());
            objectMap.put(song_title_tag, song.getTitle());
            objectMap.put(song_artist_tag, song.getArtist());
            objectMap.put(song_album_tag, song.getAlbum());
            objectMap.put(song_album_art_uri_tag, song.getAlbumArtUri());
            objectMap.put(song_uri_tag, song.getUri());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return objectMap;
    }

    public static Song fromJson(JSONObject jsonObject) {
        try {
            String key = jsonObject.getString(Song.song_key_tag);
            String title = jsonObject.getString(Song.song_title_tag);
            String artist = jsonObject.getString(Song.song_artist_tag);
            String album = jsonObject.getString(Song.song_album_tag);
            String album_art_uri = jsonObject.getString(Song.song_album_art_uri_tag);
            String uri = jsonObject.getString(Song.song_uri_tag);
            Song song = new Song(key, title, artist, album, album_art_uri, uri);
            return song;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
}
