package com.example.fluttermediaplugin;

import android.app.Notification;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.scheduler.PlatformScheduler;
import com.google.android.exoplayer2.scheduler.Scheduler;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.NotificationUtil;
import com.google.android.exoplayer2.util.Util;

import java.util.List;

import static com.example.fluttermediaplugin.StaticConst.DOWNLOAD_CHANNEL_ID;
import static com.example.fluttermediaplugin.StaticConst.DOWNLOAD_NOTIFICATION_ID;

public class MediaDownloadService extends DownloadService {
    private static final String TAG = "MediaDownloadService";

    private static final int JOB_ID = 1000;
    private static final int FOREGROUND_NOTIFICATION_ID = DOWNLOAD_NOTIFICATION_ID;
    private static int nextNotificationId = FOREGROUND_NOTIFICATION_ID + 1;

    private DownloadNotificationHelper notificationHelper;

    public MediaDownloadService() {
        super(FOREGROUND_NOTIFICATION_ID, DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL, DOWNLOAD_CHANNEL_ID, R.string.exo_download_notification_channel_name);
    }

    @Override
    public void onCreate() {
//        Log.d(TAG, "on create");
        notificationHelper = new DownloadNotificationHelper(this, DOWNLOAD_CHANNEL_ID);
        super.onCreate();
    }

    @Override
    protected DownloadManager getDownloadManager() {
        return FlutterMediaPlugin.getInstance().getDownloadUtility().getDownloadManager(this);
    }

    @Nullable
    @Override
    protected Scheduler getScheduler() {
        return new PlatformScheduler(this, JOB_ID);
    }

    @Override
    protected Notification getForegroundNotification(List<Download> downloads) {
        return notificationHelper.buildProgressNotification(R.drawable.download, null, null, downloads);
    }

    @Override
    protected void onDownloadChanged(Download download) {
        Notification notification;
        if (download.state == Download.STATE_COMPLETED) {
            notification = notificationHelper.buildDownloadCompletedNotification(R.drawable.download_done, null, Util.fromUtf8Bytes(download.request.data));
        }
        else if(download.state == Download.STATE_FAILED) {
            notification = notificationHelper.buildDownloadFailedNotification(R.drawable.download_done, null, Util.fromUtf8Bytes(download.request.data));
        }
        else {
            return;
        }

        NotificationUtil.setNotification(this, nextNotificationId++, notification);
    }
}
