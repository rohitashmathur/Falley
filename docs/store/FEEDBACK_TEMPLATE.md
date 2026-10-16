# Falley beta feedback

Use Play's private test feedback or the owner's eventual support channel. This file is a template, not a hosted form. Do not request sensitive personal information.

## Short form

```text
Falley version shown / test date:
Phone model / Android version:
Motion or touch? Motion preset if used:
Campaign stage or Arcade difficulty/hazard mix:

What happened? What did you expect?
Can you repeat it? Steps, if known:
Any crash, freeze, unfair hit, severe stutter or overheating?
Could you distinguish fire, laser, lightning and saw sounds?
Sound acceptable on speaker/headphones? Too quiet or harsh?
Was there a moment you wanted to stop? Why?
Would you choose another run? Yes / Maybe / No, with a reason:

Optional screenshot/recording; remove notifications and private information.
```

## Low-noise test plan: recommendation

Use 3–5 trusted internal testers first, covering a low/mid-range phone, a newer phone and, if available, a phone without a gyro. After the Play install/update path works, widen to the closed cohort. Recruit a few extra testers when the account's continuous-opt-in requirement applies, so a dropout does not invalidate the minimum. No fabricated installs or positive-review requirement.

Ask for three short sessions spread over the test window, roughly five minutes each:

1. **First impression:** clean Play install, enter Settings and Privacy Policy, try motion, all three response presets, then touch steering and jump. Confirm sound/blood/vibration switches and readable controls.
2. **Progress and fairness:** play early and later campaign stages, check target landing, then try fire, laser, lightning and saw in Arcade. Report a specific unfair or unclear moment rather than only a difficulty score.
3. **Return and interruptions:** verify stored settings/records, switch away/back during a run, lock/unlock, pause/restart, try offline play and speakers/headphones. Test phone-call or other-audio interruptions only when convenient and safe. Stop if sound is uncomfortable or the device gets excessively hot.

Send one invitation and one midpoint reminder only if feedback is missing; do not create daily nagging. Triage daily internally, acknowledge actionable reports, and batch routine fixes into deliberate versioned updates. Keep urgent crash/data-loss issues separate from balance opinions.

## Owner's feedback log

| Date | Build | Device/Android | Issue and reproduction | Severity | Decision/fix | Retest result |
| --- | --- | --- | --- | --- | --- | --- |
| | | | | | | |

Before production: resolve reproducible crashes/ANRs and broken install/input/privacy flows; gather listening feedback on real phones; review motion comfort and hazard fairness; retain evidence of feedback-driven changes. Simulation controller successes and emulator captures are not substitutes for these sessions.
