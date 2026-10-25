#!/usr/bin/env python3
"""Generate Falley's original quiet retro loop. Standard-library PCM synthesis, no samples.

Composition: 'Falling Circuit', Perf Games / Falley contributors, 2026.
All melody, arrangement and oscillators are written here; no external recording is used.
Run from anywhere. The deterministic output is checked before it is written.
"""
from array import array
from pathlib import Path
import math
import struct
import sys
import wave

RATE = 22050
BPM = 120
BEAT = 60.0 / BPM
BARS = 8
SECONDS = BARS * 4 * BEAT
FRAMES = round(RATE * SECONDS)
OUT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw/falley_retro_loop.wav'


def hz(midi):
    return 440 * 2 ** ((midi - 69) / 12)


def add_note(mix, beat, duration, midi, gain, voice='lead'):
    start = round(beat * BEAT * RATE)
    length = max(2, round(duration * BEAT * RATE))
    frequency = hz(midi)
    attack, release = min(.014, length / RATE * .2), min(.065, length / RATE * .35)
    for n in range(length):
        t = n / RATE
        remaining = (length - 1 - n) / RATE
        envelope = min(1, t / attack) * min(1, remaining / release)
        # Smooth low-amplitude harmonics evoke an old handheld without harsh square edges.
        phase = math.tau * frequency * t
        if voice == 'bass':
            tone = math.sin(phase) + .13 * math.sin(phase * 3)
            decay = .78 + .22 * math.exp(-8 * t)
        elif voice == 'arp':
            tone = math.sin(phase) + .16 * math.sin(phase * 2)
            decay = math.exp(-5 * t)
        else:
            tone = math.sin(phase) + .19 * math.sin(phase * 3) + .06 * math.sin(phase * 5)
            decay = .78 + .22 * math.exp(-7 * t)
        mix[(start + n) % FRAMES] += tone * gain * envelope * decay


def compose():
    mix = [0.0] * FRAMES
    # E minor colours with a brighter D-major turn; a deliberately sparse, original top line.
    chords = [(40, 55, 59, 62), (36, 52, 55, 59), (33, 52, 57, 60), (38, 54, 57, 64)]
    melody = [
        [(0, 76), (.75, 79), (1.5, 83), (2.75, 81)],
        [(.25, 79), (1.25, 76), (2, 74), (3, 71)],
        [(0, 76), (1, 81), (1.75, 79), (3, 76)],
        [(.5, 78), (1.25, 81), (2.5, 78), (3.25, 74)],
        [(0, 76), (.75, 79), (1.75, 83), (2.5, 86)],
        [(.25, 83), (1.25, 79), (2.25, 76), (3.25, 74)],
        [(0, 72), (1, 76), (2, 81), (2.75, 79)],
        [(.25, 78), (1.25, 74), (2.25, 71), (3.25, 74)],
    ]
    for bar in range(BARS):
        root, a, b, c = chords[bar % 4]
        for offset, note in [(0, root), (1.5, root + 12), (2.75, root + 7)]:
            add_note(mix, bar * 4 + offset, 1.1, note, .075, 'bass')
        for eighth in range(8):
            note = (a, b, c, b)[(eighth + bar % 2) % 4]
            add_note(mix, bar * 4 + eighth * .5, .40, note, .020, 'arp')
        for offset, note in melody[bar]:
            add_note(mix, bar * 4 + offset, .55 if offset < 3 else .43, note, .032)
    # A tiny equal-duration end/start fade guarantees an inaudible zero-crossing seam.
    fade = round(RATE * .012)
    for n in range(fade):
        fade_gain = (n / fade) ** 2 * (3 - 2 * n / fade)
        mix[n] *= fade_gain
        mix[-1 - n] *= fade_gain
    gain = .42 / max(abs(value) for value in mix)
    pcm = array('h', (round(max(-.9, min(.9, value * gain)) * 32767) for value in mix))
    return pcm


def check(pcm):
    peak = max(abs(s) for s in pcm) / 32768
    rms = math.sqrt(sum((s / 32768) ** 2 for s in pcm) / len(pcm))
    assert len(pcm) == FRAMES and .02 < peak < .65, f'Peak outside safe synthesis range: {peak}'
    assert .008 < rms < .2, f'Unexpected music loudness: {rms}'
    assert pcm[0] == pcm[-1] == 0, 'Loop boundary must begin and end at zero'
    assert max(abs(s) for s in pcm[:32] + pcm[-32:]) < 100, 'Boundary fade is not quiet'
    assert all(abs(s) < 32767 for s in pcm), 'PCM clipping'
    return peak, rms


def main():
    pcm = compose()
    peak, rms = check(pcm)
    # PCM output is little-endian on every host, including a future non-Windows generator.
    frames = b''.join(struct.pack('<h', sample) for sample in pcm)
    if '--check' in sys.argv:
        with wave.open(str(OUT), 'rb') as current:
            assert current.getnchannels() == 1 and current.getsampwidth() == 2
            assert current.getframerate() == RATE and current.getnframes() == FRAMES
            assert current.readframes(FRAMES) == frames, 'Bundled loop differs from source composition'
    else:
        OUT.parent.mkdir(parents=True, exist_ok=True)
        with wave.open(str(OUT), 'wb') as result:
            result.setnchannels(1)
            result.setsampwidth(2)
            result.setframerate(RATE)
            result.writeframes(frames)
    assert OUT.stat().st_size < 1024 * 1024
    print(f'{OUT}: {SECONDS:.1f}s, PCM16 mono {RATE}Hz, peak={peak:.4f}, rms={rms:.4f}, bytes={OUT.stat().st_size}')


if __name__ == '__main__':
    main()
