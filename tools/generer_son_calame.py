"""Bruit d'un calame (plume de roseau) qui écrit ميزان de droite à gauche, en 1,4 s.
Traits : م ي ز ا ن, puis les points. Généré par synthèse, sans échantillon externe."""
import numpy as np, sys
from scipy.io import wavfile
from scipy.signal import butter, lfilter
SR = 44100
DUR = 1.6
rng = np.random.default_rng(11)
n = int(SR * DUR); out = np.zeros(n)

def band(x, lo, hi, order=2):
    b, a = butter(order, [lo/(SR/2), hi/(SR/2)], btype='band'); return lfilter(b, a, x)

def stroke(t0, t1, gain=1.0, bright=1.0):
    i0, i1 = int(t0*SR), int(t1*SR); m = i1 - i0; t = np.arange(m)/SR
    noise = rng.standard_normal(m)
    # grain du papier : modulation rapide et irrégulière
    grain = np.abs(band(rng.standard_normal(m), 30, 180)); grain /= grain.max() + 1e-9
    body = band(noise, 1800*bright, 7500) * (0.45 + 0.55*grain)
    low = band(noise, 500, 1500) * 0.35
    s = body + low
    # pression du calame : attaque, appui, levée
    env = np.minimum(t/0.025, 1) * np.minimum((m/SR - t)/0.05, 1)
    env *= 0.75 + 0.25*np.sin(np.pi * t/(m/SR))
    out[i0:i1] += gain * s * np.clip(env, 0, 1)

def tap(t0, gain=0.9):
    i0 = int(t0*SR); m = int(0.035*SR); t = np.arange(m)/SR
    s = band(rng.standard_normal(m), 1200, 6000) * np.exp(-t/0.006)
    s += 0.6*np.sin(2*np.pi*900*t)*np.exp(-t/0.01)
    out[i0:i0+m] += gain * s

tap(0.0, 0.6)                      # le calame se pose
stroke(0.01, 0.27, 1.0, 1.0)       # م
stroke(0.29, 0.47, 0.9, 1.1)       # ي
stroke(0.49, 0.63, 0.85, 1.2)      # ز
stroke(0.65, 0.79, 1.0, 0.9)       # ا (trait vertical)
stroke(0.82, 1.18, 1.0, 1.0)       # ن (courbe)
for t0 in (1.24, 1.29, 1.34, 1.38): tap(t0, 0.8)   # les points
out = out / np.max(np.abs(out)) * 0.55
wavfile.write(sys.argv[1], SR, (out*32767).astype(np.int16))
