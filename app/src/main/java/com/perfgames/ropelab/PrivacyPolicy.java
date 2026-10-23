package com.perfgames.ropelab;

/** Full offline policy with the publisher's confirmed public contact and website. */
public final class PrivacyPolicy {
    public static final String TITLE = "Falley Privacy Policy";
    public static final String PUBLISHER = "Publisher: Perf Games";
    public static final String EFFECTIVE_DATE = "Effective: 16 October 2026";
    public static final String UPDATED_DATE = "Updated: 23 October 2026";
    public static final String CONTACT_EMAIL = "mathurrohitash16@gmail.com";
    public static final String PUBLIC_URL = "https://falley-privacy-policy.vercel.app/";
    public static final String[][] SECTIONS = {
        {"An offline game", "Falley works offline and has no Internet permission. It has no accounts, advertising, analytics or tracking SDKs. Perf Games does not collect, receive, sell or share personal information through this version of Falley."},
        {"Motion controls", "If you enable motion steering, accelerometer and gyroscope readings are processed on your device to control the game. These readings are temporary: Falley does not save them or send them anywhere. Touch controls work with motion steering off."},
        {"Data on your device", "Falley saves your settings, selected stage, scores, run counts and best times in the app's private storage on your device. Sound effects and vibration are produced locally. No gameplay data is sent to Perf Games or another service."},
        {"Deleting your data", "To erase your saved settings and records, clear Falley's app storage in Android Settings or uninstall the app. This cannot be undone. Android app backup is disabled for Falley."},
        {"Your choices", "You can turn motion steering, sound effects, vibration and blood effects on or off in Settings. Falley does not ask for camera, microphone, location or contacts access."},
        {"About this policy", "This policy describes the current offline version of Falley, published by Perf Games. If the app's data practices change, its policy will be updated before those changes are introduced."},
        {"Support messages", "If you email us, we receive your address, message and any attachments you choose to send. We use them to answer your inquiry or privacy request. Gmail, provided by Google, handles correspondence under Google's Privacy Policy. We keep messages only as long as needed to resolve your inquiry, unless law requires longer. You can request deletion by email. We do not sell messages or use them for advertising. Contacting us does not automatically send game records or sensor readings. Do not include sensitive information."},
        {"Online privacy policy", "Read the public policy at " + PUBLIC_URL + " Opening it uses your browser and Internet connection. Vercel hosts the website and may process technical request information to deliver and protect the page. This is separate from the offline game."},
        {"Privacy contact", "For privacy questions about Falley, email Perf Games at " + CONTACT_EMAIL + ". The buttons below open the policy website or your email app. No message is sent automatically."}
    };

    private PrivacyPolicy() { }

    public static String fullText() {
        StringBuilder text = new StringBuilder(TITLE).append("\n").append(PUBLISHER)
                .append("\n").append(EFFECTIVE_DATE).append("\n").append(UPDATED_DATE);
        for (String[] section : SECTIONS) text.append("\n\n").append(section[0]).append("\n").append(section[1]);
        return text.toString();
    }
}
