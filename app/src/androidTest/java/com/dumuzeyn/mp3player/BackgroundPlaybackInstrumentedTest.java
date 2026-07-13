package com.dumuzeyn.mp3player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class BackgroundPlaybackInstrumentedTest {
    private Context context;
    private Instrumentation instrumentation;
    private File waveFile;
    private Activity activity;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        instrumentation = InstrumentationRegistry.getInstrumentation();
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentedTestSupport.runShellCommand(instrumentation,
                    "pm grant " + context.getPackageName() + " "
                            + Manifest.permission.POST_NOTIFICATIONS);
        }
        stopPlayback();
        TrackStore.save(context, Collections.<Track>emptyList());
        waveFile = InstrumentedTestSupport.createTestWave(context, 8);
        Track track = new Track(Uri.fromFile(waveFile).toString(), "Instrumentation tone",
                "MP3 Player tests", "Compatibility", "Test", 8000);
        TrackStore.save(context, Collections.singletonList(track));
    }

    @After
    public void tearDown() {
        stopPlayback();
        TrackStore.save(context, Collections.<Track>emptyList());
        if (waveFile != null && waveFile.exists()) {
            waveFile.delete();
        }
        if (activity != null) {
            instrumentation.runOnMainSync(() -> activity.finish());
        }
    }

    @Test
    public void playbackContinuesAfterActivityIsClosedAndCanBePaused() {
        Intent activityIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity = instrumentation.startActivitySync(activityIntent);
        instrumentation.waitForIdleSync();

        ArrayList<String> queue = new ArrayList<>();
        queue.add(Uri.fromFile(waveFile).toString());
        Intent playIntent = new Intent(context, PlayerService.class)
                .setAction(PlayerService.ACTION_PLAY_INDEX)
                .putExtra(PlayerService.EXTRA_INDEX, 0)
                .putStringArrayListExtra(PlayerService.EXTRA_QUEUE_URIS, queue)
                .putExtra(PlayerService.EXTRA_SHUFFLE, false)
                .putExtra(PlayerService.EXTRA_LOOP_MODE, 0);
        startPlaybackService(playIntent);

        InstrumentedTestSupport.waitFor("PlayerService did not start playback", 15000L,
                () -> PlayerService.lastPlaying && PlayerService.lastDuration > 0);
        assertEquals(queue.get(0), PlayerService.lastUri);

        instrumentation.runOnMainSync(() -> activity.finish());
        activity = null;
        instrumentation.waitForIdleSync();
        SystemClock.sleep(1500L);
        PlayerService.refreshSnapshot();
        assertTrue("Playback stopped when Activity closed", PlayerService.lastPlaying);
        assertTrue("Playback position did not advance", PlayerService.lastPosition > 0);

        Intent pauseIntent = new Intent(context, PlayerService.class)
                .setAction(PlayerService.ACTION_TOGGLE);
        context.startService(pauseIntent);
        InstrumentedTestSupport.waitFor("PlayerService did not pause", 5000L,
                () -> {
                    PlayerService.refreshSnapshot();
                    return !PlayerService.lastPlaying;
                });
        assertFalse(PlayerService.lastPlaying);
        assertTrue(PlayerService.hasPlaybackSession());
    }

    private void startPlaybackService(Intent intent) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private void stopPlayback() {
        if (!PlayerService.hasPlaybackSession()) {
            return;
        }
        try {
            Intent stopIntent = new Intent(context, PlayerService.class)
                    .setAction(PlayerService.ACTION_STOP);
            context.startService(stopIntent);
        } catch (RuntimeException ignored) {
        }
        InstrumentedTestSupport.waitFor("PlayerService did not stop", 3000L,
                () -> !PlayerService.hasPlaybackSession());
    }
}
