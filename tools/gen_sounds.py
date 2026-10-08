#!/usr/bin/env python3
"""Genera gli effetti sonori dell'app in app/src/main/res/raw (WAV mono 16 bit, 44,1 kHz).

Sono suoni sintetizzati (sinusoidi con armoniche e rumore filtrato), quindi non hanno
licenze di terzi. Per cambiarne uno basta ritoccare la sua ricetta e rilanciare lo script.
"""
import math
import os
import random
import struct
import wave

RATE = 44100
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app/src/main/res/raw")


def silence(seconds):
    return [0.0] * int(RATE * seconds)


def mix(*tracks):
    out = [0.0] * max(len(t) for t in tracks)
    for t in tracks:
        for i, v in enumerate(t):
            out[i] += v
    return out


def at(offset, track):
    return silence(offset) + track


def tone(freq, seconds, decay, harmonics=((1, 1.0),), attack=0.004, glide=1.0):
    """Nota che si spegne in modo esponenziale; glide > 1 la fa salire, < 1 scendere."""
    n = int(RATE * seconds)
    out = []
    phases = [0.0] * len(harmonics)
    for i in range(n):
        t = i / RATE
        f = freq * (glide ** (i / n))
        env = min(1.0, t / attack) * math.exp(-t * decay)
        v = 0.0
        for k, (mult, amp) in enumerate(harmonics):
            phases[k] += 2 * math.pi * f * mult / RATE
            v += amp * math.sin(phases[k])
        out.append(v * env)
    return out


def noise(seconds, decay, smooth, attack=0.01, seed=1):
    """Rumore addolcito da un filtro passa-basso: più alto è smooth, più è cupo."""
    rnd = random.Random(seed)
    n = int(RATE * seconds)
    out, prev = [], 0.0
    for i in range(n):
        t = i / RATE
        prev += (rnd.uniform(-1, 1) - prev) / smooth
        out.append(prev * min(1.0, t / attack) * math.exp(-t * decay))
    return out


BELL = ((1, 1.0), (2.0, 0.45), (3.01, 0.22), (4.2, 0.1))
SOFT = ((1, 1.0), (2, 0.25))


def note(name):
    names = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}
    semis = names[name[0]] + (1 if "#" in name else 0) + 12 * (int(name[-1]) - 4) - 9
    return 440.0 * 2 ** (semis / 12)


SOUNDS = {
    # Tocco di un pulsante: breve e morbido.
    "tap": (tone(820, 0.05, 70, SOFT, glide=0.7), 0.35),
    # Carta che si gira: fruscio.
    "flip": (mix(noise(0.22, 16, 3, attack=0.04), tone(240, 0.12, 30, glide=1.8)), 0.5),
    # Sfida completata: due note che salgono.
    "success": (mix(tone(note("E5"), 0.5, 9, BELL), at(0.09, tone(note("B5"), 0.6, 8, BELL))), 0.55),
    # Premio o traguardo: arpeggio luminoso.
    "win": (
        mix(*[at(0.085 * i, tone(note(n), 0.9, 5.5, BELL)) for i, n in enumerate(["C5", "E5", "G5", "C6", "E6"])]),
        0.6,
    ),
    # La temperatura sale.
    "level": (mix(tone(330, 0.45, 5, SOFT, attack=0.05, glide=2.0), at(0.3, tone(note("A5"), 0.7, 7, BELL))), 0.55),
    # Dado che batte sul tavolo.
    "dice": (mix(tone(170, 0.09, 45, glide=0.6), noise(0.05, 80, 2, attack=0.001, seed=3)), 0.6),
    # Scatto della ruota.
    "tick": (mix(tone(1900, 0.02, 220, glide=0.8), noise(0.012, 300, 1.5, attack=0.0005, seed=5)), 0.4),
    # Fine del timer: due rintocchi.
    "bell": (mix(tone(note("A5"), 1.1, 4.5, BELL), at(0.32, tone(note("A5"), 1.2, 4.0, BELL))), 0.6),
    # Passo, PIN errato: due note basse che scendono.
    "error": (mix(tone(260, 0.16, 16, SOFT), at(0.12, tone(196, 0.26, 12, SOFT))), 0.5),
}

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, (samples, peak) in SOUNDS.items():
        top = max(abs(v) for v in samples) or 1.0
        fade = int(RATE * 0.01)
        data = bytearray()
        for i, v in enumerate(samples):
            v = v / top * peak * min(1.0, (len(samples) - i) / fade)
            data += struct.pack("<h", int(v * 32767))
        with wave.open(os.path.join(OUT, f"sfx_{name}.wav"), "wb") as f:
            f.setnchannels(1)
            f.setsampwidth(2)
            f.setframerate(RATE)
            f.writeframes(bytes(data))
        print(f"sfx_{name}.wav  {len(samples) / RATE:.2f}s")
