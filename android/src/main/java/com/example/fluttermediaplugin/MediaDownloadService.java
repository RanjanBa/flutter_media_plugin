package com.example.fluttermediaplugin;

import android.app.Notification;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.scheduler.PlatformScheduler;
import com.google.android.exoplayer2.scheduler.Scheduler;
import com.google.android.exoplayer2.util.NotificationUtil;
import com.google.android.exoplayer2.util.Util;
import android.app.PendingIntent;
import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import com.google.android.exoplayer2.C;
import java.util.List;

import static com.example.fluttermediaplugin.Utility.Constants.DOWNLOAD_CHANNEL_ID;
import static com.example.fluttermediaplugin.Utility.Constants.DOWNLOAD_NOTIFICATION_ID;

public class MediaDownloadService extends DownloadService {
    private final class DownloadNotificationHelper {

        private static final @StringRes
        int NULL_STRING_ID = 0;

        private final Context context;
        private final NotificationCompat.Builder notificationBuilder;

        /**
         * @param context   A context.
         * @param channelId The id of the notification channel to use.
         */
        DownloadNotificationHelper(Context context, String channelId) {
            context = context.getApplicationContext();
            this.context = context;
            this.notificationBuilder = new NotificationCompat.Builder(context, channelId);
        }

        /**
         * Returns a progress notification for the given downloads.
         *
         * @param smallIcon     A small icon for the notification.
         * @param contentIntent An optional content intent to send when the notification is clicked.
         * @param message       An optional message to display on the notification.
         * @param downloads     The downloads.
         * @return The notification.
         */
        Notification buildProgressNotification(
                @DrawableRes int smallIcon,
                @Nullable PendingIntent contentIntent,
                @Nullable String message,
                List<Download> downloads) {
            float totalPercentage = 0;
            int downloadTaskCount = 0;
            boolean allDownloadPercentagesUnknown = true;
            boolean haveDownloadedBytes = false;
            boolean haveDownloadTasks = false;
            boolean haveRemoveTasks = false;
            for (int i = 0; i < downloads.size(); i++) {
                Download download = downloads.get(i);
                if (download.state == Download.STATE_REMOVING) {
                    haveRemoveTasks = true;
                    continue;
                }
                if (download.state != Download.STATE_RESTARTING
                        && download.state != Download.STATE_DOWNLOADING) {
                    continue;
                }
                haveDownloadTasks = true;
                float downloadPercentage = download.getPercentDownloaded();
                if (downloadPercentage != C.PERCENTAGE_UNSET) {
                    allDownloadPercentagesUnknown = false;
                    totalPercentage += downloadPercentage;
                }
                haveDownloadedBytes |= download.getBytesDownloaded() > 0;
                downloadTaskCount++;
            }

            int titleStringId =
                    haveDownloadTasks
                            ? R.string.exo_download_downloading
                            : (haveRemoveTasks ? R.string.exo_download_removing : NULL_STRING_ID);
            int progress = 0;
            boolean indeterminate = true;
            if (haveDownloadTasks) {
                progress = (int) (totalPercentage / downloadTaskCount);
                indeterminate = allDownloadPercentagesUnknown && haveDownloadedBytes;
            }
            return buildNotification(
                    smallIcon,
                    contentIntent,
                    message,
                    titleStringId,
                    /* maxProgress= */ 100,
                    progress,
                    indeterminate,
                    /* ongoing= */ true,
                    /* showWhen= */ false);
        }

        /**
         * Returns a notification for a completed download.
         *
         * @param smallIcon     A small icon for the notifications.
         * @param contentIntent An optional content intent to send when the notification is clicked.
         * @param message       An optional message to display on the notification.
         * @return The notification.
         */
        Notification buildDownloadCompletedNotification(
                @DrawableRes int smallIcon, @Nullable PendingIntent contentIntent, @Nullable String message) {
            int titleStringId = R.string.exo_download_completed;
            return buildEndStateNotification(smallIcon, contentIntent, message, titleStringId);
        }

        /**
         * Returns a notification for a failed download.
         *
         * @param smallIcon     A small icon for the notifications.
         * @param contentIntent An optional content intent to send when the notification is clicked.
         * @param message       An optional message to display on the notification.
         * @return The notification.
         */
        public Notification buildDownloadFailedNotification(
                @DrawableRes int smallIcon, @Nullable PendingIntent contentIntent, @Nullable String message) {
            @StringRes int titleStringId = R.string.exo_download_failed;
            return buildEndStateNotification(smallIcon, contentIntent, message, titleStringId);
        }

        private Notification buildEndStateNotification(
                @DrawableRes int smallIcon,
                @Nullable PendingIntent contentIntent,
                @Nullable String message,
                @StringRes int titleStringId) {
            return buildNotification(
                    smallIcon,
                    contentIntent,
                    message,
                    titleStringId,
                    /* maxProgress= */ 0,
                    /* currentProgress= */ 0,
                    /* indeterminateProgress= */ false,
                    /* ongoing= */ false,
                    /* showWhen= */ true);
        }

        private Notification buildNotification(
                @DrawableRes int smallIcon,
                @Nullable PendingIntent contentIntent,
                @Nullable String message,
                @StringRes int titleStringId,
                int maxProgress,
                int currentProgress,
                boolean indeterminateProgress,
                boolean ongoing,
                boolean showWhen) {
            notificationBuilder.setSmallIcon(smallIcon);
            notificationBuilder.setContentTitle(
                    titleStringId == NULL_STRING_ID ? null : context.getResources().getString(titleStringId));
            notificationBuilder.setContentIntent(contentIntent);
            notificationBuilder.setStyle(
                    message == null ? null : new NotificationCompat.BigTextStyle().bigText(message));
            notificationBuilder.setProgress(maxProgress, currentProgress, indeterminateProgress);
            notificationBuilder.setOngoing(ongoing);
            notificationBuilder.setShowWhen(showWhen);
            return notificationBuilder.build();
        }
    }

//    private static final String TAG = "MediaDownloadService";

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
        return FlutterMediaPlugin.getInstance().getDownloadManager().getExoPlayerDownloadManager(this);
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
        } else if (download.state == Download.STATE_FAILED) {
            notification = notificationHelper.buildDownloadFailedNotification(R.drawable.download_failed_icon, null, Util.fromUtf8Bytes(download.request.data));
        } else {
            return;
        }

        NotificationUtil.setNotification(this, nextNotificationId++, notification);
    }
}
