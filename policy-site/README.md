# Falley privacy policy website

A plain, responsive HTML page for Falley by Perf Games. No packages, JavaScript, fonts, analytics or build step. `public/index.html` is the complete page, including its styles and small vector brand illustration. Open it directly in a browser to preview it.

## Published policy

- Production URL: **https://falley-privacy-policy.vercel.app/**
- Public support/privacy email: **mathurrohitash16@gmail.com**
- Published by the owner; checked in a browser on **23 October 2026**. The policy opens without a login and displays all eight sections, app/publisher identification and the correct contact link. The visible layout was inspected. This check does not establish availability from every country.

Use the production URL above in Play Console. The contact was first added in **3.3.1/code 11**; current app builds include this contact, public URL and browser/email actions in its offline policy. See [app update notes](../docs/PRIVACY_UPDATE_3_3_1.md). The older 3.3.0/code 10 files do not include that contact; website publication does not change already installed builds.

## Before publishing

The page uses the supplied privacy contact **mathurrohitash16@gmail.com**. Monitor this inbox for privacy inquiries. The contact section says that Gmail processes correspondence, messages are used to respond to inquiries, they are kept only as long as necessary to resolve the inquiry (unless law requires longer), and senders can request deletion. Review and follow these handling commitments when publishing. No legal company identity beyond the supplied Perf Games publisher branding has been invented. Perf Games must match the publisher branding used in your store listing.

The local page reflects the **4.3** source: temporary network/SIM/device-region country hints, English-default language choice, local stars/avatar/rope preferences, Daily records and explicit result-image sharing, and bundled music with independent controls. The live page was checked read-only on **25 October 2026** and still showed the older **23 October** text. Redeploy `public/index.html` before distributing the matching beta. No Vercel account/project authentication is configured in this workspace; source publication alone does not update the public site.

## Deploy on Vercel

Deploy **this `policy-site` directory**, not the Android repository root. The included `vercel.json` serves only `public/`, keeps deployment instructions out of the served website, skips installation/building, adds response security headers and provides `/privacy` and `/privacy-policy` aliases for the same policy.

If importing the Falley GitHub repository after these files have been added to it, use:

| Vercel setting | Value |
| --- | --- |
| Root Directory | `policy-site` |
| Framework Preset | Other |
| Build Command | Override with an empty value |
| Install Command | Override with an empty value |
| Output Directory | `public` |

Alternatively, extract the supplied ZIP and deploy its `policy-site` folder using your normal Vercel workflow. These website files are prepared locally; this task does not create a Vercel project or deploy anything.

After deployment:

1. Open the production HTTPS URL while signed out, preferably on another device. It must show the full policy without a Vercel login, password or country restriction.
2. Check the contact email, text and mobile layout. Keep Web Analytics, Speed Insights and other injected tracking disabled unless you revise the policy to describe them.
3. Use the stable production root URL (or `/privacy`) for the Google Play privacy-policy field. Share that URL so it can be linked from the app and publishing documents.
4. Keep the URL live and update this page when the app's data practices change. If you use another host, adjust the Vercel-specific website paragraph.

Google Play requires a public privacy policy even for apps that do not collect personal data, including developer identity, a privacy contact mechanism and retention/deletion information. This page covers the app behavior verified in source and the supplied public contact.

## References

- [Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)
- [Vercel static-site build settings](https://vercel.com/docs/builds/configure-a-build)
- [Vercel Privacy Notice](https://vercel.com/legal/privacy-notice)

## Verification

Checked on 23 October 2026:

- The local static server returned HTTP 200 for the complete policy.
- All five internal navigation links resolve; all eight policy sections are present.
- The supplied email link is present, with no remaining publication placeholders.
- No scripts, forms, frames or external asset dependencies are included.
- The Vercel configuration parses correctly and serves only `public/`.
- A second source review found no mismatch with the current app's data practices.

Local browser preview was blocked during initial preparation; the production page was subsequently opened and visually inspected after the owner published it. The production root URL and displayed content are verified. The optional Vercel aliases and response headers have not been independently checked. The 24 October local revision adds the sharing disclosure and record types without scripts or external assets. It has not been deployed or rechecked at the production URL. No application permissions or game behavior are changed by these website files.

4.3 changes avatar physics, boss presentation and local layouts only; it adds no network service or data collection. Firebase/Analytics and their necessary policy changes are deferred to 4.4.
