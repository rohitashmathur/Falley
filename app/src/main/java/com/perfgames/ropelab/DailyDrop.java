package com.perfgames.ropelab;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** The offline daily course is shared by UTC date and generator version, never by device locale. */
public final class DailyDrop {
    public static final int GENERATOR_VERSION = 2;
    public static final long MILLIS_PER_DAY = 86_400_000L;
    public static final long NO_DAY = Long.MIN_VALUE;
    // The supported public date range keeps corrupt stored timestamps out of arithmetic/formatting.
    public static final long MIN_DAY = 0, MAX_DAY = 2_932_896L;
    public static final int TOUCH = 0, MOTION = 1, MIXED = 2;
    public static int controlMode(int value) { return value >= TOUCH && value <= MIXED ? value : TOUCH; }

    public static long today() { return utcDay(System.currentTimeMillis()); }
    public static long utcDay(long epochMillis) { return Math.floorDiv(epochMillis, MILLIS_PER_DAY); }
    public static boolean validDay(long day) { return day >= MIN_DAY && day <= MAX_DAY; }

    /** A version change intentionally starts a new reproducible course family. No network is used. */
    public static long seed(long day) {
        if (!validDay(day)) throw new IllegalArgumentException("Unsupported Daily Drop date");
        long value = day ^ (0x9e3779b97f4a7c15L * GENERATOR_VERSION);
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    public static String dateLabel(long day) {
        if (!validDay(day)) return "—";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(day * MILLIS_PER_DAY));
    }

    /** Included in shared results so scores from future generator versions remain distinguishable. */
    public static String courseId(long day) { return dateLabel(day) + " · v" + GENERATOR_VERSION; }
    private DailyDrop() { }
}
