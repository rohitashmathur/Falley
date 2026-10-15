"""Rebuild Falley's original synthesized sound effects using Python's standard library.

Run: python scripts/generate_audio.py
Check bundled files without rewriting: python scripts/generate_audio.py --check
Writes mono 16-bit PCM WAVs at 24 kHz to app/src/main/res/raw. No recordings,
samples, external assets, packages or music are used. Each sound is generated
from oscillators and deterministic noise, with shaped attack/release and a
maximum 0.55 full-scale peak. Playback uses at most three voices at 0.45 gain,
leaving mixer headroom even when all three asset peaks coincide.
"""
from pathlib import Path
import math
import random
import struct
import sys
import wave

RATE = 24000
TAU = math.tau
DEST = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"
SOUNDS = (("fire_launch", .85), ("fire_burn", 1.05), ("laser_zap", .36),
          ("lightning_crack", .72), ("saw_motor", 1.00), ("saw_impact", .55),
          ("ui", .045), ("jump", .15), ("win", .36), ("loss", .21), ("rope", .09))


def render(name, duration, kind, seed):
    rng = random.Random(seed)
    samples, low, phase = [], 0.0, 0.0
    crackles = [(rng.uniform(0.015, duration * .88), rng.uniform(.008, .035)) for _ in range(24)]
    for i in range(round(duration * RATE)):
        t = i / RATE
        u = t / duration
        noise = rng.uniform(-1, 1)
        low += .038 * (noise-low)
        edge = min(1.0, t / .006) * min(1.0, (duration-t) / .035)
        if kind in ("fire_launch", "fire_burn"):
            # Low filtered-noise roar, irregular crackle and a windy high hiss.
            burn = kind == "fire_burn"
            phase += TAU * (72 + 35 * math.exp(-t*5) + 8*math.sin(t*11)) / RATE
            roar = low * 5.2 + math.sin(phase) * .18
            crackle = sum(math.exp(-(t-a)/b)*noise*.5 for a, b in crackles if 0 <= t-a < b*4)
            signal = (roar + crackle + noise*(.19 if burn else .09)) * (1-u)**.65
            if not burn:
                signal *= .35 + .65*math.sin(math.pi*u)**.6
        elif kind == "laser_zap":
            # Rapid descending FM chirp with a short electrical-noise tail.
            phase += TAU * (180 + 1950*math.exp(-t*13)) / RATE
            signal = (math.sin(phase + 2.0*math.sin(TAU*73*t))*.65 + noise*.13) * math.exp(-t*7)
        elif kind == "lightning_crack":
            # Three sharp broadband cracks, followed by a brief low thunder tail.
            impulse = sum(math.exp(-(t-a)*75) for a in (0, .027, .092) if t >= a)
            signal = noise*impulse*.95 + low*3*math.exp(-t*4) + math.sin(TAU*48*t)*math.exp(-t*8)*.18
        elif kind in ("saw_motor", "saw_impact"):
            impact = kind == "saw_impact"
            frequency = (185-95*u) if impact else (75+105*(1-math.exp(-t*8)))
            phase += TAU * frequency / RATE
            tooth = sum(math.sin(phase*k)/k for k in range(1, 10))
            rasp = math.sin(TAU*(1050+60*math.sin(t*17))*t)*.17 + noise*.25
            envelope = math.exp(-t*5) if impact else min(1, t/.08)*min(1,(duration-t)/.16)
            signal = (tooth*.55+rasp)*envelope
            if impact:
                signal += (math.sin(TAU*2100*t)+math.sin(TAU*3260*t))*.16*math.exp(-t*20)
        elif kind == "jump":
            phase += TAU*(240+530*u)/RATE
            signal = math.sin(phase)*math.sin(math.pi*u)**1.4
        elif kind == "win":
            note = (523.25, 659.25, 783.99)[min(2, int(u*3))]
            signal = (math.sin(TAU*note*t)+.2*math.sin(TAU*note*2*t)) * (.55+.45*math.sin(math.pi*u))
        elif kind == "loss":
            phase += TAU*(260-150*u)/RATE
            signal = (math.sin(phase)+noise*.08)*(1-u)
        elif kind == "rope":
            signal = (math.sin(TAU*145*t)+.27*math.sin(TAU*310*t))*math.exp(-t*18)
        else:
            signal = math.sin(TAU*760*t)*math.sin(math.pi*u)
        samples.append(math.tanh(signal) * edge)
    peak = max(abs(value) for value in samples)
    gain = .55 / max(peak, 1e-9)
    pcm = [round(value*gain*32767) for value in samples]
    DEST.mkdir(parents=True, exist_ok=True)
    path = DEST / ("falley_"+name+".wav")
    with wave.open(str(path), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(RATE)
        output.writeframes(struct.pack("<"+"h"*len(pcm), *pcm))
    rms = math.sqrt(sum((value/32767)**2 for value in pcm)/len(pcm))
    assert max(abs(value) for value in pcm) < 18023
    print(f"{path.name}: {duration:.2f}s, peak={max(abs(v) for v in pcm)/32767:.4f}, RMS={rms:.4f}, {path.stat().st_size} bytes")


def verify():
    distinct = set()
    for name, duration in SOUNDS:
        path = DEST / ("falley_"+name+".wav")
        with wave.open(str(path), "rb") as sound:
            assert sound.getnchannels() == 1 and sound.getsampwidth() == 2 and sound.getframerate() == RATE
            assert sound.getnframes() == round(duration*RATE)
            data = sound.readframes(sound.getnframes())
        pcm = struct.unpack("<"+"h"*(len(data)//2), data)
        peak = max(abs(value) for value in pcm)/32767
        rms = math.sqrt(sum((value/32767)**2 for value in pcm)/len(pcm))
        assert .54 <= peak <= .551 and rms > .02, f"unexpected level in {name}"
        assert abs(pcm[0]) < 650 and abs(pcm[-1]) < 650, f"missing edge fade in {name}"
        distinct.add(data)
        print(f"PASS {name}: mono PCM16/{RATE}Hz, {duration:.3f}s, peak {peak:.4f}, RMS {rms:.4f}")
    assert len(distinct) == len(SOUNDS), "duplicate clips"
    print("PASS: 11 distinct original clips, faded edges, no clipped PCM samples; max three-voice summed peak <= 0.743.")


if __name__ == "__main__":
    if "--check" in sys.argv:
        verify()
    else:
        for index, (name, duration) in enumerate(SOUNDS):
            render(name, duration, name, 3100+index)
