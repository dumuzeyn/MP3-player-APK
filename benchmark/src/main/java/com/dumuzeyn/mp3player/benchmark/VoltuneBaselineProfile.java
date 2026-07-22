package com.dumuzeyn.mp3player.benchmark;

import androidx.benchmark.macro.junit4.BaselineProfileRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject2;
import kotlin.Unit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class VoltuneBaselineProfile {
    private static final String PACKAGE_NAME = "com.dumuzeyn.mp3player";

    @Rule public final BaselineProfileRule rule = new BaselineProfileRule();

    @Test public void criticalUserJourneys() {
        rule.collect(PACKAGE_NAME, scope -> {
            scope.startActivityAndWait();
            click(scope, "Songs", "Песни");
            click(scope, "Favorites", "Избранное");
            click(scope, "Settings", "Настройки");
            click(scope, "Songs", "Песни");
            return Unit.INSTANCE;
        });
    }

    private static void click(androidx.benchmark.macro.MacrobenchmarkScope scope,
            String english, String russian) {
        UiObject2 target = scope.getDevice().findObject(By.textContains(english));
        if (target == null) {
            target = scope.getDevice().findObject(By.textContains(russian));
        }
        if (target != null) {
            target.click();
            scope.getDevice().waitForIdle();
        }
    }
}
