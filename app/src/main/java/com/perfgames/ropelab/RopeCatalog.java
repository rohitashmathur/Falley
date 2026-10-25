package com.perfgames.ropelab;

/** Per-surface abilities in Campaign and Arcade; Daily stays Classic. */
public final class RopeCatalog {
    public static final int CLASSIC=0, SPRING=1, STATIC=2, COUNT=3;
    public static int normalize(int id){return id>=0&&id<COUNT?id:CLASSIC;}
    public static String name(int id){switch(normalize(id)){case SPRING:return "Spring";case STATIC:return "Static";default:return "Classic";}}
    public static String description(int id){switch(normalize(id)){case SPRING:return "1.5× supporting-rope rebound. Manual jumps stay normal.";case STATIC:return "After rope support: next manual jump 0.8×, brief steering damping.";default:return "Original rope rebound. No extra effects.";}}
    public static int requiredStars(int id){switch(normalize(id)){case SPRING:return 6;case STATIC:return 12;default:return 0;}}
    public static int color(int id){switch(normalize(id)){case SPRING:return 0xff70e2c2;case STATIC:return 0xffb8a3f4;default:return 0xfff6d477;}}
    private RopeCatalog(){}
}