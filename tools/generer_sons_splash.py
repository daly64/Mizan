import numpy as np, sys
from scipy.io import wavfile
from scipy.signal import butter, lfilter
SR=44100
def env(n, a=0.005, d=1.0):
    t=np.arange(n)/SR
    e=np.minimum(t/a,1.0)*np.exp(-t/d)
    return e
def bell(f, dur, decay):
    n=int(SR*dur); t=np.arange(n)/SR
    s=(np.sin(2*np.pi*f*t)+0.35*np.sin(2*np.pi*f*2.76*t)*np.exp(-t*3)+0.15*np.sin(2*np.pi*f*5.4*t)*np.exp(-t*6))
    return s*env(n,0.004,decay)
# Carillon : deux notes douces (balance qui se pose)
dur=1.6; n=int(SR*dur); chime=np.zeros(n)
b1=bell(1318.5,1.6,0.45); chime[:len(b1)]+=b1
o=int(0.12*SR); b2=bell(1975.5,1.6-0.12,0.55); chime[o:o+len(b2)]+=0.8*b2
chime*=0.5/np.max(np.abs(chime))
# Plume : frottement doux filtré, 1.4 s, avec petites variations
rng=np.random.default_rng(7); n2=int(SR*1.4); t=np.arange(n2)/SR
noise=rng.standard_normal(n2)
b,a=butter(2,[2500/(SR/2),7000/(SR/2)],btype='band'); scr=lfilter(b,a,noise)
strokes=0.55+0.45*np.abs(np.sin(2*np.pi*2.3*t))
fade=np.minimum(t/0.08,1)*np.minimum((1.4-t)/0.25,1)
scr*=strokes*fade
scr*=0.18/np.max(np.abs(scr))
wavfile.write('chime.wav',SR,(chime*32767).astype(np.int16))
wavfile.write('plume.wav',SR,(scr*32767).astype(np.int16))
