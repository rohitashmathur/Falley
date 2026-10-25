package com.perfgames.ropelab;

/** Original earned roster. Ability numbers live in AvatarAbilities; copy explains the tradeoffs. */
public final class AvatarCatalog {
    public static final int HUMAN=0, DEMON=1, ANGEL=2, MILITARY=3, ASTRONAUT=4, ROBOT=5, SHADOW=6, COUNT=7;
    private static final String[] NAMES={"Human","Demon","Angel","Military","Astronaut","Robot","Shadow"};
    private static final String[] TRAITS={
        "Standard jumps", "Jump height +20%", "Jump height +20%", "Jump height −15%",
        "Jump height +35%", "Fireball immunity", "Laser and lightning immunity"
    };
    private static final String[] DETAILS={
        "Standard rope load. All hazards can still kill.",
        "Standard rope load. Wings do not block hazards.",
        "Standard rope load. Wings do not block hazards.",
        "Rope load +25%. No hazard immunity.",
        "Rope load −25%. No hazard immunity.",
        "Rope load +15%. Fire walls and other hazards still kill.",
        "Jump height +20%; rope load −15%. Fireballs still kill."
    };
    private static final String[] STORIES={
        "A city runner who turns every fall into a way forward.",
        "Escaped the furnace with wings made for one more leap.",
        "A sky watcher following the ropes into the dark.",
        "A rescue veteran carrying the weight of those left below.",
        "A stranded explorer, light on the ropes and hungry for open sky.",
        "A foundry worker built to cross sparks and falling fire.",
        "Born between broken lights, at home in beams and storms."
    };
    private static int index(int avatar){return Math.max(0,Math.min(COUNT-1,avatar));}
    public static String name(int avatar){return NAMES[index(avatar)];}
    public static String trait(int avatar){return TRAITS[index(avatar)];}
    public static String detail(int avatar){return DETAILS[index(avatar)];}
    public static String story(int avatar){return STORIES[index(avatar)];}
    private AvatarCatalog(){}
}
