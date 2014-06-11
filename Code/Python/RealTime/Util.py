import numpy as np
from scipy import interpolate
from scipy import fftpack   # DFT

# ===================================
# Moving average
# ===================================
def movingAvg(values,window):
    values = myResample(values, len(values)+window-1)
    weigths = np.repeat(1.0, window)/window
    smas = np.convolve(values, weigths, 'valid')
    return smas # as a numpy array

# ===================================
# Reset the input data buffer
# ===================================
def resetBuffer(ch,purgeLength,rawData):
    for i in range(int(purgeLength)):
        if len(rawData[0])==0 or len(rawData[1])==0:
            break
        rawData[0].pop(0)
        rawData[1].pop(0)
    return rawData
# ===================================
# Find zero crossing
# ===================================
def findZeroCrossing(features1, features2):
    features1 = features1 - np.mean(features1)
    features2 = features2 - np.mean(features2)
    zero_crossing1 = np.where(np.diff(np.sign(features1)))[0]
    zero_crossing2 = np.where(np.diff(np.sign(features2)))[0]
    return len(zero_crossing1) + len(zero_crossing2)

# ===================================
# FFT
# ===================================
def findDominantFreq(val):
    # -------------------------------
    # FFT
    N = len(val)
    T = 4.0 / 1000.0     # Sample at 125 Hz
    xf = fftpack.fftfreq(N,T)
    yf = fftpack.fft(val)

    # Only reserve half of the spectrum
    psIdx = np.where(xf>0)
    spectrum = xf[psIdx]
    power = np.abs(yf[psIdx])**2   # Power spectrum

    return spectrum[np.where(power==max(power))[0]]


# ===================================
# Prepare feature vector
# ===================================
def getFeatureVector(features1, features2):
    strFeature = ''
    #
    maxCh1 = max(features1)
    minCh1 = min(features1)
    maxCh2 = max(features2)
    minCh2 = min(features2)
    selfCorr1 = max(np.correlate(features1, features1, 'full'))
    selfCorr2 = max(np.correlate(features2, features2, 'full'))
    crosCorr  = max(np.correlate(features1, features2, 'full'))
    #
    strFeature = strFeature + str(int(maxCh1 - maxCh2)) + ","
    strFeature = strFeature + str(int(minCh1 - minCh2)) + ","
    strFeature = strFeature + str(int(maxCh1 - minCh1)) + ","
    strFeature = strFeature + str(int(maxCh2 - minCh2)) + ","
    strFeature = strFeature + str(int(selfCorr1)) + ","
    strFeature = strFeature + str(int(selfCorr2)) + ","
    strFeature = strFeature + str(int(crosCorr)) + ","
    return strFeature

# ===================================
# Reshape the curve using interpolation
# ===================================
def myResample(data,newSize):
    try:
        N = len(data)
        x = np.linspace(0.0, N, N)
        f = interpolate.interp1d(x,data)
        # re-sampling
        newX = np.linspace(0.0, N, newSize)
        newData = f(newX)
    except ValueError:
        return None
    return newData