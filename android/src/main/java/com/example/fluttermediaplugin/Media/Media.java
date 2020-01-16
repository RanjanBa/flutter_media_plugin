package com.example.fluttermediaplugin.Media;

import java.util.Map;

public interface Media {
    String getKey();
    String getTitle();
    String getUrl();
    Map<String, Object> toMap();
}
