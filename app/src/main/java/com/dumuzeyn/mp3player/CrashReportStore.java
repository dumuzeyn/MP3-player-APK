package com.dumuzeyn.mp3player;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class CrashReportStore {
    private static final String DIRECTORY = "crash-reports";
    private static final String TAG = "MP3CrashReporter";
    private static final int MAX_REPORTS = 5;
    private static final int MAX_REPORT_LENGTH = 96 * 1024;

    private CrashReportStore() {
    }

    static void install(Context context) {
        Thread.UncaughtExceptionHandler current = Thread.getDefaultUncaughtExceptionHandler();
        if (current instanceof ReportingHandler) {
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler(
                new ReportingHandler(context.getApplicationContext(), current));
    }

    static File record(Context context, Thread thread, Throwable error) {
        if (context == null || error == null) {
            return null;
        }
        File directory = reportDirectory(context);
        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(TAG, "report_directory_failed");
            return null;
        }
        String timestamp = utcTimestamp();
        File report = new File(directory,
                "crash-" + timestamp.replace(':', '-') + "-" + Process.myPid() + ".txt");
        String body = buildReport(context, thread, error, timestamp);
        try (FileOutputStream stream = new FileOutputStream(report, false)) {
            stream.write(body.getBytes(StandardCharsets.UTF_8));
            stream.flush();
            prune(directory);
            Log.e(TAG, "crash_report_saved file=" + report.getName());
            return report;
        } catch (Exception writeError) {
            Log.e(TAG, "crash_report_write_failed", writeError);
            return null;
        }
    }

    static int count(Context context) {
        return reports(context).length;
    }

    static String latestSummary(Context context) {
        File[] reports = reports(context);
        if (reports.length == 0) {
            return "";
        }
        File latest = reports[0];
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(latest), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("exception=")) {
                    return line.substring("exception=".length());
                }
            }
        } catch (Exception ignored) {
        }
        return latest.getName();
    }

    static void clear(Context context) {
        for (File report : reports(context)) {
            if (!report.delete()) {
                Log.w(TAG, "crash_report_delete_failed file=" + report.getName());
            }
        }
    }

    static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replaceAll("content://\\S+", "content://<redacted>")
                .replaceAll("file:/+\\S+", "file://<redacted>")
                .replaceAll("/storage/\\S+", "/storage/<redacted>")
                .replaceAll("/sdcard/\\S+", "/sdcard/<redacted>");
    }

    private static String buildReport(Context context, Thread thread, Throwable error,
            String timestamp) {
        StringWriter stackBuffer = new StringWriter();
        error.printStackTrace(new PrintWriter(stackBuffer));
        String stack = sanitize(stackBuffer.toString());
        if (stack.length() > MAX_REPORT_LENGTH) {
            stack = stack.substring(0, MAX_REPORT_LENGTH) + "\n<truncated>";
        }
        return "timestamp=" + timestamp + "\n"
                + "version=" + appVersion(context) + "\n"
                + "android=" + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n"
                + "device=" + sanitize(Build.MANUFACTURER + " " + Build.MODEL) + "\n"
                + "thread=" + sanitize(thread == null ? "unknown" : thread.getName()) + "\n"
                + "exception=" + sanitize(error.getClass().getName() + ": " + error.getMessage()) + "\n\n"
                + stack;
    }

    @SuppressWarnings("deprecation")
    private static String appVersion(Context context) {
        try {
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= 33) {
                info = context.getPackageManager().getPackageInfo(context.getPackageName(),
                        android.content.pm.PackageManager.PackageInfoFlags.of(0));
            } else {
                info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            }
            return info.versionName + " (" + info.versionCode + ")";
        } catch (Exception error) {
            return "unknown";
        }
    }

    private static File reportDirectory(Context context) {
        return new File(context.getFilesDir(), DIRECTORY);
    }

    private static File[] reports(Context context) {
        File[] files = reportDirectory(context).listFiles((directory, name) ->
                name.startsWith("crash-") && name.endsWith(".txt"));
        if (files == null) {
            return new File[0];
        }
        Arrays.sort(files, (left, right) -> Long.compare(right.lastModified(), left.lastModified()));
        return files;
    }

    private static void prune(File directory) {
        File[] files = directory.listFiles((parent, name) ->
                name.startsWith("crash-") && name.endsWith(".txt"));
        if (files == null || files.length <= MAX_REPORTS) {
            return;
        }
        Arrays.sort(files, (left, right) -> Long.compare(
                right.lastModified(), left.lastModified()));
        for (int index = MAX_REPORTS; index < files.length; index++) {
            if (!files[index].delete()) {
                Log.w(TAG, "old_crash_report_delete_failed file=" + files[index].getName());
            }
        }
    }

    private static String utcTimestamp() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    private static final class ReportingHandler implements Thread.UncaughtExceptionHandler {
        private final Context context;
        private final Thread.UncaughtExceptionHandler next;

        ReportingHandler(Context context, Thread.UncaughtExceptionHandler next) {
            this.context = context;
            this.next = next;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable error) {
            record(context, thread, error);
            if (next != null) {
                next.uncaughtException(thread, error);
            } else {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        }
    }
}
