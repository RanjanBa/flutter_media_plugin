package com.example.fluttermediaplugin;

import android.content.Context;

import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.offline.ActionFileUpgradeUtil;
import com.google.android.exoplayer2.offline.DefaultDownloadIndex;
import com.google.android.exoplayer2.offline.DefaultDownloaderFactory;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.io.IOException;

import static com.example.fluttermediaplugin.StaticConst.DOWNLOAD_ACTION_FILE;
import static com.example.fluttermediaplugin.StaticConst.DOWNLOAD_CONTENT_DIRECTORY;
import static com.example.fluttermediaplugin.StaticConst.DOWNLOAD_TRACKER_ACTION_FILE;

final class DownloadUtility {
    private static final String TAG = "DownloadUtility";

    private static Cache downloadCache;
    private static DatabaseProvider databaseProvider;
    private static File downloadDirectory;
    private static DownloadManager downloadManager;

    static synchronized Cache getDownloadCache(Context context) {
        if (downloadCache == null) {
            File downloadContentDirectory = new File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY);
            downloadCache =
                    new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor(), getDatabaseProvider(context));
        }
        return downloadCache;
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

    static synchronized DownloadManager getDownloadManager(Context context) {
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
}
