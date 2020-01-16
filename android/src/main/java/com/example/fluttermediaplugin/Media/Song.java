package com.example.fluttermediaplugin.Media;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

import static com.example.fluttermediaplugin.Utility.MediaIds.ALBUM_ART_URL_TAG;
import static com.example.fluttermediaplugin.Utility.MediaIds.KEY_TAG;
import static com.example.fluttermediaplugin.Utility.MediaIds.SONG_ALBUM_TAG;
import static com.example.fluttermediaplugin.Utility.MediaIds.SONG_ARTISTS_TAG;
import static com.example.fluttermediaplugin.Utility.MediaIds.TITLE_TAG;
import static com.example.fluttermediaplugin.Utility.MediaIds.URL_TAG;

public final class Song implements Media {
    private String key;
    private String title;
    private String url;
    private String artists;
    private String album;
    private String album_art_url;

    private static String capitalizeEveryWord(String str) {
        boolean isSpaceFound = true;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if(isSpaceFound) {
                String s = "" + c;
                s = s.toUpperCase();

                str = str.substring(0, i) + s + str.substring(i + 1);
                isSpaceFound = false;
            }
            if(c == ' ') {
                isSpaceFound = true;
            }
        }

        return str;
    }

    private Song(@NonNull String key, @NonNull String title, @NonNull String artist, @NonNull String album, @NonNull String album_art_url, @NonNull String url) {
        this.key = key;
        this.title = title;
        this.artists = artist;
        this.album = album;
        this.album_art_url = album_art_url;
        this.url = url;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getTitle() {
        return capitalizeEveryWord(title);
    }

    @Override
    public String getUrl() {
        return url;
    }

    public String getAlbumArtUri() {
        return album_art_url;
    }

    public String getAlbum() {
        return capitalizeEveryWord(album);
    }

    public String getArtist() {
        return capitalizeEveryWord(artists);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> songMap = new HashMap<>();
        songMap.put(KEY_TAG, key);
        songMap.put(TITLE_TAG, title);
        songMap.put(SONG_ARTISTS_TAG, artists);
        songMap.put(SONG_ALBUM_TAG, album);
        songMap.put(ALBUM_ART_URL_TAG, album_art_url);
        songMap.put(URL_TAG, url);
        return songMap;
    }

    public static Song fromMap(Map<String, String> mapObject) {
        String key = mapObject.get(KEY_TAG);
        String title = mapObject.get(TITLE_TAG);
        String artists = mapObject.get(SONG_ARTISTS_TAG);
        String album = mapObject.get(SONG_ALBUM_TAG);
        String album_art_url = mapObject.get(ALBUM_ART_URL_TAG);
        String url = mapObject.get(URL_TAG);

        if(key == null || title == null || artists == null || album == null || album_art_url == null || url == null) {
            return null;
        }

        return new Song(key, title, artists, album, album_art_url, url);
    }
}
