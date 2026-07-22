package com.dumuzeyn.mp3player;

import android.content.Context;
import java.util.ArrayList;

/** Generates synthetic metadata only in the non-distributable benchmark build. */
final class BenchmarkLibrarySeeder {
    static final String EXTRA_TRACK_COUNT = "voltuneBenchmarkTrackCount";
    private static final int MAX_TRACKS = 10000;

    private BenchmarkLibrarySeeder() {
    }

    static void seedIfRequested(Context context, int requestedCount) {
        if (!"benchmark".equals(BuildConfig.BUILD_TYPE) || requestedCount <= 0) {
            return;
        }
        int count = Math.min(MAX_TRACKS, requestedCount);
        ArrayList<Track> tracks = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            tracks.add(new Track("content://voltune.benchmark/track/" + index,
                    String.format(java.util.Locale.ROOT, "Benchmark song %05d", index),
                    "Benchmark artist " + (index % 200), "Benchmark album " + (index % 500),
                    "Benchmark genre " + (index % 20), 180000 + (index % 120000)));
        }
        TrackStore.save(context, tracks);
    }
}
