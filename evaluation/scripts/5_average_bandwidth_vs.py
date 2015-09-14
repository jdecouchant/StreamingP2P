#!/usr/bin/python

import sys
import os
import re

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *

if len(sys.argv) == 1:
   print("Usage: ./2_average_bandwidth.py bargossip_dir cofree_dir")
   sys.exit()

# Goal: follow the bandwidth of ancient nodes when a massive join/departure occurs, 
# or in perfect conditions.
def study_dir(dir):

   nb_args = len(sys.argv)
   list_args = sys.argv

   roundList = []
   nbNodesList = []
   bdwList = []
   peakBdwList = []
   bdwUpdatesList = []
   bdwLogList = []

   nbNodes = 0
   for filename in os.listdir(dir):
      if re.search("downloadBandwidth", filename) == None:
         continue
   
      f = open(dir+"/"+filename, "r")
      
      array_line = map(int, f.readline().split(' '))
      nodeId = int(array_line[0])

      nbNodes += 1
      for line in f:
         array_line = map(int, line.split(' '))
         roundId = array_line[0]
         nodeState = array_line[1]
         bdwTotal = array_line[2]
         bdwUpdates = array_line[3]
         bdwLog = array_line[4]
      
         if not roundList.__contains__(roundId):
            if nodeState == 2 or nodeState == 0:
               roundList.append(roundId)
               roundList.sort()
               bdwList.append(bdwTotal)
               peakBdwList.append(bdwTotal)
               bdwUpdatesList.append(bdwUpdates)
               bdwLogList.append(bdwLog)
         else:
            bdwList[roundList.index(roundId)] += bdwTotal 
            peakBdwList[roundList.index(roundId)] = max(peakBdwList[roundList.index(roundId)], bdwTotal)
            bdwUpdatesList[roundList.index(roundId)] += bdwUpdates
            bdwLogList[roundList.index(roundId)] += bdwLog
            
      f.close()
      
   for i in range(len(bdwList)):
      bdwList[i] /= nbNodes
      bdwUpdatesList[i] /= nbNodes
      bdwLogList[i] /= nbNodes
      
   return (roundList, bdwList, bdwUpdatesList, bdwLogList, peakBdwList)

(x0,a0,b0,c0,d0) = study_dir(sys.argv[1])
(x1,a1,b1,c1,d1) = study_dir(sys.argv[2])
      
p1 = plot(x0, a0, 'k-', linewidth=2, label="Avg BAR Gossip ") # k for black
#p2 = plot(x0, d0, 'k--', linewidth=2, label="Peak BAR Gossip")

p3 = plot(x1, a1, 'k:', linewidth=2, label="Avg CoFree")
#p4 = plot(x1, d1, 'k-.', linewidth=2, label="Peak CoFree")

#plt.xticks(tf) 
#xt = linspace(1, len(jitteredRoundsList), 4)
#xticks(xt)

#title('my plot')
tick_params(axis='both', which='major', labelsize=18)
ylabel('Bandwidth in kbps', fontsize=18)
xlabel('Time in sec.', fontsize=18)
legend(loc="lower right", prop={'size':10})
ylim(ymax=700, ymin=0)
xlim(xmax=1000, xmin=200)

show()
#savefig('2_average_bandwidth.pdf')

#os.system("pdfcrop percentageNonJitteredRounds.pdf percentageNonJitteredRounds.pdf")
