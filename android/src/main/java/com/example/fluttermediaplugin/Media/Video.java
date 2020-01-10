package com.example.fluttermediaplugin.Media;

import androidx.annotation.NonNull;

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
}
