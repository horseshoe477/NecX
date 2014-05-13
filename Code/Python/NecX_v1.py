from pandas import read_csv
import matplotlib.pyplot as plt
import numpy as np
from numpy import fft       # FT
from scipy import fftpack   # DFT

# ===============================================
def movingAvg(values,window):
    weigths = np.repeat(1.0, window)/window
    smas = np.convolve(values, weigths, 'valid')
    return smas # as a numpy array
# ===============================================
def truncate(x,size):
    return x[size:len(x)-size]
# ===============================================

DB = '/Volumes/Data/local/download/Research/NecX/GitHub/NecX/Data/20140508/'
filename  = 'NecX_LTilt.csv'
Root = DB + filename
f = open(Root)
lines = f.readlines()
if filename=='NecX_LRotate.csv':    # Fix incorrect format in the data set
    lines = [l.split('\r') for l in lines][0]
f.close()

# <codecell>

# ====================================
# Read raw data
# ====================================
ch = 2

# Fill-in data array
rawData = [[0]*len(lines) for i in range(ch)]
for idx in range(len(rawData[0])):
    tokens = lines[idx].split(',')
    if len(tokens)==3:
        rawData[0][idx] = (float)(tokens[0])
        rawData[1][idx] = (float)(tokens[2])
        
# Plotting
plt.close('all')
#plt.close(f1)
x = np.linspace(0, len(rawData[0]), num=len(rawData[0]))
f1, ax1 = plt.subplots(ch, sharex=True, sharey=False, figsize=(10, 6))
for i in range(ch):
    ax1[i].plot(x, rawData[i])
    ax1[i].set_ylabel('ch'+str(i))
plt.title('Raw')

# <codecell>

# ====================================
# Moving average
# ====================================
smoothWinSize = 50
smoData = [None]*2
for i in range(ch):
    smoData[i] = movingAvg(rawData[i], smoothWinSize)
    
# Plotting
plt.close(f2)
x = np.linspace(0, len(smoData[0]), num=len(smoData[0]))
f2, ax2 = plt.subplots(ch, sharex=True, sharey=False, figsize=(10, 6))
for i in range(ch):
    ax2[i].plot(x, smoData[i])
plt.title('Smoothed')

# <codecell>

# ====================================
# 1st Derivative
# ====================================
#difData = [[0]*len(smoData[0]) for i in range(ch)]   # Initialize the array
difData = [None]*2
for i in range(ch):
    difData[i] = np.diff(smoData[i])

# Plotting
plt.close(f3)
x = np.linspace(0, len(difData[0]), num=len(difData[0]))
f3, ax3 = plt.subplots(ch, sharex=True, sharey=True, figsize=(10, 6))
for i in range(ch):
    ax3[i].plot(x, difData[i])
    ax3[i].set_ylabel('ch'+str(i))
plt.title('1st Derivative')

# <codecell>

# ====================================
# Absolute of the 1st Derivative
# ====================================
for i in range(ch):
    difData[i] = np.absolute(difData[i])

# Abs of 1st Derivative
plt.close(f4)
x = np.linspace(0, len(difData[0]), num=len(difData[0]))
f4, ax4 = plt.subplots(ch, sharex=True, sharey=True, figsize=(10, 6))
for i in range(ch):
    ax4[i].plot(x, difData[i])
    ax4[i].set_ylabel('ch'+str(i))
plt.title('Abs of 1st Derivative')

# <codecell>

# ====================================
# Sum of absolute of the 1st Derivative
# ====================================
segSmoWinSize = 80
sumOfDif = movingAvg([x + y for x, y in zip(difData[0], difData[1])], segSmoWinSize)
dif_sumOfDif = movingAvg(np.diff(sumOfDif), segSmoWinSize)

# Plotting
plt.close(f5)
f5, ax5 = plt.subplots(ch, sharex=True, sharey=False, figsize=(10, 6))

x = np.linspace(0, len(sumOfDif), num=len(sumOfDif))
ax5[0].plot(x, sumOfDif)
ax5[0].set_ylabel('Sum of Diff')

x = np.linspace(0, len(dif_sumOfDif), num=len(dif_sumOfDif))
ax5[1].plot(x, dif_sumOfDif)
ax5[1].set_ylabel('Diff of Sum of Diff')

# <codecell>

# ====================================
# Segmentation
# ====================================
Th = 0.02
slideWinSize = 300
minWin = slideWinSize*0.5
centerShift = smoothWinSize*2.3
segSize = smoothWinSize*2.35

segStart = []
segEnd = []
segCenter = []    # Center index of a segmentation
i = 0
while i < len(dif_sumOfDif)-1:
    i = i + 1
    hit = False
    if dif_sumOfDif[i-1]<Th and dif_sumOfDif[i]>=Th:
        start = i
        while i < start+slideWinSize and i < len(dif_sumOfDif)-1:
            i = i + 1
            if dif_sumOfDif[i-1] < -Th and dif_sumOfDif[i] >= -Th:
                end = i
                if end-start > minWin:   # Gesture length is not too short (half window size)
                    hit = True
        if hit == True:
            segStart.append(start)
            segEnd.append(end)
            segCenter.append(math.floor((start+end)/2) + centerShift)
            hit = False
            i = end+1
            
for i in range(len(segCenter)):
    # Plot the segment
    ax2[0].axvspan(segCenter[i]-segSize, segCenter[i]+segSize, facecolor='g', alpha=0.25)
    ax2[1].axvspan(segCenter[i]-segSize, segCenter[i]+segSize, facecolor='g', alpha=0.25)
    # Plot the threshold line
    ax5[0].axvspan(segStart[i]+50, segEnd[i]+50, facecolor='r', alpha=0.25)
    ax5[1].axhline(y=Th, linewidth=2, color='r')
    ax5[1].axhline(y=-Th, linewidth=2, color='r')

