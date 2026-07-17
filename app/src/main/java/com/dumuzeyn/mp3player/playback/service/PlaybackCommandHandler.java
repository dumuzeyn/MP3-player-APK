package com.dumuzeyn.mp3player.playback.service;

import android.content.Intent;
import com.dumuzeyn.mp3player.PlayerService;
import java.util.ArrayList;

/** Converts Android service intents into validated playback commands. */
public final class PlaybackCommandHandler {
    public enum Type {
        RESTORE,
        TIMER_START,
        TIMER_CANCEL,
        PLAY_INDEX,
        TOGGLE,
        NEXT,
        PREVIOUS,
        SEEK,
        LOOP,
        AUDIO_EFFECTS,
        UPDATE_QUEUE,
        STOP,
        UNKNOWN
    }

    public static final class Command {
        public final Type type;
        public final int index;
        public final boolean hasIndex;
        public final int position;
        public final int loopMode;
        public final boolean oneShot;
        public final boolean shuffle;
        public final boolean hasShuffle;
        public final long timerMs;
        public final ArrayList<String> queueUris;

        private Command(Type type, Intent intent) {
            this.type = type;
            this.index = intent == null ? 0 : intent.getIntExtra(PlayerService.EXTRA_INDEX, 0);
            this.hasIndex = intent != null && intent.hasExtra(PlayerService.EXTRA_INDEX);
            this.position = intent == null
                    ? 0 : intent.getIntExtra(PlayerService.EXTRA_POSITION, 0);
            this.loopMode = intent == null ? Integer.MIN_VALUE
                    : intent.getIntExtra(PlayerService.EXTRA_LOOP_MODE, Integer.MIN_VALUE);
            this.oneShot = intent != null
                    && intent.getBooleanExtra(PlayerService.EXTRA_ONE_SHOT, false);
            this.shuffle = intent != null
                    && intent.getBooleanExtra(PlayerService.EXTRA_SHUFFLE, false);
            this.hasShuffle = intent != null && intent.hasExtra(PlayerService.EXTRA_SHUFFLE);
            this.timerMs = intent == null
                    ? 0L : intent.getLongExtra(PlayerService.EXTRA_TIMER_MS, 0L);
            this.queueUris = intent == null
                    ? null : intent.getStringArrayListExtra(PlayerService.EXTRA_QUEUE_URIS);
        }
    }

    public Command read(Intent intent) {
        if (intent == null) {
            return new Command(Type.RESTORE, null);
        }
        String action = intent.getAction();
        if (PlayerService.ACTION_TIMER_START.equals(action)) {
            return new Command(Type.TIMER_START, intent);
        }
        if (PlayerService.ACTION_TIMER_CANCEL.equals(action)) {
            return new Command(Type.TIMER_CANCEL, intent);
        }
        if (PlayerService.ACTION_PLAY_INDEX.equals(action)) {
            return new Command(Type.PLAY_INDEX, intent);
        }
        if (PlayerService.ACTION_TOGGLE.equals(action)) {
            return new Command(Type.TOGGLE, intent);
        }
        if (PlayerService.ACTION_NEXT.equals(action)) {
            return new Command(Type.NEXT, intent);
        }
        if (PlayerService.ACTION_PREV.equals(action)) {
            return new Command(Type.PREVIOUS, intent);
        }
        if (PlayerService.ACTION_SEEK.equals(action)) {
            return new Command(Type.SEEK, intent);
        }
        if (PlayerService.ACTION_LOOP.equals(action)) {
            return new Command(Type.LOOP, intent);
        }
        if (PlayerService.ACTION_AUDIO_EFFECTS.equals(action)) {
            return new Command(Type.AUDIO_EFFECTS, intent);
        }
        if (PlayerService.ACTION_UPDATE_QUEUE.equals(action)) {
            return new Command(Type.UPDATE_QUEUE, intent);
        }
        if (PlayerService.ACTION_STOP.equals(action)) {
            return new Command(Type.STOP, intent);
        }
        return new Command(Type.UNKNOWN, intent);
    }
}
