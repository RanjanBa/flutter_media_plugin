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
import com.google.android.exoplayer2.offline.DownloadManager;
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

final class DownloadUtility {
    private static final String TAG = "DownloadUtility";

    private static Cache downloadCache;
    private static DatabaseProvider databaseProvider;
    private static File downloadDirectory;

    private DownloadManager downloadManager;
    private final HashMap<Uri, Download> downloads;
    private DownloadIndex downloadIndex;

    DownloadUtility(Context context) {
        downloads = new HashMap<>();
        downloadManager = getDownloadManager(context);
        downloadIndex = downloadManager.getDownloadIndex();

        loadDownloads();
        downloadManager.addListener(new DownloadManager.Listener() {
            @Override
            public void onDownloadChanged(DownloadManager downloadManager, Download download) {
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
            public void onDownloadRemoved(DownloadManager downloadManager, Download download) {
                downloads.remove(download.request.uri);
            }
        });

        try {
            DownloadService.start(FlutterMediaPlugin.getInstance().getRegistrar().activeContext(), MediaDownloadService.class);
        } catch (IllegalStateException e) {
            DownloadService.startForeground(FlutterMediaPlugin.getInstance().getRegistrar().activeContext(), MediaDownloadService.class);
        }
    }

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
        return new DefaultHttpDataSourceFactory(DownloadUtility.getUserAgent(context));
    }

    private static synchronized String getUserAgent(Context context) {
        return Util.getUserAgent(context, "ExoPlayer Download");
    }

    private static synchronized void upgradeActionFile(
            Context context, String fileName, DefaultDownloadIndex downloadIndex, boolean addNewDownloadsAsCompleted) {
        try {
            ActionFileUpgradeUtil.upgradeAndDelete(
                    new File(DownloadUtility.getDownloadDirectory(context), fileName),
                    null,
                    downloadIndex,
                    true,
                    addNewDownloadsAsCompleted);
        } catch (IOException e) {
            Log.e(TAG, "Failed to upgrade action file: " + fileName, e);
        }
    }

    private void loadDownloads() {
        try (DownloadCursor downloadCursor = downloadIndex.getDownloads()) {
            while(downloadCursor.moveToNext()) {
                Download download = downloadCursor.getDownload();
                downloads.put(download.request.uri, download);
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to query downloads", e);
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

    DownloadManager getDownloadManager(Context context) {
        if (downloadManager == null) {
            DefaultDownloadIndex downloadIndex = new DefaultDownloadIndex(DownloadUtility.getDatabaseProvider(context));
            upgradeActionFile(context,
                    DOWNLOAD_ACTION_FILE, downloadIndex, false);
            upgradeActionFile(context,
                    DOWNLOAD_TRACKER_ACTION_FILE, downloadIndex, true);
            DownloaderConstructorHelper downloaderConstructorHelper =
                    new DownloaderConstructorHelper(getDownloadCache(context), buildHttpDataSourceFactory(context));
            downloadManager =
                    new DownloadManager(
                            context, downloadIndex, new DefaultDownloaderFactory(downloaderConstructorHelper));
        }

        return downloadManager;
    }

    boolean isDownloaded(Uri uri) {
        Download download = downloads.get(uri);
        return download != null && download.state != Download.STATE_FAILED;
    }

    void startDownload(Context context,String id, Uri url) {
        DownloadRequest downloadRequest = new DownloadRequest(id, DownloadRequest.TYPE_PROGRESSIVE, url, Collections.<StreamKey>emptyList(), null, null);
        DownloadService.sendAddDownload(context, MediaDownloadService.class, downloadRequest, true);
    }

    void removeDownload(Context context, String id) {
        DownloadService.sendRemoveDownload(context, MediaDownloadService.class, id, true);
    }

}
