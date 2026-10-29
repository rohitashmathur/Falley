# Falley privacy policy website

Plain, responsive HTML for Falley by **Perf Games**. The served page is `public/index.html`; no JavaScript, packages, forms, external assets or build step. The Android app contains the same core English disclosures, with Hindi and Bahasa Indonesia translations.

- Public URL: [falley-privacy-policy.vercel.app](https://falley-privacy-policy.vercel.app/)
- Support/privacy contact: [mathurrohitash16@gmail.com](mailto:mathurrohitash16@gmail.com)
- Current source: **Falley 4.4**, updated **29 October 2026**.

## Required before distributing 4.4

Redeploy this source to the public URL. A source push does not confirm a matching Vercel deployment. The existing live deployment was last inspected on 25 October and still showed older text; this task does not claim a new production deployment or current live-page verification.

The 4.4.1 policy describes two independent reporting choices, both on by default when no choice is saved (explicit opt-outs are preserved): Firebase Crashlytics and Google Analytics for Firebase. Gameplay still works offline. Both off at startup means Firebase is not initialized. Analytics changes apply immediately; crash-report changes fully apply on the next launch. Later crash opt-in may include reports cached while the SDK was previously active, and already queued/in-flight reports may finish. Disabling reporting or clearing app storage does not delete information already received by Google.

Local sensor samples and the temporary network/SIM/device-region language hint are not sent in Falley reporting events. Analytics may separately derive approximate geography from network information. The page also covers local records, Daily result-image sharing, local music, support email, provider processing, retention and deletion limits.

## Vercel deployment

Deploy the **`policy-site` directory**, using the supplied `vercel.json`:

| Setting | Value |
| --- | --- |
| Root Directory | `policy-site` |
| Framework Preset | Other |
| Build Command | Override with an empty value |
| Install Command | Override with an empty value |
| Output Directory | `public` |

The configuration serves only `public/`, adds security headers and aliases `/privacy` and `/privacy-policy` to the same page. No Vercel account or project authentication is configured here. Keep Vercel Web Analytics, Speed Insights and other injected tracking off unless the policy is revised.

After deployment, open the public root URL while signed out on phone and desktop. Check the 29 October 2026 update, both Firebase disclosures, all sections, contact link and readable mobile layout. Confirm there is no deployment login, country restriction or error. Use that stable URL in Play Console.

## Firebase and Play Console owner checks

The old “no data collected” Data safety answer no longer describes a reporting-enabled 4.4.1 install. Optional collection must still be considered in the form. Review the exact shipped SDKs and project settings, including transitive Firebase Installations and Sessions:

| Data to review | Why |
| --- | --- |
| Crash logs and diagnostics | Crashlytics stack traces, app state and device information |
| App activity / app interactions | Individual jumps/swipes, sampled raw motion/touch, screen/language changes, rewards, ropes, sharing taps, automatic session/app events and installation/source attribution |
| Device or other IDs | App-instance and Firebase installation/crash identifiers |
| Approximate location | Analytics geography inferred from network information |

Select the purposes matching this implementation: game reliability and gameplay analytics, with optional user choice. Review Google's service-provider handling against Play's definition of sharing rather than automatically marking every provider transfer as “shared” or “not shared.” These notes are a source-based checklist, not a submitted or approved Data safety form.

The merged app manifest contains four normal SDK permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK` and `com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE`. It also contains an app-local signature permission protecting internal dynamic receivers. These do not create runtime permission prompts. Play install-referrer access supports installation/source attribution while reporting is enabled; it is separate from advertising identifiers. Advertising-ID and Ad Services permissions remain excluded.

In Firebase/Analytics, confirm Analytics is enabled for the correct production project, restrict project access, review Crash Insights/data-sharing and ad integration settings, and choose/document an appropriate Analytics retention setting. Do not link an advertising account or enable advertising features without revisiting the app controls and disclosures. There is no account-based user lookup in Falley; deletion requests may not be linkable to a pseudonymous report.

Test each default-on switch and opt-out separately using the debug Android app registration, inspect DebugView and a controlled test-crash report, then verify production uses its own Android registration. The supplied registrations share one Firebase project: filter by app/stream and `build_channel` to keep debug measurements separate. Dashboard receipt is an external check; a successful APK build alone does not prove receipt.

## Verification scope

Source checks validate complete policy copy, matching app/website disclosures, all internal section targets, public contact/URL, updated date, three-language app coverage and the absence of scripts/forms/external page assets. Live deployment, visual browser acceptance and the owner's console declarations are separate checks. Monitor the supplied inbox and follow the support-message retention commitment.

## Primary references

- [Firebase Android SDK data disclosures](https://firebase.google.com/docs/android/play-data-disclosure)
- [Google Analytics Play disclosures](https://support.google.com/analytics/answer/11582702)
- [Firebase privacy and retention](https://firebase.google.com/support/privacy/)
- [Analytics data collection controls](https://firebase.google.com/docs/analytics/android/configure-data-collection)
- [Crashlytics collection controls](https://firebase.google.com/docs/crashlytics/android/customize-crash-reports)
- [Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)
- [Vercel Privacy Notice](https://vercel.com/legal/privacy-notice)

The policy now explicitly describes 2 Hz per-sensor raw samples, 10 Hz touch-move samples, background SDK handoff, bounded memory, process-kill/overflow loss and Analytics breadcrumbs in crash reports. This source update is not a live deployment or a legal approval for worldwide default-on collection.
