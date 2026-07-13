package com.dumuzeyn.mp3player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CompatibilityInstrumentedTest {
    @Test
    public void applicationAndPlaybackServiceAreConfiguredForCurrentAndroid() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 0);
        assertNotNull(packageInfo.applicationInfo);
        assertEquals(Mp3PlayerApplication.class.getName(), packageInfo.applicationInfo.className);

        ServiceInfo serviceInfo = context.getPackageManager().getServiceInfo(
                new ComponentName(context, PlayerService.class), 0);
        assertNotNull(serviceInfo);
        assertTrue(!serviceInfo.exported);
        if (Build.VERSION.SDK_INT >= 29) {
            assertTrue((serviceInfo.getForegroundServiceType()
                    & ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) != 0);
        }
        assertTrue(Build.VERSION.SDK_INT >= 26);
    }
}
