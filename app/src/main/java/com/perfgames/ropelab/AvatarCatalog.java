package com.perfgames.ropelab;

/** User-approved original roster, with a starter and one earned chapter reward. */
public final class AvatarCatalog {
    public static final int HUMAN=0, DEMON=1, ANGEL=2, MILITARY=3, ASTRONAUT=4, ROBOT=5, SHADOW=6, COUNT=7;
    private static final String[] NAMES={"Human","Demon","Angel","Military","Astronaut","Robot","Shadow"};
    public static String name(int avatar){return NAMES[Math.max(0,Math.min(COUNT-1,avatar))];}
    private AvatarCatalog(){}
}
