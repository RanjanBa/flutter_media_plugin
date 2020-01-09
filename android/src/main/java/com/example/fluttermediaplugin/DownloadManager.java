package com.example.fluttermediaplugin;

import android.content.Context;
import android.net.Uri;
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
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import static com.example.fluttermediaplugin.StaticConst.DOWNLOAD_ACTION_FILE;
import static com.example.fluttermediaplugin.StaticConst.DOWNLOAD_CONTENT_DIRECTORY;
import static com.example.fluttermediaplugin.StaticConst.DOWNLOAD_TRACKER_ACTION_FILE;

final class DownloadManager {
    private static final String TAG = "DownloadManager";

    private static Cache downloadCache;
    private static DatabaseProvider databaseProvider;
    private static File downloadDirectory;

    private com.google.android.exoplayer2.offline.DownloadManager exoplayerDownloadManager;
    private final HashMap<Uri, Download> downloads;
    private DownloadIndex downloadIndex;

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

    DownloadManager(Context context) {
        downloads = new HashMap<>();
        exoplayerDownloadManager = getExoPlayerDownloadManager(context);
        downloadIndex = exoplayerDownloadManager.getDownloadIndex();
        loadDownloads(downloadIndex);
        exoplayerDownloadManager.addListener(new com.google.android.exoplayer2.offline.DownloadManager.Listener() {
            @Override
            public void onDownloadChanged(com.google.android.exoplayer2.offline.DownloadManager downloadManager, Download download) {
                if(download.state == Download.STATE_DOWNLOADING || download.state == Download.STATE_COMPLETED) {
                    if(!downloads.containsKey(download.request.uri)) {
                        downloads.put(download.request.uri, download);
                    }
                }
                else {
                    downloads.remove(download.request.uri);
                }
            }

            @Override
            public void onDownloadRemoved(com.google.android.exoplayer2.offline.DownloadManager downloadManager, Download download) {
                downloads.remove(download.request.uri);
            }
        });

        try {
            DownloadService.start(FlutterMediaPlugin.getInstance().getRegistrar().activeContext(), MediaDownloadService.class);
        } catch (IllegalStateException e) {
            DownloadService.startForeground(FlutterMediaPlugin.getInstance().getRegistrar().activeContext(), MediaDownloadService.class);
        }
    }

    private void loadDownloads(DownloadIndex downloadIndex) {
        try (DownloadCursor downloadCursor = downloadIndex.getDownloads()) {
            while(downloadCursor.moveToNext()) {
                Download download = downloadCursor.getDownload();
                downloads.put(download.request.uri, download);
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to query downloads", e);
        }
    }

    com.google.android.exoplayer2.offline.DownloadManager getExoPlayerDownloadManager(Context context) {
        if (exoplayerDownloadManager == null) {
            DefaultDownloadIndex downloadIndex = new DefaultDownloadIndex(DownloadManager.getDatabaseProvider(context));
            upgradeActionFile(context,
                    DOWNLOAD_ACTION_FILE, downloadIndex, false);
            upgradeActionFile(context,
                    DOWNLOAD_TRACKER_ACTION_FILE, downloadIndex, true);
            DownloaderConstructorHelper downloaderConstructorHelper =
                    new DownloaderConstructorHelper(getDownloadCache(context), buildHttpDataSourceFactory(context));
            exoplayerDownloadManager =
                    new com.google.android.exoplayer2.offline.DownloadManager(
                            context, downloadIndex, new DefaultDownloaderFactory(downloaderConstructorHelper));
        }

        return exoplayerDownloadManager;
    }

    void addDownloadListener(com.google.android.exoplayer2.offline.DownloadManager.Listener listener) {
        exoplayerDownloadManager.addListener(listener);
    }

    void removeDownloadListener(com.google.android.exoplayer2.offline.DownloadManager.Listener listener) {
        exoplayerDownloadManager.removeListener(listener);
    }

    void startDownload(Context context,String id, Uri url) {
        DownloadRequest downloadRequest = new DownloadRequest(id, DownloadRequest.TYPE_PROGRESSIVE, url, Collections.<StreamKey>emptyList(), null, null);
        DownloadService.sendAddDownload(context, MediaDownloadService.class, downloadRequest, true);
    }

    void removeDownload(Context context, String id) {
        DownloadService.sendRemoveDownload(context, MediaDownloadService.class, id, true);
    }

    boolean isDownloaded(Uri uri) {
        Download download = downloads.get(uri);
        return download != null && download.state != Download.STATE_FAILED;
    }
}