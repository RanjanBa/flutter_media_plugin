package com.example.fluttermediaplugin;

import androidx.annotation.NonNull;

import android.net.Uri;
import android.util.Log;

import com.example.fluttermediaplugin.Media.Song;
import com.google.android.exoplayer2.Player;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterMediaPlugin
 */
public class FlutterMediaPlugin implements MethodCallHandler {
    private static final String TAG = "FlutterMediaPlugin";
    private static Pattern METHOD_NAME_MATCH = Pattern.compile("([^/]+)/([^/]+)");

    static String VIDEO_METHOD_TYPE = "VIDEO_TYPE";
    static String AUDIO_METHOD_TYPE = "AUDIO_TYPE";
    static String DOWNLOAD_METHOD_TYPE = "DOWNLOAD_TYPE";

    private static FlutterMediaPlugin instance;

    private Registrar registrar;
    private AudioPlayer audioPlayer;
    private VideoPlayer videoPlayer;
    private DownloadManager downloadManager;

    private MethodChannel channel;

    static FlutterMediaPlugin getInstance() {
        if (instance == null) {
            Log.e(TAG, "Flutter Media plugin instance is null");
        }
        return instance;
    }

    Registrar getRegistrar() {
        return registrar;
    }

    AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    DownloadManager getDownloadManager() {
        return downloadManager;
    }

    private FlutterMediaPlugin(Registrar _registrar) {
        this.registrar = _registrar;
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_media_plugin");

        channel.setMethodCallHandler(this);
        this.channel = channel;
    }

    private void initializeAudioPlayer() {
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(registrar.context()).build();
        ImageLoader.getInstance().init(config);

        audioPlayer = new AudioPlayer(registrar.activeContext(), channel);
    }

    private void initializeVideoPlayer() {
        videoPlayer = new VideoPlayer(registrar.activeContext(), registrar.textures(), channel);
    }

    /**
     * Plugin registration.
     */

    public static void registerWith(@NonNull Registrar _registrar) {
        if (instance == null) instance = new FlutterMediaPlugin(_registrar);
        else {
            instance.registrar = _registrar;
            MethodChannel channel = new MethodChannel(_registrar.messenger(), "flutter_media_plugin");
            channel.setMethodCallHandler(instance);
            instance.channel = channel;

            if (instance.audioPlayer != null) {
                instance.audioPlayer.setChannel(channel);
            }

            if (instance.videoPlayer != null) {
                instance.videoPlayer.setChannel(channel);
            }
        }

        if (instance.downloadManager != null) {
            instance.downloadManager.setChannel(instance.channel);
        } else {
            instance.downloadManager = new DownloadManager(instance.getRegistrar().activeContext(), instance.channel);
        }
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        MethodTypeCall methodTypeCall = parseMethodName(call.method);
//        Log.d(TAG, methodTypeCall.toString());
        if (methodTypeCall.methodType != null) {
            if (methodTypeCall.methodType.equals(AUDIO_METHOD_TYPE)) {
                if (methodTypeCall.method.equals("initialize")) {
                    if (audioPlayer == null) {
                        initializeAudioPlayer();
                    } else {
                        Log.d(TAG, "Already audioPlayer is initialized");
                        audioPlayer.initialize();
                    }

                    result.success(null);
                    return;
                }

                if (audioPlayer == null) {
                    Log.d(TAG, "AudioPlayer is null");
                    result.success(null);
                    return;
                }

                audioMethodCall(methodTypeCall.method, call, result);
            } else if (methodTypeCall.methodType.equals(VIDEO_METHOD_TYPE)) {
                if (methodTypeCall.method.equals("initialize")) {
                    if (videoPlayer == null) {
                        initializeVideoPlayer();
                    } else {
                        Log.d(TAG, "Already videoPlayer is initialized");
                        videoPlayer.initialize();
                    }
                    result.success(null);
                    return;
                }

                if (videoPlayer == null) {
                    Log.d(TAG, "VideoPlayer is null");
                    result.success(null);
                    return;
                }

                videoMethodCall(methodTypeCall.method, call, result);
            } else if (methodTypeCall.methodType.equals(DOWNLOAD_METHOD_TYPE)) {
                if (downloadManager == null) {
                    downloadManager = new DownloadManager(getRegistrar().activeContext(), instance.channel);
                }

                downloadMethodCall(methodTypeCall.method, call, result);
            }
        } else {
            Log.e(TAG, "Method is not called appropriately");
        }
    }

    private void audioMethodCall(String method, MethodCall call, Result result) {
        switch (method) {
            case "play":
//                Log.d(TAG, "play");
                audioPlayer.play();
                if (videoPlayer != null) {
                    videoPlayer.pause();
                }
                result.success(null);
                break;
            case "pause":
//                Log.d(TAG, "pause");
                audioPlayer.pause();
                result.success(null);
                break;
            case "seekTo":
                //noinspection ConstantConditions
                int position = call.argument("position");
                audioPlayer.seekTo(position);
                result.success(null);
                break;
            case "skipToNext":
                audioPlayer.skipToNext();
                result.success(null);
                break;
            case "skipToPrevious":
                audioPlayer.skipToPrevious();
                result.success(null);
                break;
            case "skipToIndex": {
                //noinspection ConstantConditions
                int index = call.argument("index");
                audioPlayer.skipToIndex(index);
                result.success(null);
                break;
            }
            case "setRepeatMode": {
                //noinspection ConstantConditions
                int repeatMode = call.argument("repeatMode");
                if(repeatMode == Player.REPEAT_MODE_OFF || repeatMode == Player.REPEAT_MODE_ONE || repeatMode == Player.REPEAT_MODE_ALL) {
                    audioPlayer.setRepeatMode(repeatMode);
                    result.success(null);
                }
                else {
                    result.error("Set Repeat", "Repeat value is " + repeatMode, null);
                }
                break;
            }
            case "setShuffleModeEnabled": {
                //noinspection ConstantConditions
                boolean shuffleModeEnabled = call.argument("shuffleModeEnabled");
                audioPlayer.setShuffleModeEnabled(shuffleModeEnabled);
                result.success(null);
                break;
            }
            case "stop":
                audioPlayer.stop();
                result.success(null);
                break;
            case "release":
                audioPlayer.release();
                result.success(null);
                break;
            case "playNext": {
                Map<String, String> stringMap = call.arguments();
                Song song = Song.fromMap(stringMap);
                if(song != null) {
                    result.success(audioPlayer.playNext(song));
                }
                else {
                    result.error("Song key", "Song key is not found", "Song key is not found");
                }
                break;
            }
            case "addSongAtIndex": {
                //noinspection ConstantConditions
                int index = call.argument("index");
                //noinspection ConstantConditions
                int shouldPlay = call.argument("shouldPlay");

                Map<String, String> stringMap = call.arguments();
                Song song = Song.fromMap(stringMap);
                if(song != null) {
                    boolean isAdded = audioPlayer.addSongAtIndex(index, song, shouldPlay == 1);
                    result.success(isAdded);
                }else {
                    result.error("Song key", "Song key is not found", "Song key is not found");
                }
                break;
            }
            case "removeSongFromIndex": {
                Map<String, String> stringMap = call.arguments();
                Song song = Song.fromMap(stringMap);

                if(song != null) {
                    //noinspection ConstantConditions
                    int index = call.argument("index");
                    boolean isRemoved = audioPlayer.removeSongFromIndex(song, index);
                    result.success(isRemoved);
                }
                else {
                    result.error("Song key", "Song key is not found", "Song key is not found");
                }
                break;
            }
            case "setPlaylist": {
                String playlistStr = call.argument("playlist");

                try {
                    JSONObject playlistJsonObject = new JSONObject(playlistStr);
                    audioPlayer.setPlaylist(playlistJsonObject, result);
                } catch (JSONException e) {
                    e.printStackTrace();
                    result.error("Set Playlist", e.getMessage(), null);
                }
                break;
            }
            default:
                result.notImplemented();
                break;
        }
    }

    private void videoMethodCall(String method, MethodCall call, Result result) {
        switch (method) {
            case "addAndPlay": {
                String url = call.argument("url");
                String asset = call.argument("asset");
                if (url == null && asset != null) {
                    String assetLookupKey = registrar.lookupKeyForAsset(asset);
                    Log.d(TAG, "asset : " + assetLookupKey);
                    videoPlayer.addAndPlay("assets:///" + assetLookupKey, registrar.textures());
                } else if (url != null && asset == null) {
                    videoPlayer.addAndPlay(url, registrar.textures());
                }
                result.success(null);
                break;
            }
            case "play":
                videoPlayer.play();
                if (audioPlayer != null) {
                    audioPlayer.pause();
                }
                result.success(null);
                break;
            case "pause":
                videoPlayer.pause();
                result.success(null);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void downloadMethodCall(String method, MethodCall call, Result result) {
        switch (method) {
            case "download": {
                Log.d(TAG, "Download tap");
                String url = call.argument("url");
                if (url != null) {
                    downloadManager.startDownload(registrar.activeContext(), url, Uri.parse(url));
                }
                result.success(null);
                break;
            }
            case "downloadRemove": {
                String url = call.argument("url");
                if (url != null) {
                    downloadManager.removeDownload(registrar.activeContext(), url);
                }
                result.success(null);
                break;
            }
            case "isDownloaded":
                String url = call.argument("url");
                if (url != null) {
                    result.success(downloadManager.isDownloaded(Uri.parse(url)));
                } else {
                    result.success(false);
                }
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private MethodTypeCall parseMethodName(@NonNull String methodName) {
        try {
            Matcher matcher = METHOD_NAME_MATCH.matcher(methodName);

            if (matcher.matches() && matcher.groupCount() >= 2) {
                String mediaType = matcher.group(1);
                String command = matcher.group(2);
                return new MethodTypeCall(mediaType, command);
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return new MethodTypeCall(null, methodName);
    }

    private static class MethodTypeCall {
        final String methodType;
        final String method;

        private MethodTypeCall(String methodType, @NonNull String method) {
            this.methodType = methodType;
            this.method = method;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("MethodTypeCall - methodType %s, method: %s", methodType, method);
        }
    }
}
