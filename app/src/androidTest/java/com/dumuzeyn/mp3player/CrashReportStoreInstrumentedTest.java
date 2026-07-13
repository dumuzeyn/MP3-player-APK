package com.dumuzeyn.mp3player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RunWith(AndroidJUnit4.class)
public class CrashReportStoreInstrumentedTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        CrashReportStore.clear(context);
    }

    @After
    public void tearDown() {
        CrashReportStore.clear(context);
    }

    @Test
    public void reportIsStoredLocallyAndSensitivePathsAreRedacted() throws Exception {
        IllegalStateException error = new IllegalStateException(
                "Cannot read content://music/private/song.mp3 from /storage/emulated/0/Music/song.mp3");
        File report = CrashReportStore.record(context, Thread.currentThread(), error);

        assertNotNull(report);
        assertTrue(report.isFile());
        assertEquals(1, CrashReportStore.count(context));

        String body = new String(Files.readAllBytes(report.toPath()), StandardCharsets.UTF_8);
        assertTrue(body.contains("content://<redacted>"));
        assertTrue(body.contains("/storage/<redacted>"));
        assertFalse(body.contains("private/song.mp3"));
        assertFalse(body.contains("emulated/0/Music"));
    }
}
