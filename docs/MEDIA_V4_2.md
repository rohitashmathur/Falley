# Falley 4.2 media

This change adds one original retro background composition and distinct cosmetic jump poses.
It does not change avatar weight, hitboxes, jumping power, steering or hazard collisions.

## Original soundtrack

- Composition: **Falling Circuit**, generated specifically for Falley in 2026.
- Source: `scripts/generate_music.py`. Oscillators and score are written in source; no samples,
  recordings, downloaded music or other external audio were used.
- Bundled file: `app/src/main/res/raw/falley_retro_loop.wav`.
- Format: 16.0 seconds, 120 BPM, eight bars, mono 16-bit PCM, 22,050 Hz, 705,644 bytes.
- SHA-256: `a9c3c82f7eeebb9b0fe2f33811d8840294e5e466eb4b82edcceea40ae0df18f3`.
- File peak: 0.4200 full scale. File RMS: 0.1662. Both loop endpoints are zero, with 12 ms fades.
- Music playback gain is 0.16, separately from the three SoundPool effects voices at 0.45.
  Even coinciding theoretical maximum music/effects peaks remain below 0.81 full scale.
- No new permission, background service, remote audio or network request is introduced.

Regenerate with `python scripts/generate_music.py`. Verify bundled bytes without rewriting them
with `python scripts/generate_music.py --check`.

## Playback integration

`FalleyAudio.setMusicEnabled(boolean)` is separate from Sound Effects. The activity passes the
saved Music preference here. `setMusicPlaying(boolean)` sets screen eligibility; call it
idempotently whenever the screen changes or is drawn. Music requires all three gates:
Music enabled, activity foreground, and an eligible screen. Existing `setForeground(false)`
suspends playback and abandons focus; `release()` releases both MediaPlayer and SoundPool.
`stopAll()` affects only one-shot effects, so resetting hazard sounds does not restart the score.

A system audio-focus interruption pauses the loop, and a transient focus restoration resumes
only while all gates are still satisfied. Permanent focus loss does not repeatedly steal focus
back from another app. API 24/25 use the compatibility focus path; API 26+ use AudioFocusRequest.
A decoder/error/focus failure produces silence without affecting gameplay or effects.

## Cosmetic avatar styles

The approved renderer signature is unchanged. `AvatarPose.sample(avatar, vy, landingPulse)`
provides bounded, smooth coordinates from actual vertical velocity. Contact compression
always overrides the airborne pose. Grounded and compressed feet meet the same contact line.

| Avatar | Rising gesture |
| --- | --- |
| Human | Familiar raised-arm jump |
| Demon | Wide bat-like wing fan |
| Angel | Lifted feather fan and graceful spread |
| Military | Tucked knees and feet |
| Astronaut | Open floating pose |
| Robot | Folded spring/piston legs |
| Shadow | Narrow, elongated rise |

Wings, tuck and stretch are visual only. Physics owns the same body and collision checks for
all avatars. No trails or landing-effect mechanics are added by this media change.

## Verification and limits

- `AvatarPoseTest`: 112,384 checks for visible differences, contact alignment, invalid inputs,
  bounded coordinates and continuous velocity response.
- `python scripts/test_media.py --jdk-root <JDK17>`: validates exact generated PCM plus 56
  checks against the actual music/effects controllers using fake media services. Covers both
  API compatibility paths, independent toggles, idempotent updates, lifecycle, focus loss,
  late callbacks, decoder failure and one-time resource release.
- The media classes compile with Java 8 settings against Android 36.
- The geometry sheet in local `build/avatar-motion-preview/avatar-motion.png` was visually
  inspected for each avatar's idle, rising, falling and landing silhouettes.
- These tests do not replace real Android audio-focus checks or listening on speakers and
  headphones. Verify that the quiet loop remains audible and that hazard cues stay distinct
  during a physical-device playtest. Loop playback latency/gap depends on the device decoder.
