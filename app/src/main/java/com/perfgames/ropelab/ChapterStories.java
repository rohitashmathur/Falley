package com.perfgames.ropelab;

/** Original chapter framing, with a visible threat and the earned companion it leaves behind. */
final class ChapterStories {
    static final String[] INTRO={
        "The city hangs in light. Read the prism rig's charge and find a way down.",
        "The canyon keeps what falls. A spinning rig guards the last bounce.",
        "Below the canyon, a furnace demon wakes. Read its hands before the fire flies.",
        "A storm robot drinks the sky. Its coils glow before every bolt.",
        "The foundry's iron guardian is still running. Its cannon spits saws and light.",
        "The core demon waits at the end. Read each charge; take the gap it leaves."
    };
    static final String[] OUTRO={
        "The prism rig sleeps. A winged stranger joins your descent.",
        "The saw rig falls silent. A sky watcher offers another way down.",
        "The furnace demon fades. A rescue veteran answers the falling sparks.",
        "The storm robot powers down. An explorer arrives from the quiet sky.",
        "The iron guardian stops. A foundry worker chooses the way down.",
        "The core demon dissolves. A shadow steps out of the broken light."
    };
    static String intro(int chapter){return INTRO[Math.max(0,Math.min(5,chapter))];}
    static String outro(int chapter){return OUTRO[Math.max(0,Math.min(5,chapter))];}
    private ChapterStories(){}
}
