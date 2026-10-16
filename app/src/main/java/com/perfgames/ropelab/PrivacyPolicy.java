package com.perfgames.ropelab;

/** Offline policy for the current app; no external address or unconfigured link. */
public final class PrivacyPolicy {
    public static final String TITLE = "Falley Privacy Policy";
    public static final String PUBLISHER = "Publisher: Perf Games";
    public static final String EFFECTIVE_DATE = "Effective: 16 October 2026";
    public static final String[][] SECTIONS = {
        {"An offline game", "Falley works offline and has no Internet permission. It has no accounts, advertising, analytics or tracking SDKs. Perf Games does not collect, receive, sell or share personal information through this version of Falley."},
        {"Motion controls", "If you enable motion steering, accelerometer and gyroscope readings are processed on your device to control the game. These readings are temporary: Falley does not save them or send them anywhere. Touch controls work with motion steering off."},
        {"Data on your device", "Falley saves your settings, selected stage, scores, run counts and best times in the app's private storage on your device. Sound effects and vibration are produced locally. No gameplay data is sent to Perf Games or another service."},
        {"Deleting your data", "To erase your saved settings and records, clear Falley's app storage in Android Settings or uninstall the app. This cannot be undone. Android app backup is disabled for Falley."},
        {"Your choices", "You can turn motion steering, sound effects, vibration and blood effects on or off in Settings. Falley does not ask for camera, microphone, location or contacts access."},
        {"About this policy", "This policy describes the current offline version of Falley, published by Perf Games. If the app's data practices change, its policy will be updated before those changes are introduced."}
    };

    private PrivacyPolicy() { }

    public static String fullText() {
        StringBuilder text = new StringBuilder(TITLE).append("\n").append(PUBLISHER)
                .append("\n").append(EFFECTIVE_DATE);
        for (String[] section : SECTIONS) text.append("\n\n").append(section[0]).append("\n").append(section[1]);
        return text.toString();
    }
}
