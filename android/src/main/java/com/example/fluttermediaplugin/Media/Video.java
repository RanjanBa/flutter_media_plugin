package com.example.fluttermediaplugin.Media;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

import static com.example.fluttermediaplugin.Utility.MediaIds.KEY_TAG;
import static com.example.fluttermediaplugin.Utility.MediaIds.TITLE_TAG;
import static com.example.fluttermediaplugin.Utility.MediaIds.URL_TAG;

public class Video implements Media {
    private String key;
    private String title;
    private String url;

    Video(@NonNull String key, @NonNull String title, @NonNull String url) {
        this.key = key;
        this.title = title;
        this.url = url;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getUrl() {
        return url;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> songMap = new HashMap<>();
        songMap.put(KEY_TAG, key);
        songMap.put(TITLE_TAG, title);
        songMap.put(URL_TAG, url);
        return songMap;
    }

    public static Video fromMap(Map<String, String> mapObject) {
        String key = mapObject.get(KEY_TAG);
        String title = mapObject.get(TITLE_TAG);
        String url = mapObject.get(URL_TAG);

        if(key == null || title == null || url == null) {
            return null;
        }

        return new Video(key, title, url);
    }
}
