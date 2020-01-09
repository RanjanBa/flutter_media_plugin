package com.example.fluttermediaplugin;

import androidx.annotation.NonNull;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.offline.Download;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.TextureRegistry;

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

    private com.google.android.exoplayer2.offline.DownloadManager.Listener exoPlayerDownloadManagerListener;

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
        videoPlayer = new VideoPlayer(registrar.activeContext(), channel);
    }


    private com.google.android.exoplayer2.offline.DownloadManager.Listener getExoPlayerDownloadManagerListener() {
        return new com.google.android.exoplayer2.offline.DownloadManager.Listener() {
            @Override
            public void onInitialized(com.google.android.exoplayer2.offline.DownloadManager downloadManager) {
                List<Download> downloads = downloadManager.getCurrentDownloads();
            }

            @Override
            public void onDownloadChanged(com.google.android.exoplayer2.offline.DownloadManager downloadManager, Download download) {
                Log.d(TAG, "on download changed : " + download.state);
                Map<String, Object> args = new HashMap<>();
                args.put("url", download.request.uri.toString());
                args.put("state", download.state);
                channel.invokeMethod(DOWNLOAD_METHOD_TYPE + "/onDownloadChanged", args);
            }


        };
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
            instance.downloadManager.removeDownloadListener(instance.exoPlayerDownloadManagerListener);
            instance.exoPlayerDownloadManagerListener = instance.getExoPlayerDownloadManagerListener();
            instance.downloadManager.addDownloadListener(instance.exoPlayerDownloadManagerListener);
        } else {
            instance.downloadManager = new DownloadManager(instance.getRegistrar().activeContext());
            instance.exoPlayerDownloadManagerListener = instance.getExoPlayerDownloadManagerListener();
            instance.downloadManager.addDownloadListener(instance.exoPlayerDownloadManagerListener);
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
                        result.success(null);
                    } else {
                        Log.d(TAG, "Already audioPlayer is initialized");
                        audioPlayer.initialize(result);
                    }
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
                        Log.d(TAG, "Already audioPlayer is initialized");
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
                    downloadManager = new DownloadManager(getRegistrar().activeContext());
                    exoPlayerDownloadManagerListener = getExoPlayerDownloadManagerListener();
                    downloadManager.addDownloadListener(exoPlayerDownloadManagerListener);
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
                Log.d(TAG, "play");
                audioPlayer.play();
                if (videoPlayer != null) {
                    videoPlayer.pause();
                }
                result.success(null);
                break;
            case "pause":
                Log.d(TAG, "pause");
                audioPlayer.pause();
                result.success(null);
                break;
            case "seekTo":
                //noinspection ConstantConditions
                int position = call.argument("position");
                audioPlayer.seekTo(position);
                result.success(null);
                break;
            case "addAndPlay": {
                String key = call.argument(Song.song_key_tag);
                String title = call.argument(Song.song_title_tag);
                String artists = call.argument(Song.song_artists_tag);
                String album = call.argument(Song.song_album_tag);
                String album_art_url = call.argument(Song.song_album_art_url_tag);
                String url = call.argument(Song.song_url_tag);
                Log.d(TAG, "url : " + url);
                Song song = new Song(key, title, artists, album, album_art_url, url);
                audioPlayer.addAndPlay(song);
                audioPlayer.play();
                result.success(null);
                break;
            }
            case "addSong": {
                String key = call.argument(Song.song_key_tag);
                String title = call.argument(Song.song_title_tag);
                String artists = call.argument(Song.song_artists_tag);
                String album = call.argument(Song.song_album_tag);
                String album_art_url = call.argument(Song.song_album_art_url_tag);
                String url = call.argument(Song.song_url_tag);
                Song song = new Song(key, title, artists, album, album_art_url, url);
                audioPlayer.addSong(song);
                result.success(null);
                break;
            }
            case "playNext": {
                String key = call.argument(Song.song_key_tag);
                String title = call.argument(Song.song_title_tag);
                String artists = call.argument(Song.song_artists_tag);
                String album = call.argument(Song.song_album_tag);
                String album_art_url = call.argument(Song.song_album_art_url_tag);
                String url = call.argument(Song.song_url_tag);
                Song song = new Song(key, title, artists, album, album_art_url, url);
                audioPlayer.playNext(song);
                result.success(null);
                break;
            }
            case "addSongAtIndex": {
                //noinspection ConstantConditions
                int index = call.argument("index");
                String key = call.argument(Song.song_key_tag);
                String title = call.argument(Song.song_title_tag);
                String artists = call.argument(Song.song_artists_tag);
                String album = call.argument(Song.song_album_tag);
                String album_art_url = call.argument(Song.song_album_art_url_tag);
                String url = call.argument(Song.song_url_tag);
                Song song = new Song(key, title, artists, album, album_art_url, url);
                audioPlayer.addSongAtIndex(index, song);
                result.success(null);
                break;
            }
            case "removeSong": {
                String key = call.argument(Song.song_key_tag);
                String title = call.argument(Song.song_title_tag);
                String artists = call.argument(Song.song_artists_tag);
                String album = call.argument(Song.song_album_tag);
                String album_art_url = call.argument(Song.song_album_art_url_tag);
                String url = call.argument(Song.song_url_tag);
                Song song = new Song(key, title, artists, album, album_art_url, url);
                audioPlayer.removeSong(song);
                result.success(null);
                break;
            }
            case "setPlaylist": {
                String playlistStr = call.argument("playlist");
                audioPlayer.setPlaylist(playlistStr);
                audioPlayer.preparePlaylist();
                result.success(null);
                break;
            }
            case "getPlaylist":
                Log.d(TAG, "Json onPlaylistChanged");
                JSONObject jsonObject = Playlist.toJson(audioPlayer.getPlaylist());
                if (jsonObject != null) {
                    String playlistJson = jsonObject.toString();
                    result.success(playlistJson);
                } else {
                    Log.d(TAG, "Json object playlist is null");
                    result.error("Playlist Object", "Json object playlist is null", null);
                }
                break;
            case "clearPlaylist":
                audioPlayer.clearPlaylist();
                result.success(null);
                break;
            case "setRepeatMode": {
                //noinspection ConstantConditions
                int repeatMode = call.argument("repeatMode");
                audioPlayer.setRepeatMode(repeatMode);
                result.success(null);
                break;
            }
            case "getRepeatMode": {
                int repeatMode = audioPlayer.getRepeatMode();
                result.success(repeatMode);
                break;
            }
            case "setShuffleModeEnabled": {
                //noinspection ConstantConditions
                boolean shuffleModeEnabled = call.argument("shuffleModeEnabled");
                audioPlayer.setShuffleModeEnabled(shuffleModeEnabled);
                result.success(null);
                break;
            }
            case "getShuffleModeEnabled": {
                boolean shuffleModeEnabled = audioPlayer.getShuffleModeEnabled();
                result.success(shuffleModeEnabled);
                break;
            }
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
            case "stop":
                audioPlayer.stop();
                result.success(null);
                break;
            case "release":
                audioPlayer.release();
                result.success(null);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void videoMethodCall(String method, MethodCall call, Result result) {
        switch (method) {
            case "addAndPlay": {
                TextureRegistry textures = registrar.textures();
                if (textures == null) {
                    result.error("no_activity", "video_player plugin requires a foreground activity", null);
                    return;
                }

                TextureRegistry.SurfaceTextureEntry handle = textures.createSurfaceTexture();
                if (handle == null) {
                    return;
                }

                String uri = call.argument("uri");
                String asset = call.argument("asset");
                if (uri == null && asset != null) {
                    String assetLookupKey = registrar.lookupKeyForAsset(asset);
                    Log.d(TAG, "asset : " + assetLookupKey);
                    videoPlayer.addAndPlay("assets:///" + assetLookupKey, handle);
                } else if (uri != null && asset == null) {
                    videoPlayer.addAndPlay(uri, handle);
                }
                result.success(null);
                break;
            }
            case "initSetTexture": {
                TextureRegistry textures = registrar.textures();
                if (textures == null) {
                    result.error("no_activity", "video_player plugin requires a foreground activity", null);
                    return;
                }

                TextureRegistry.SurfaceTextureEntry handle = textures.createSurfaceTexture();
                if (handle != null) {
                    videoPlayer.setupVideoPlayer(handle);
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
