package com.dumuzeyn.mp3player;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Calculates one fixed gain per track and caches it. Gain never changes inside a song. */
final class TrackLoudnessNormalizer {
    private static final String DEBUG_TAG = "VoltuneDebug";
    private static final String PREFS = "track_loudness_cache";
    private static final String GAIN_PREFIX = "gain_db_";
    private static final float TARGET_DBFS = -18.0f;
    private static final float MAX_BOOST_DB = 8.0f;
    private static final float MAX_CUT_DB = -10.0f;
    private static final long MAX_ANALYSIS_US = 15L * 60L * 1_000_000L;

    private final Context context;
    private final SharedPreferences cache;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Set<String> pending = Collections.synchronizedSet(new HashSet<>());

    TrackLoudnessNormalizer(Context context) {
        this.context = context.getApplicationContext();
        this.cache = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    float cachedGainDb(String uri) {
        if (!enabled() || uri == null || uri.isEmpty()) {
            return 0.0f;
        }
        return cache.getFloat(GAIN_PREFIX + key(uri), 0.0f);
    }

    void prefetch(List<Track> queue, int currentIndex) {
        if (!enabled() || queue == null || queue.isEmpty()) {
            return;
        }
        for (int offset = 0; offset < Math.min(3, queue.size()); offset++) {
            Track track = queue.get((Math.max(0, currentIndex) + offset) % queue.size());
            prefetch(track == null ? null : track.uri);
        }
    }

    void release() {
        executor.shutdownNow();
        pending.clear();
    }

    private void prefetch(String uri) {
        if (uri == null || uri.isEmpty()) {
            return;
        }
        String cacheKey = GAIN_PREFIX + key(uri);
        if (cache.contains(cacheKey) || !pending.add(uri)) {
            return;
        }
        executor.execute(() -> {
            try {
                Float gain = analyze(uri);
                if (gain != null) {
                    cache.edit().putFloat(cacheKey, gain).apply();
                    Log.i(DEBUG_TAG, "loudness_analyzed gainDb=" + gain + " uriHash=" + key(uri));
                }
            } catch (RuntimeException error) {
                Log.w(DEBUG_TAG, "loudness_analysis_failed error=" + error.getMessage());
            } finally {
                pending.remove(uri);
            }
        });
    }

    private Float analyze(String uri) {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec decoder = null;
        try {
            extractor.setDataSource(context, Uri.parse(uri), null);
            MediaFormat format = selectAudioTrack(extractor);
            if (format == null) {
                return null;
            }
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime == null) {
                return null;
            }
            int sampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                    ? format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
            int channels = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                    ? format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 2;
            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, null, null, 0);
            decoder.start();
            return decodeLoudness(extractor, decoder, sampleRate, channels);
        } catch (Exception error) {
            Log.w(DEBUG_TAG, "loudness_decode_failed error=" + error.getMessage());
            return null;
        } finally {
            if (decoder != null) {
                try {
                    decoder.stop();
                } catch (RuntimeException ignored) {
                }
                try {
                    decoder.release();
                } catch (RuntimeException ignored) {
                }
            }
            extractor.release();
        }
    }

    private MediaFormat selectAudioTrack(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); index++) {
            MediaFormat format = extractor.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                extractor.selectTrack(index);
                return format;
            }
        }
        return null;
    }

    private Float decodeLoudness(MediaExtractor extractor, MediaCodec decoder,
            int sampleRate, int channels) {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        LoudnessAccumulator accumulator = new LoudnessAccumulator(sampleRate, channels);
        boolean inputDone = false;
        boolean outputDone = false;
        int pcmEncoding = AudioFormat.ENCODING_PCM_16BIT;
        while (!outputDone && !Thread.currentThread().isInterrupted()) {
            if (!inputDone) {
                int inputIndex = decoder.dequeueInputBuffer(10_000L);
                if (inputIndex >= 0) {
                    ByteBuffer input = decoder.getInputBuffer(inputIndex);
                    int size = input == null ? -1 : extractor.readSampleData(input, 0);
                    long sampleTime = extractor.getSampleTime();
                    if (size < 0 || sampleTime > MAX_ANALYSIS_US) {
                        decoder.queueInputBuffer(inputIndex, 0, 0,
                                Math.max(0L, sampleTime), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        decoder.queueInputBuffer(inputIndex, 0, size, sampleTime, 0);
                        extractor.advance();
                    }
                }
            }
            int outputIndex = decoder.dequeueOutputBuffer(info, 10_000L);
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat output = decoder.getOutputFormat();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N
                        && output.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                    pcmEncoding = output.getInteger(MediaFormat.KEY_PCM_ENCODING);
                }
            } else if (outputIndex >= 0) {
                ByteBuffer output = decoder.getOutputBuffer(outputIndex);
                if (output != null && info.size > 0) {
                    output.position(info.offset);
                    output.limit(info.offset + info.size);
                    accumulator.add(output.slice().order(ByteOrder.LITTLE_ENDIAN), pcmEncoding);
                }
                outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                decoder.releaseOutputBuffer(outputIndex, false);
            }
        }
        return accumulator.gainDb();
    }

    private boolean enabled() {
        return context.getSharedPreferences(EqualizerController.PREFS, Context.MODE_PRIVATE)
                .getBoolean(VolumeLevelingController.ENABLED, false);
    }

    private static String key(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(32);
            for (int index = 0; index < 16; index++) {
                result.append(String.format(java.util.Locale.ROOT, "%02x", digest[index]));
            }
            return result.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static final class LoudnessAccumulator {
        private final int blockSamples;
        private int blockCount;
        private double blockSquareSum;
        private long acceptedSamples;
        private double acceptedSquareSum;
        private double peak;

        LoudnessAccumulator(int sampleRate, int channels) {
            blockSamples = Math.max(1, Math.round(sampleRate * Math.max(1, channels) * 0.4f));
        }

        void add(ByteBuffer buffer, int encoding) {
            if (encoding == AudioFormat.ENCODING_PCM_FLOAT) {
                while (buffer.remaining() >= 4) {
                    addSample(Math.max(-1.0, Math.min(1.0, buffer.getFloat())));
                }
            } else {
                while (buffer.remaining() >= 2) {
                    addSample(buffer.getShort() / 32768.0);
                }
            }
        }

        private void addSample(double sample) {
            double absolute = Math.abs(sample);
            peak = Math.max(peak, absolute);
            blockSquareSum += sample * sample;
            blockCount++;
            if (blockCount >= blockSamples) {
                double blockRms = Math.sqrt(blockSquareSum / blockCount);
                if (blockRms >= Math.pow(10.0, -50.0 / 20.0)) {
                    acceptedSquareSum += blockSquareSum;
                    acceptedSamples += blockCount;
                }
                blockSquareSum = 0.0;
                blockCount = 0;
            }
        }

        Float gainDb() {
            if (acceptedSamples == 0 || peak <= 0.0) {
                return null;
            }
            double rms = Math.sqrt(acceptedSquareSum / acceptedSamples);
            double rmsDb = 20.0 * Math.log10(Math.max(1.0e-9, rms));
            double peakDb = 20.0 * Math.log10(Math.max(1.0e-9, peak));
            double gain = Math.min(TARGET_DBFS - rmsDb, -1.0 - peakDb);
            return (float) Math.max(MAX_CUT_DB, Math.min(MAX_BOOST_DB, gain));
        }
    }
}
