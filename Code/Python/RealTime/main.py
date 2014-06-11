import numpy as np
import matplotlib.pyplot as plt
from ML import segmentation
from Util import resetBuffer, getFeatureVector, findZeroCrossing
from mySocketClient import SocketClient

# ===========================================
# Parameters and Variables
# ===========================================
# Segmentation
smoothWinSize = 50
segSmoWinSize = 80
segMarginal = 80 #60
#
Th = 0.03
ThFire = 25   # Threshold to fire
slideWinSize = 400
#
ch = 2                                              # Number of channels
rawData = [[0]*slideWinSize for i in range(ch)]     # Data array
#
bIsOnFire = False

# ===========================================
# Accepting data for segmentation
# ===========================================
#sc = SocketClient("192.168.1.142",9999);    # Connect to the socket server
sc = SocketClient("127.0.0.1",9999);

print 'Connected!!'

while True:
    # ====================================
    # Add new element
    msg = sc.readline().split('==')
    if len(msg) == 2:
        try:
            ch0 = float(msg[0])
            ch1 = float(msg[1])
            rawData[0].append(ch0)
            rawData[1].append(ch1)
            if len(rawData[0])<slideWinSize+1:  # make sure the sliding window contains enough data points
                sc.writeline("NoHit")
                continue
            rawData[0].pop(0)
            rawData[1].pop(0)
        except ValueError, TypeError:
            continue
    else:
        continue

    # ====================================
    # Segmentation
    smoData,hit,segStart,segEnd = segmentation(ch, rawData, smoothWinSize, segSmoWinSize, Th,slideWinSize)

    # Find zero crossing
    startCheck = int(len(smoData[0])*0.3)
    zeroCross = findZeroCrossing(smoData[0][startCheck:-1], smoData[1][startCheck:-1])

    # ====================================
    # Feature extraction
    strFeature = ""
    if hit==False:
        sc.writeline("NoHit")       # No hit
        continue
    elif segStart!=-1 and segEnd!=-1 and hit==True:

        print zeroCross
        if zeroCross>ThFire:            # Fire
            sc.writeline("Fire")
        else:
            # ====================================
            features1 = smoData[0][segStart:segEnd] # ch1
            features2 = smoData[1][segStart:segEnd] # ch2
            # Zero mean
            features1 = features1 - np.mean(features1)
            features2 = features2 - np.mean(features2)
            # Segmentation error
            length = len(features1)-1
            if abs(features1[length-1]) > segMarginal or abs(features2[length-1]) > segMarginal:
                #print 'Segmentation error', segStart, segEnd
                sc.writeline("NoHit")
                continue
            # ====================================
            # Prepare feature vector
            if zeroCross < ThFire:
                strFeature = getFeatureVector(features1, features2)
                #rawData = resetBuffer(ch,segEnd,rawData)
                '''
                plt.close('all')
                x = np.linspace(0, len(smoData[0]), num=len(smoData[0]))
                f2, ax2 = plt.subplots(ch, sharex=True, sharey=True, figsize=(6, 10))
                for i in range(ch):
                    ax2[i].plot(x, smoData[i])
                plt.title('Smoothed')
                ax2[0].axvspan(segStart, segEnd, facecolor='g', alpha=0.25)
                ax2[1].axvspan(segStart, segEnd, facecolor='g', alpha=0.25)
                plt.show()
                '''
        rawData = resetBuffer(ch,segEnd,rawData)
    sc.writeline(strFeature)