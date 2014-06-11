import numpy as np
from Util import movingAvg

def segmentation(ch, rawData, smoothWinSize, segSmoWinSize, Th,slideWinSize):

    minWin = slideWinSize*0.3
    centerShift = 0#smoothWinSize

    # ====================================
    # Moving average & 1st derivative of respective channels
    # ====================================
    smoData = [None]*2
    difData = [None]*2
    for i in range(ch):
        smoData[i] = movingAvg(rawData[i], smoothWinSize)  # Moving average
        difData[i] = np.absolute(np.diff(smoData[i]))      # Absolute of the 1st derivative

    # ====================================
    # Sum of absolute of the 1st Derivative
    # ====================================
    amplify = 2
    sumOfDif = movingAvg([x+y for x,y in zip(difData[0], difData[1])], segSmoWinSize) * amplify
    dif_sumOfDif = movingAvg(np.diff(sumOfDif), segSmoWinSize)

    # ====================================
    # Segmentation
    # ====================================
    segStart = -1
    segEnd = -1
    hit = False
    for i in range(len(dif_sumOfDif)):
        if dif_sumOfDif[i-1]<Th and dif_sumOfDif[i]>=Th:
            segStart = i

            while i < segStart+slideWinSize and i < len(dif_sumOfDif)-1:
                i = i + 1
                if dif_sumOfDif[i-1] < -Th and dif_sumOfDif[i] >= -Th:
                    segEnd = i
                    if segEnd-segStart > minWin:   # Gesture length is not too short (half window size)
                        hit = True
                        break
        if hit==True:
            break

    # Need to wait until the value back to the baseline
    if hit==True:
        segStart = segStart + centerShift
        segEnd = segEnd + centerShift
        if segEnd < len(smoData)-1:
            print 'Ending segment is too short!'
            hit = False
            segStart = -1
            segEnd = -1

    return smoData,hit,segStart,segEnd
