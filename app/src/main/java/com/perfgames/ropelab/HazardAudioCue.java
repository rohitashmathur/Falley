package com.perfgames.ropelab;

/** Event edge tracking, independent of Android and audio-device availability. */
public final class HazardAudioCue {
    private long sequence;
    public void reset(long currentSequence) { sequence = Math.max(0,currentSequence); }
    public int consume(long nextSequence,int event) {
        if (nextSequence <= sequence) return 0;
        sequence = nextSequence;
        return event >= 1 && event <= 6 ? event : 0;
    }
}
