package com.example.fluttermediaplugin;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.View;

import com.google.android.exoplayer2.Player;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.HashMap;
import java.util.Objects;

public class MediaPlayerNotificationService extends Service {
    private static final String TAG = "MediaPlayerNotification";
    private PlayerNotificationManager playerNotificationManager;

    private static MediaPlayerNotificationService instance;

    private static HashMap<String, Bitmap> albumArts = new HashMap<>();
    private MediaSessionCompat mediaSession;

    private static Bitmap getBitmap(String key) {
        if (albumArts.containsKey(key)) {
            return albumArts.get(key);
        }
        return null;
    }

    private boolean isServiceStarted = false;

    public static MediaPlayerNotificationService getInstance() {
        return instance;
    }

    public PlayerNotificationManager getPlayerNotificationManager() {
        return playerNotificationManager;
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        Log.d(TAG, "Create media notification player");
    }

    private void instantiateNotification() {
        final Context context = FlutterMediaPlugin.getInstance().getRegistrar().activeContext();
        if (mediaSession == null) {
            mediaSession = new MediaSessionCompat(context, com.example.fluttermediaplugin.C.MEDIA_SESSION_TAG);
            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        }

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                FlutterMediaPlugin.getInstance().getRegistrar().context(), com.example.fluttermediaplugin.C.PLAYBACK_CHANNEL_ID, R.string.exo_track_unknown, com.example.fluttermediaplugin.C.PLAYBACK_NOTIFICATION_ID, new PlayerNotificationManager.MediaDescriptionAdapter() {

                    @Override
                    public String getCurrentContentTitle(Player player) {
                        Song song = FlutterMediaPlugin.getInstance().getAudioPlayer().getSongByIndex(player.getCurrentWindowIndex());
                        if (song == null) {
                            stopSelf();
                            return "No Name";
                        }
                        return song.getTitle();
                    }

                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        String packageName = FlutterMediaPlugin.getInstance().getRegistrar().context().getPackageName();
                        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                        try {
                            assert launchIntent != null;
                            String className = Objects.requireNonNull(launchIntent.getComponent()).getClassName();
                            Intent intent = new Intent(context, Class.forName(className));
                            return PendingIntent.getActivity(context, 0, intent, 0);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }

                    @Override
                    public String getCurrentContentText(Player player) {
                        Song song = FlutterMediaPlugin.getInstance().getAudioPlayer().getSongByIndex(player.getCurrentWindowIndex());
                        if (song == null) {
                            return "No Artist";
                        }
                        return song.getArtist();
                    }

                    @Override
                    public String getCurrentSubText(Player player) {
                        //Song song = FlutterMediaPlugin.getInstance().getAudioPlayer().getSongByIndex(player.getCurrentWindowIndex());
                        if (player == null) {
                            return "0.00";
                        }
                        String str;

                        long minute = (player.getCurrentPosition() / 1000) / 60;
                        long second = (player.getCurrentPosition() / 1000) % 60;

                        str = minute + ":" + (second < 10 ? "0" : "") + second;

                        return str;
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        Song song = FlutterMediaPlugin.getInstance().getAudioPlayer().getSongByIndex(player.getCurrentWindowIndex());
                        String uri;
                        if (song != null) {
                            uri = song.getAlbumArtUri();
                        } else {
                            return ((BitmapDrawable) context.getResources().getDrawable(android.R.drawable.ic_dialog_dialer)).getBitmap();
                        }

                        Bitmap bitmap = getBitmap(uri);
                        if (bitmap == null) {
                            loadImageAsync(uri, callback);
                            bitmap = ((BitmapDrawable) context.getResources().getDrawable(android.R.drawable.ic_dialog_dialer)).getBitmap();
                        }
                        return bitmap;
                    }
                }
        );

        playerNotificationManager.setNotificationListener(new PlayerNotificationManager.NotificationListener() {
            @Override
            public void onNotificationStarted(int notificationId, Notification notification) {
                Log.d(TAG, "Notification started");
                startForeground(notificationId, notification);
                isServiceStarted = true;
                stopService(false);
                FlutterMediaPlugin.getInstance().getAudioPlayer().onNotificationStarted();
            }

            @Override
            public void onNotificationCancelled(int notificationId) {
                Log.d(TAG, "Notification canceled");
                FlutterMediaPlugin.getInstance().getAudioPlayer().onNotificationDestroyed();
                stopSelf();
            }
        });
        playerNotificationManager.setPlayer(FlutterMediaPlugin.getInstance().getAudioPlayer().getSimpleExoPlayer());
        mediaSession.setActive(true);
        playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());
    }

    private void loadImageAsync(String uri, final PlayerNotificationManager.BitmapCallback callback) {
        ImageLoader.getInstance().loadImage(uri, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                super.onLoadingComplete(imageUri, view, loadedImage);
                Log.d(TAG, "Image loader completed");
                if (loadedImage == null) {
                    Log.d(TAG, "Image loaded is null");
                } else {
                    albumArts.put(imageUri, loadedImage);
                    callback.onBitmap(loadedImage);
                    Log.d(TAG, "Image loaded is not null");
                }
            }
        });
    }

    public void stopService(boolean removeNotification) {
        if (isServiceStarted) {
            stopForeground(removeNotification);
        }
        if (removeNotification) {
            if (playerNotificationManager != null) {
                playerNotificationManager.setPlayer(null);
            }
            playerNotificationManager = null;
            mediaSession = null;
            instance = null;
            if (albumArts != null) {
                albumArts.clear();
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service is destroyed");
        isServiceStarted = false;
        if (playerNotificationManager != null) {
            playerNotificationManager.setPlayer(null);
        }
        playerNotificationManager = null;
        mediaSession = null;
        instance = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "on start command audio player");
        instantiateNotification();
        return START_STICKY;
    }

}
