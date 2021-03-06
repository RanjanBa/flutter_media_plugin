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

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import android.support.v4.media.session.MediaSessionCompat;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;

import com.example.fluttermediaplugin.Media.Song;
import com.google.android.exoplayer2.Player;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.example.fluttermediaplugin.Utility.Constants.MEDIA_SESSION_TAG;

public class MediaPlayerNotificationService extends Service {
    private static final String TAG = "MediaPlayerNotification";

    private static final String ACTION_HEART = "com.google.android.flutter_media.play";
    private static final String EXTRA_INSTANCE_ID = "INSTANCE_ID";
    private PlayerNotificationManager playerNotificationManager;

    private static MediaPlayerNotificationService instance;

    private static ArrayMap<String, Bitmap> albumArts = new ArrayMap<>();
    private MediaSessionCompat mediaSession;

    private static void putBitmap(String url, Bitmap bitmap) {
        if (albumArts.size() >= 10) {
            albumArts.removeAt(albumArts.size() - 1);
        }
        albumArts.put(url, bitmap);
    }

    private static Bitmap getBitmap(String key) {
        if (albumArts.containsKey(key)) {
            return albumArts.get(key);
        }
        return null;
    }

    private Notification notification;
    private int notificationId;

    public static MediaPlayerNotificationService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
//        Log.d(TAG, "Create media notification player");
    }

    private void instantiateNotification() {
        final Context context = FlutterMediaPlugin.getInstance().getRegistrar().activeContext();
        if (mediaSession == null) {
            mediaSession = new MediaSessionCompat(context, MEDIA_SESSION_TAG);
            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        }

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                FlutterMediaPlugin.getInstance().getRegistrar().context(), R.string.exo_channel_name, new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public String getCurrentContentTitle(Player player) {
                        Song song = FlutterMediaPlugin.getInstance().getAudioPlayer().getSongByIndex(player.getCurrentWindowIndex());
                        if (song == null) {
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
                            return ((BitmapDrawable) context.getResources().getDrawable(R.drawable.music_notification_icon)).getBitmap();
                        }

                        Bitmap bitmap = getBitmap(uri);
                        if (bitmap == null) {
                            loadImageAsync(uri, callback);
                            bitmap = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.music_notification_icon)).getBitmap();
                        }

                        return bitmap;
                    }
                }, new PlayerNotificationManager.CustomActionReceiver() {
                    @Override
                    public Map<String, NotificationCompat.Action> createCustomActions(Context context, int instanceId) {
                        Intent intent = new Intent(ACTION_HEART).setPackage(context.getPackageName());
                        intent.putExtra(EXTRA_INSTANCE_ID, instanceId);

                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                context, instanceId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        Map<String, NotificationCompat.Action> actions = new HashMap<>();
                        actions.put(
                                ACTION_HEART,
                                new NotificationCompat.Action(
                                        R.drawable.exo_icon_heart,
                                        context.getString(R.string.exo_controls_heart_description),
                                        pendingIntent));
                        return actions;
                    }

                    @Override
                    public List<String> getCustomActions(Player player) {
                        List<String> stringActions = new ArrayList<>();
                        stringActions.add(ACTION_HEART);
                        return stringActions;
                    }

                    @Override
                    public void onCustomAction(Player player, String action, Intent intent) {
                        if(ACTION_HEART.equals(action)) {
                            Log.d(TAG, "You love song");
                        }
                    }
                }
        );

        playerNotificationManager.setNotificationListener(new PlayerNotificationManager.NotificationListener() {
            @Override
            public void onNotificationStarted(int id, Notification not) {
//                Log.d(TAG, "Notification started");
                notificationId = id;
                notification = not;
                startService();
                FlutterMediaPlugin.getInstance().getAudioPlayer().onNotificationStarted();
            }

            @Override
            public void onNotificationCancelled(int notificationId) {
                Log.d(TAG, "Notification canceled");
                notification = null;
                FlutterMediaPlugin.getInstance().getAudioPlayer().onNotificationDestroyed();
                stopSelf();
            }
        });
        playerNotificationManager.setPlayer(FlutterMediaPlugin.getInstance().getAudioPlayer().getSimpleExoPlayer());
        playerNotificationManager.setUseChronometer(true);
        mediaSession.setActive(true);
        playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());
    }

    private void loadImageAsync(String uri, final PlayerNotificationManager.BitmapCallback callback) {
        ImageLoader.getInstance().loadImage(uri, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                super.onLoadingComplete(imageUri, view, loadedImage);
                if (loadedImage != null) {
                    putBitmap(imageUri, loadedImage);
                    callback.onBitmap(loadedImage);
                }
            }
        });
    }

    public void startService() {
        if (notification != null) {
            if (playerNotificationManager != null) {
                playerNotificationManager.setOngoing(true);
            }
            startForeground(notificationId, notification);
        }
    }

    public void stopService(boolean removeNotification) {
//        playerNotificationManager.setPriority();
        if (playerNotificationManager != null) {
            playerNotificationManager.setOngoing(false);
        }
        stopForeground(removeNotification);


        if (removeNotification) {
            if (playerNotificationManager != null) {
                playerNotificationManager.setPlayer(null);
            }
            playerNotificationManager = null;
            mediaSession = null;
            instance = null;
            albumArts.clear();
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service is destroyed");
        if (playerNotificationManager != null) {
            playerNotificationManager.setPlayer(null);
        }
        albumArts.clear();
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
//        Log.d(TAG, "on start method audio player");
        instantiateNotification();
        return START_STICKY;
    }

}
