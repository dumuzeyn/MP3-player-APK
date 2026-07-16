package com.dumuzeyn.mp3player;

final class PlaybackQueueNavigator {
    enum Reason {
        FINISHED,
        MANUAL_NEXT,
        MANUAL_PREVIOUS,
        ERROR
    }

    static final class Decision {
        final boolean stop;
        final int nextIndex;
        final int loopOneErrorRetries;

        private Decision(boolean stop, int nextIndex, int loopOneErrorRetries) {
            this.stop = stop;
            this.nextIndex = nextIndex;
            this.loopOneErrorRetries = loopOneErrorRetries;
        }

        static Decision play(int index, int retries) {
            return new Decision(false, index, retries);
        }

        static Decision stop() {
            return new Decision(true, -1, 0);
        }
    }

    private PlaybackQueueNavigator() {
    }

    static Decision decide(int queueSize, int currentIndex, int loopMode, boolean oneShot,
            Reason reason, int loopOneErrorRetries) {
        if (queueSize <= 0) {
            return Decision.stop();
        }
        if (reason == Reason.MANUAL_PREVIOUS) {
            int previous = currentIndex <= 0 ? queueSize - 1 : currentIndex - 1;
            return Decision.play(previous, loopOneErrorRetries);
        }
        if (reason == Reason.FINISHED && loopMode == 1) {
            return Decision.play(normalize(currentIndex, queueSize), loopOneErrorRetries);
        }
        if (reason == Reason.ERROR && loopMode == 1 && loopOneErrorRetries == 0) {
            return Decision.play(normalize(currentIndex, queueSize), 1);
        }
        if (oneShot && loopMode == 0 && reason == Reason.FINISHED) {
            return Decision.stop();
        }
        if (loopMode == 0 && reason == Reason.FINISHED && currentIndex >= queueSize - 1) {
            return Decision.stop();
        }
        int next = currentIndex < 0 ? 0 : (currentIndex + 1) % queueSize;
        return Decision.play(next, 0);
    }

    private static int normalize(int index, int queueSize) {
        return Math.max(0, Math.min(index, queueSize - 1));
    }
}
