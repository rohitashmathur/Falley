# Offline beta: proposed app-content answers

**Publication update — 23 October 2026:** Use **https://falley-privacy-policy.vercel.app/** for the public policy and **mathurrohitash16@gmail.com** for support/privacy contact. These two fields are no longer unset; the table below preserves the original 3.2 draft. The **3.3.1/code 11** local app builds now include the real in-app contact and policy URL. Current app/artifact declarations still require owner review; see [privacy publishing notes](privacy/PUBLISHING_NOTES.md) and [current build notes](../PRIVACY_UPDATE_3_3_1.md).

Prepared for **3.2.0 / code 7** only. This is a draft for an owner to review against the **final release AAB**, its merged manifest, bundled code and active Play tracks. Nothing has been submitted. Console labels and conditional questions can change; answer the meaning of the live question rather than paste an incompatible form.

## Data safety

| Topic shown by Console | Proposed response and basis |
| --- | --- |
| Does the app collect or share any required user-data types? | **No**, provided final artifact review confirms the current offline implementation. No Internet permission, analytics, ads, backend or data-upload feature is included. |
| Local records and preferences | Scores, times, stage/configuration choices, sound, vibration, blood and motion settings stay in app-local storage. |
| Motion sensors | Optional accelerometer/gyroscope samples are processed transiently on device for steering, not recorded or transmitted. |
| Account creation | **No account creation**. No sign-in provider or credential collection. |
| Account deletion URL | Not applicable to this account-free build. Do not enter a fake URL. Local records can be removed using Android Clear storage or uninstalling. |
| Data encrypted in transit | No app data transfer occurs. Follow the conditional **No collection** form; do not claim a transport-encryption feature or independent security certification. |
| Independent security review / certification | **No**, if asked. Code review and automated tests are not an accredited independent audit. |
| Public privacy-policy URL | **UNSET / BLOCKER.** Publish the completed policy and enter its real URL. A repository file or local HTML preview is not a published policy. |

Google excludes data processed only on the device from the definition of collected data. Closed/open/production tracks require the Data safety form even for no-collection apps; an app exclusively on internal testing is exempt. App/SDK changes require reassessment. [Data safety rules](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)

This draft concerns the game itself. If testers voluntarily send feedback through Play, email or a separate form, those services handle that communication separately. Do not add in-game feedback upload, crash analytics or an ad SDK while continuing to rely on these offline answers.

## Other app-content declarations

| Declaration | Proposed response / owner task |
| --- | --- |
| Contains ads | **No.** No advertising is implemented in this beta. Future AdMob work needs a new verified build and updated answers. |
| Advertising ID | The app does **not use Advertising ID**. Confirm the final manifest contains no AD_ID permission or advertising SDK. |
| App access | All functionality available without special access; use `REVIEWER_ACCESS.md`. |
| Payments and purchases | No in-app purchases, subscriptions, real-money rewards, gambling or paid unlocks. |
| News, health, financial, government, social/dating or user-generated-content functionality | None of these are features of this game. Answer any relevant Console declarations accordingly, reviewing their actual wording. |
| App category | Game; **Arcade** recommended. |
| Target audience | **Owner decision pending.** Recommend an adult-focused beta, selecting 18+ only if that truthfully describes the intended audience and marketing. This is not a rating certificate. Do not select child audience groups merely to increase reach. |
| Content rating | Complete the Games questionnaire truthfully using the content inventory below. No rating has been obtained and no exact rating is promised. |
| Support email | **UNSET / BLOCKER.** A reachable owner-controlled address is required. |
| Developer/publisher identity | Perf Games is the game branding. Complete Console's legal account verification with the actual person/entity; do not invent company status or legal details. |

## IARC content inventory for the owner

These are factual inputs, not a pre-filled answer to every possible branch:

- A human-looking, abstract figure can die in accidental game hazards. Violence is fictional and stylized, but it involves a human figure.
- Blood particles/pools and bone fragments are present with Blood Effects on, which is the default. Laser deaths melt the figure; saw deaths visibly separate the figure; spikes can pin the corpse; fire/lightning char the figure and turn it to ash. Later missed landings show injury/death.
- Hazard sounds include synthetic fire, electrical cracks/zaps and saw effects. Review the final sound presentation when answering impact/intensity questions.
- No sexual content, sexual violence, profanity, drug/alcohol themes, real people, discriminatory targets, realistic photographs, gambling, public chat or user-generated content is implemented.
- The blood toggle does not remove all visible death reactions. Declare accessible content, not only the least graphic setting or selected store screenshots.

Ratings are assigned by regional authorities from complete questionnaire answers and may differ by territory. Including children in the intended audience adds Families obligations; audience selection and content rating are separate. Review the calculated certificate and any territory restrictions before selecting countries. [Content ratings and target audience](https://support.google.com/googleplay/android-developer/answer/9859655?hl=en)

Final review should assess whether any effect appears gratuitous or realistically graphic; a mature rating does not itself guarantee compliance. Keep the beta audience, store copy, screenshots and shipped presentation consistent. [Play violence policy](https://support.google.com/googleplay/android-developer/answer/18258653?hl=en)
