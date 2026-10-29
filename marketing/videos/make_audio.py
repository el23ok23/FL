#!/usr/bin/env python3
"""Música original sintetizada para los videos de Fletaba (sin samples de terceros)."""
import numpy as np, wave, sys, math
SR = 44100
def note(freq, dur, kind="sine", vol=1.0, attack=0.01, release=0.08):
    t = np.arange(int(SR*dur))/SR
    if kind == "sine": w = np.sin(2*np.pi*freq*t)
    elif kind == "saw": w = 2*(t*freq - np.floor(0.5 + t*freq))
    elif kind == "square": w = np.sign(np.sin(2*np.pi*freq*t))
    elif kind == "pluck":
        w = np.sin(2*np.pi*freq*t) + 0.5*np.sin(4*np.pi*freq*t) + 0.25*np.sin(6*np.pi*freq*t)
        w *= np.exp(-t*4)
    env = np.ones_like(t)
    a = int(SR*attack); r = int(SR*release)
    if a: env[:a] = np.linspace(0, 1, a)
    if r: env[-r:] *= np.linspace(1, 0, r)
    return w*env*vol
def kick(dur=0.35):
    t = np.arange(int(SR*dur))/SR
    f = 120*np.exp(-t*18)+45
    return np.sin(2*np.pi*np.cumsum(f)/SR)*np.exp(-t*9)*1.2
def hat(dur=0.06, vol=0.25):
    n = np.random.default_rng(1).standard_normal(int(SR*dur))
    return n*np.exp(-np.arange(len(n))/SR*80)*vol
def snare(dur=0.18):
    n = np.random.default_rng(2).standard_normal(int(SR*dur))
    t = np.arange(len(n))/SR
    return (n*0.6 + np.sin(2*np.pi*190*t)*0.5)*np.exp(-t*22)*0.9
def whoosh(dur=0.7, vol=0.5):
    n = np.random.default_rng(3).standard_normal(int(SR*dur))
    t = np.arange(len(n))/SR
    env = np.sin(np.pi*t/dur)**2
    # filtro pasabajo simple (promedio móvil creciente → barrido)
    k = 12; kernel = np.ones(k)/k
    return np.convolve(n, kernel, mode="same")*env*vol
def ding(freq=1046.5, dur=1.2, vol=0.5):
    t = np.arange(int(SR*dur))/SR
    return (np.sin(2*np.pi*freq*t)+0.4*np.sin(2*np.pi*freq*2*t))*np.exp(-t*3)*vol
NOTES = {"C":261.63,"D":293.66,"E":329.63,"F":349.23,"G":392.0,"A":440.0,"B":493.88}
def freq(name, octave=4):
    return NOTES[name]*(2**(octave-4))
def build(total, bpm, prog, seed, lead=True, events=()):
    beat = 60/bpm
    out = np.zeros(int(SR*total))
    def add(sig, at):
        i = int(SR*at)
        if i >= len(out): return
        s = sig[:len(out)-i]
        out[i:i+len(s)] += s
    nbars = int(total/(beat*4))+1
    rng = np.random.default_rng(seed)
    for bar in range(nbars):
        root, third, fifth = prog[bar % len(prog)]
        t0 = bar*beat*4
        # pad (acorde)
        for n, o, v in ((root, 3, .09), (third, 4, .07), (fifth, 4, .07), (root, 4, .05)):
            add(note(freq(n, o), beat*4, "saw", v, attack=0.15, release=0.4), t0)
        # bajo
        for b in range(4):
            add(note(freq(root, 2), beat*0.9, "sine", .55, release=0.1), t0+b*beat)
            add(note(freq(root, 2), beat*0.4, "square", .08, release=0.05), t0+b*beat+beat/2)
        # batería
        for b in range(4):
            add(kick(), t0+b*beat)
            if b in (1, 3): add(snare(), t0+b*beat)
            for h in range(2): add(hat(vol=.18 if h else .3), t0+b*beat+h*beat/2)
        # melodía (pluck) sobre notas del acorde
        if lead:
            pool = [third, fifth, root]
            for s in range(8):
                if rng.random() < 0.55:
                    n = pool[rng.integers(0, 3)]; o = 5 if rng.random() < .6 else 4
                    add(note(freq(n, o), beat*0.5, "pluck", .35), t0+s*beat/2)
    for kind, at in events:
        if kind == "whoosh": add(whoosh(), at)
        if kind == "ding": add(ding(), at)
    # fade in/out y normalización
    fi = int(SR*0.3); fo = int(SR*1.2)
    out[:fi] *= np.linspace(0, 1, fi); out[-fo:] *= np.linspace(1, 0, fo)
    out = out/np.max(np.abs(out))*0.85
    return out
def save(path, mono):
    stereo = np.stack([mono, mono*0.97], axis=1)
    data = (stereo*32767).astype(np.int16)
    with wave.open(path, "wb") as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(data.tobytes())
if __name__ == "__main__":
    out, total, bpm, variant = sys.argv[1], float(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4])
    events = [tuple(e.split(":")) for e in sys.argv[5:]]
    events = [(k, float(v)) for k, v in events]
    progs = {
        1: [("C","E","G"),("G","B","D"),("A","C","E"),("F","A","C")],
        2: [("F","A","C"),("C","E","G"),("G","B","D"),("A","C","E")],
        3: [("D","F","A"),("A","C","E"),("F","A","C"),("G","B","D")],
        4: [("A","C","E"),("F","A","C"),("C","E","G"),("G","B","D")],
        5: [("G","B","D"),("D","F","A"),("E","G","B"),("C","E","G")],
    }
    save(out, build(total, bpm, progs[variant], seed=variant, lead=True, events=events))
    print("audio", out)
