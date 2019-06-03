package com.example.fluttermediaplugin;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Song {
    private String key;
    private String title;
    private String artists;
    private String album;
    private String album_art_url;
    private String url;

    static String song_key_tag = "key";
    static String song_title_tag = "title";
    static String song_artists_tag = "artists";
    static String song_album_tag = "album";
    static String song_album_art_url_tag = "album_art_url";
    static String song_url_tag = "uri";

    public Song(String key, String title, String artist, String album, String album_art_url, String url) {
        this.key = key;
        this.title = title;
        this.artists = artist;
        this.album = album;
        this.album_art_url = album_art_url;
        this.url = url;
    }

    public String getKey() {
        return key;
    }

    public String getUri() {
        return url;
    }

    public String getAlbumArtUri() {
        return album_art_url;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artists;
    }

    public String getTitle() {
        return title;
    }

    public static JSONObject toJson(Song song) {
        JSONObject objectMap = new JSONObject();

        try {
            objectMap.put(song_key_tag, song.getKey());
            objectMap.put(song_title_tag, song.getTitle());
            objectMap.put(song_artists_tag, song.getArtist());
            objectMap.put(song_album_tag, song.getAlbum());
            objectMap.put(song_album_art_url_tag, song.getAlbumArtUri());
            objectMap.put(song_url_tag, song.getUri());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return objectMap;
    }

    public static Song fromJson(JSONObject jsonObject) {
        try {
            String key = jsonObject.getString(Song.song_key_tag);
            String title = jsonObject.getString(Song.song_title_tag);
            String artists = jsonObject.getString(Song.song_artists_tag);
            String album = jsonObject.getString(Song.song_album_tag);
            String album_art_url = jsonObject.getString(Song.song_album_art_url_tag);
            String url = jsonObject.getString(Song.song_url_tag);
            return new Song(key, title, artists, album, album_art_url, url);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Map<String, Object> toMap(Song song) {
        Map<String, Object> songMap = new HashMap<>();
        songMap.put(song_key_tag, song.key);
        songMap.put(song_title_tag, song.title);
        songMap.put(song_artists_tag, song.artists);
        songMap.put(song_album_tag, song.album);
        songMap.put(song_album_art_url_tag, song.album_art_url);
        songMap.put(song_url_tag, song.url);
        return songMap;
    }
}
