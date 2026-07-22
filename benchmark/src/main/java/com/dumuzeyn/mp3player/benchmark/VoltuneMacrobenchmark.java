package com.dumuzeyn.mp3player.benchmark;

import android.content.Intent;
import androidx.benchmark.macro.CompilationMode;
import androidx.benchmark.macro.FrameTimingMetric;
import androidx.benchmark.macro.MacrobenchmarkScope;
import androidx.benchmark.macro.StartupMode;
import androidx.benchmark.macro.StartupTimingMetric;
import androidx.benchmark.macro.junit4.MacrobenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject2;
import java.util.Arrays;
import kotlin.Unit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class VoltuneMacrobenchmark {
    private static final String PACKAGE_NAME = "com.dumuzeyn.mp3player";

    @Rule public final MacrobenchmarkRule rule = new MacrobenchmarkRule();

    @Test public void coldStartup() {
        startup(StartupMode.COLD);
    }

    @Test public void warmStartup() {
        startup(StartupMode.WARM);
    }

    @Test public void hotStartup() {
        startup(StartupMode.HOT);
    }

    @Test public void largeLibraryScrollAndSectionSwitch() {
        rule.measureRepeated(PACKAGE_NAME,
                Arrays.asList(new FrameTimingMetric()), new CompilationMode.Partial(), null, 5,
                scope -> {
                    scope.pressHome();
                    return Unit.INSTANCE;
                },
                scope -> {
                    Intent intent = new Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_LAUNCHER)
                            .setPackage(PACKAGE_NAME)
                            .putExtra("voltuneBenchmarkTrackCount", 10000);
                    scope.startActivityAndWait(intent);
                    scope.getDevice().swipe(500, 1600, 500, 400, 20);
                    clickText(scope, "Favorites", "Избранное");
                    clickText(scope, "Songs", "Песни");
                    return Unit.INSTANCE;
                });
    }

    private void startup(StartupMode mode) {
        rule.measureRepeated(PACKAGE_NAME,
                Arrays.asList(new StartupTimingMetric(), new FrameTimingMetric()),
                new CompilationMode.Partial(), mode, 5,
                scope -> {
                    scope.pressHome();
                    return Unit.INSTANCE;
                },
                scope -> {
                    scope.startActivityAndWait();
                    return Unit.INSTANCE;
                });
    }

    private static void clickText(MacrobenchmarkScope scope, String english, String russian) {
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
