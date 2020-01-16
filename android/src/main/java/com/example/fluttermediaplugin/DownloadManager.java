package com.example.fluttermediaplugin;

import android.content.Context;
import android.net.Uri;

import com.example.fluttermediaplugin.Media.Media;
import com.example.fluttermediaplugin.Media.Song;
import com.example.fluttermediaplugin.Media.Video;
import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.offline.ActionFileUpgradeUtil;
import com.google.android.exoplayer2.offline.DefaultDownloadIndex;
import com.google.android.exoplayer2.offline.DefaultDownloaderFactory;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadCursor;
import com.google.android.exoplayer2.offline.DownloadIndex;
import com.google.android.exoplayer2.offline.DownloadRequest;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper;
import com.google.android.exoplayer2.offline.StreamKey;
import com.google.android.exoplayer2.scheduler.Requirements;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodChannel;

import static com.example.fluttermediaplugin.FlutterMediaPlugin.DOWNLOAD_METHOD_TYPE;
import static com.example.fluttermediaplugin.Utility.Constants.DOWNLOAD_ACTION_FILE;
import static com.example.fluttermediaplugin.Utility.Constants.DOWNLOAD_CONTENT_DIRECTORY;
import static com.example.fluttermediaplugin.Utility.Constants.DOWNLOAD_TRACKER_ACTION_FILE;
import static com.example.fluttermediaplugin.Utility.MediaIds.MEDIA_TYPE;
import static com.example.fluttermediaplugin.Utility.MediaIds.SONG_MEDIA_TAG;
import static com.example.fluttermediaplugin.Utility.MediaIds.VIDEO_MEDIA_TAG;

final class DownloadManager {
    private static final String TAG = "DownloadManager";

    private static Cache downloadCache;
    private static DatabaseProvider databaseProvider;
    private static File downloadDirectory;

    private static synchronized DatabaseProvider getDatabaseProvider(Context context) {
        if (databaseProvider == null) {
            databaseProvider = new ExoDatabaseProvider(context);
        }

        return databaseProvider;
    }

    private static synchronized File getDownloadDirectory(Context context) {
        if (downloadDirectory == null) {
            downloadDirectory = context.getExternalFilesDir(null);
            if (downloadDirectory == null) {
                downloadDirectory = context.getFilesDir();
            }
        }
        return downloadDirectory;
    }

    private static synchronized HttpDataSource.Factory buildHttpDataSourceFactory(Context context) {
        return new DefaultHttpDataSourceFactory(DownloadManager.getUserAgent(context));
    }

    private static synchronized String getUserAgent(Context context) {
        return Util.getUserAgent(context, "ExoPlayer Download");
    }

    private static synchronized void upgradeActionFile(
            Context context, String fileName, DefaultDownloadIndex downloadIndex, boolean addNewDownloadsAsCompleted) {
        try {
            ActionFileUpgradeUtil.upgradeAndDelete(
                    new File(getDownloadDirectory(context), fileName),
                    null,
                    downloadIndex,
                    true,
                    addNewDownloadsAsCompleted);
        } catch (IOException e) {
            Log.e(TAG, "Failed to upgrade action file: " + fileName, e);
        }
    }

    static synchronized Cache getDownloadCache(Context context) {
        if (downloadCache == null) {
            File downloadContentDirectory = new File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY);
            downloadCache =
                    new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor(), getDatabaseProvider(context));
        }
        return downloadCache;
    }

    private com.google.android.exoplayer2.offline.DownloadManager exoPlayerDownloadManager;
    private final HashMap<String, Download> downloads;
    private final DownloadManagerListener downloadManagerListener;
    private MethodChannel channel;
    private Context context;

    DownloadManager(Context context, final MethodChannel channel) {
        this.context = context;
        this.channel = channel;
        downloads = new HashMap<>();
        exoPlayerDownloadManager = getExoPlayerDownloadManager(context);

        DownloadIndex downloadIndex = exoPlayerDownloadManager.getDownloadIndex();
        loadDownloads(downloadIndex);

        downloadManagerListener = new DownloadManagerListener();
        exoPlayerDownloadManager.addListener(new com.google.android.exoplayer2.offline.DownloadManager.Listener() {
                                                 @Override
                                                 public void onInitialized(com.google.android.exoplayer2.offline.DownloadManager downloadManager) {
                                                     downloadManagerListener.onInitialized();
                                                 }

                                                 @Override
                                                 public void onIdle(com.google.android.exoplayer2.offline.DownloadManager downloadManager) {

                                                 }

                                                 @Override
                                                 public void onRequirementsStateChanged(com.google.android.exoplayer2.offline.DownloadManager downloadManager, Requirements requirements, int notMetRequirements) {

                                                 }

                                                 @Override
                                                 public void onDownloadChanged(com.google.android.exoplayer2.offline.DownloadManager downloadManager, Download download) {
                                                     if (download.state == Download.STATE_DOWNLOADING || download.state == Download.STATE_COMPLETED) {
                                                         if (!downloads.containsKey(download.request.id)) {
                                                             downloads.put(download.request.id, download);
                                                         }
                                                         downloadManagerListener.onDownloadChangedOrAdded(download);
                                                     } else {
                                                         downloads.remove(download.request.id);
                                                         downloadManagerListener.onDownloadRemoved(download);
                                                     }
                                                 }

                                                 @Override
                                                 public void onDownloadRemoved(com.google.android.exoplayer2.offline.DownloadManager downloadManager, Download download) {
                                                     downloads.remove(download.request.id);
                                                     downloadManagerListener.onDownloadRemoved(download);
                                                 }
                                             }
        );

        try {
            DownloadService.start(FlutterMediaPlugin.getInstance().getRegistrar().activeContext(), MediaDownloadService.class);
        } catch (IllegalStateException e) {
            DownloadService.startForeground(FlutterMediaPlugin.getInstance().getRegistrar().activeContext(), MediaDownloadService.class);
        }
    }

    private void loadDownloads(DownloadIndex downloadIndex) {
        try (DownloadCursor downloadCursor = downloadIndex.getDownloads()) {
            while (downloadCursor.moveToNext()) {
                Download download = downloadCursor.getDownload();
                downloads.put(download.request.id, download);
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to query downloads", e);
        }
    }

    private void startDownload(String id, Uri url) {
        DownloadRequest downloadRequest = new DownloadRequest(id, DownloadRequest.TYPE_PROGRESSIVE, url, Collections.<StreamKey>emptyList(), null, null);
        DownloadService.sendAddDownload(context, MediaDownloadService.class, downloadRequest, true);
    }

    private void removeDownload(String id) {
        DownloadService.sendRemoveDownload(context, MediaDownloadService.class, id, true);
    }

    void setChannel(final MethodChannel channel) {
        this.channel = channel;
    }

    com.google.android.exoplayer2.offline.DownloadManager getExoPlayerDownloadManager(Context context) {
        if (exoPlayerDownloadManager == null) {
            DefaultDownloadIndex downloadIndex = new DefaultDownloadIndex(DownloadManager.getDatabaseProvider(context));
            upgradeActionFile(context,
                    DOWNLOAD_ACTION_FILE, downloadIndex, false);
            upgradeActionFile(context,
                    DOWNLOAD_TRACKER_ACTION_FILE, downloadIndex, true);
            DownloaderConstructorHelper downloaderConstructorHelper =
                    new DownloaderConstructorHelper(getDownloadCache(context), buildHttpDataSourceFactory(context));
            exoPlayerDownloadManager =
                    new com.google.android.exoplayer2.offline.DownloadManager(
                            context, downloadIndex, new DefaultDownloaderFactory(downloaderConstructorHelper));
        }

        return exoPlayerDownloadManager;
    }

    void startDownload(Media media) {
        try {
            JSONObject json = new JSONObject(media.toMap());
            if (media instanceof Song) {
                json.put(MEDIA_TYPE, SONG_MEDIA_TAG);
            } else if (media instanceof Video) {
                json.put(MEDIA_TYPE, VIDEO_MEDIA_TAG);
            }

            String id = json.toString();
            startDownload(id, Uri.parse(media.getUrl()));
//            Log.d(TAG, "Start download" + id);
        } catch (JSONException e) {
            Log.d(TAG, "can't parse media to map object");
        }
    }

    void removeDownload(Media media) {
        try {
            JSONObject json = new JSONObject(media.toMap());
            if (media instanceof Song) {
                json.put(MEDIA_TYPE, SONG_MEDIA_TAG);
            } else if (media instanceof Video) {
                json.put(MEDIA_TYPE, VIDEO_MEDIA_TAG);
            }

            String id = json.toString();
            Log.d(TAG, "remove media : " + id);
            removeDownload(id);
        } catch (JSONException e) {
            Log.d(TAG, "can't parse media to map object. " + e.getMessage());
        }
    }

    boolean isDownloaded(Media media) {
        try {
            JSONObject json = new JSONObject(media.toMap());
            if (media instanceof Song) {
                json.put(MEDIA_TYPE, SONG_MEDIA_TAG);
            } else if (media instanceof Video) {
                json.put(MEDIA_TYPE, VIDEO_MEDIA_TAG);
            }
            String id = json.toString();
            Download download = downloads.get(id);

            return download != null && download.state != Download.STATE_FAILED;
        } catch (JSONException e) {
            Log.d(TAG, "can't parse media to map object. " + e.getMessage());
        }

        return false;
    }

    void updateDownloads(List<Download> downloads) {
        for (Download download: downloads) {
            downloadManagerListener.onDownloadChangedOrAdded(download);
        }
    }

    private class DownloadManagerListener {
        void onInitialized() {
            ArrayList<Map<String, Object>> args = new ArrayList<>();

            for (Map.Entry<String, Download> download : downloads.entrySet()) {
                try {
                    Map<String, Object> songMap = new HashMap<>();

                    JSONObject jsonObject = new JSONObject(download.getKey());

                    Iterator<String> keys = jsonObject.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String value = (String) jsonObject.get(key);
                        songMap.put(key, value);
                    }

                    songMap.put("state", download.getValue().state);
                    songMap.put("percent", download.getValue().getPercentDownloaded());
                    songMap.put("bytesDownloaded", download.getValue().getBytesDownloaded());

                    args.add(songMap);
                } catch (JSONException throwable) {
                    Log.e(TAG, "Could not parse malformed JSON: \"" + throwable.getMessage() + "\"");
                }
            }

            channel.invokeMethod(DOWNLOAD_METHOD_TYPE + "/onInitialized", args);
        }

        void onDownloadChangedOrAdded(Download download) {
            try {
                Map<String, String> songMap = new HashMap<>();

                JSONObject jsonObject = new JSONObject(download.request.id);

                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = (String) jsonObject.get(key);
                    songMap.put(key, value);
                }

                Map<String, Object> args = new HashMap<>();
                args.put("song", songMap);
                args.put("state", download.state);
                args.put("percent", download.getPercentDownloaded());
                args.put("bytesDownloaded", download.getBytesDownloaded());

                channel.invokeMethod(DOWNLOAD_METHOD_TYPE + "/onDownloadChangedOrAdded", args);
            } catch (JSONException e) {
                Log.e(TAG, "Could not parse malformed JSON: \"" + e.getMessage() + "\"");
            }
        }

        // called when download is removed with success
        void onDownloadRemoved(Download download) {
            try {
                Map<String, String> songMap = new HashMap<>();

                JSONObject jsonObject = new JSONObject(download.request.id);

                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = (String) jsonObject.get(key);
                    songMap.put(key, value);
                }

                channel.invokeMethod(DOWNLOAD_METHOD_TYPE + "/onDownloadRemoved", songMap);
            } catch (JSONException e) {
                Log.e(TAG, "Could not parse malformed JSON: \"" + e.getMessage() + "\"");
            }
        }
    }
}
