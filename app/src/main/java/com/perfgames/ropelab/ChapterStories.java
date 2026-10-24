package com.perfgames.ropelab;

/** Short original chapter framing; no text interrupts an active descent. */
final class ChapterStories {
    static final String[] INTRO={
        "The lights have trapped the city. Find a path through their rhythm.",
        "The canyon keeps what falls. Trust the bounce, then let go.",
        "The furnace is awake. Borrow a breath of slow time.",
        "The storm has no master. Read the wind before it turns.",
        "The foundry is still running. Break its rhythm, not your nerve.",
        "Every fall led here. Bring the last machine to silence."
    };
    static final String[] OUTRO={
        "A line of light goes dark. A new way down opens.",
        "The canyon lets you pass. Something burns below.",
        "The furnace quiets. The sky answers with thunder.",
        "The storm clears. The foundry waits beneath it.",
        "The blades rest. One final descent remains.",
        "The machines are quiet. Tomorrow brings a new drop."
    };
    static String intro(int chapter){return INTRO[Math.max(0,Math.min(5,chapter))];}
    static String outro(int chapter){return OUTRO[Math.max(0,Math.min(5,chapter))];}
    private ChapterStories(){}
}
