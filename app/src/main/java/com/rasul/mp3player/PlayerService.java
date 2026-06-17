package com.rasul.mp3player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import java.util.ArrayList;

public class PlayerService extends Service {
    public static final String ACTION_PLAY_INDEX = "com.rasul.mp3player.PLAY_INDEX";
    public static final String ACTION_TOGGLE = "com.rasul.mp3player.TOGGLE";
    public static final String ACTION_NEXT = "com.rasul.mp3player.NEXT";
    public static final String ACTION_PREV = "com.rasul.mp3player.PREV";
    public static final String ACTION_STOP = "com.rasul.mp3player.STOP";
    public static final String EXTRA_INDEX = "index";

    private static final String CHANNEL_ID = "playback";
    private static final int NOTIFICATION_ID = 7;

    private final ArrayList<Track> queue = new ArrayList<>();
    private MediaPlayer player;
    private int currentIndex = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        queue.clear();
        queue.addAll(TrackStore.load(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? "" : intent.getAction();
        if (ACTION_PLAY_INDEX.equals(action)) {
            playIndex(intent.getIntExtra(EXTRA_INDEX, 0));
        } else if (ACTION_TOGGLE.equals(action)) {
            toggle();
        } else if (ACTION_NEXT.equals(action)) {
            playNext();
        } else if (ACTION_PREV.equals(action)) {
            playPrevious();
        } else if (ACTION_STOP.equals(action)) {
            stopPlayback();
            stopSelf();
        } else if (currentIndex >= 0) {
            startForeground(NOTIFICATION_ID, buildNotification());
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void playIndex(int index) {
        queue.clear();
        queue.addAll(TrackStore.load(this));
        if (queue.isEmpty()) return;
        currentIndex = Math.max(0, Math.min(index, queue.size() - 1));
        Track track = queue.get(currentIndex);
        releasePlayer();
        player = new MediaPlayer();
        try {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            player.setDataSource(this, track.asUri());
            player.setOnCompletionListener(mp -> playNext());
            player.prepare();
            player.start();
            startForeground(NOTIFICATION_ID, buildNotification());
        } catch (Exception exception) {
            releasePlayer();
            stopForeground(true);
        }
    }

    private void toggle() {
        if (player == null) {
            if (currentIndex < 0) currentIndex = 0;
            playIndex(currentIndex);
            return;
        }
        if (player.isPlaying()) player.pause();
        else player.start();
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    private void playNext() {
        if (queue.isEmpty()) queue.addAll(TrackStore.load(this));
        if (queue.isEmpty()) return;
        int next = currentIndex < 0 ? 0 : (currentIndex + 1) % queue.size();
        playIndex(next);
    }

    private void playPrevious() {
        if (queue.isEmpty()) queue.addAll(TrackStore.load(this));
        if (queue.isEmpty()) return;
        int previous = currentIndex <= 0 ? queue.size() - 1 : currentIndex - 1;
        playIndex(previous);
    }

    private void stopPlayback() {
        releasePlayer();
        currentIndex = -1;
        stopForeground(true);
    }

    private void releasePlayer() {
        if (player == null) return;
        try {
            player.stop();
        } catch (Exception ignored) {}
        player.release();
        player = null;
    }

    private Notification buildNotification() {
        Track track = currentIndex >= 0 && currentIndex < queue.size()
                ? queue.get(currentIndex)
                : new Track("", "MP3 Player", "Музыка играет");

        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                1,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(track.title)
                .setContentText(track.artist)
                .setContentIntent(contentIntent)
                .setOngoing(player != null && player.isPlaying())
                .addAction(android.R.drawable.ic_media_previous, "Назад", serviceIntent(ACTION_PREV, 2))
                .addAction(player != null && player.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        player != null && player.isPlaying() ? "Пауза" : "Играть", serviceIntent(ACTION_TOGGLE, 3))
                .addAction(android.R.drawable.ic_media_next, "Дальше", serviceIntent(ACTION_NEXT, 4));
        return builder.build();
    }

    private PendingIntent serviceIntent(String action, int requestCode) {
        Intent intent = new Intent(this, PlayerService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Музыка", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (player != null && player.isPlaying()) {
            startForeground(NOTIFICATION_ID, buildNotification());
        }
        super.onTaskRemoved(rootIntent);
    }
}
