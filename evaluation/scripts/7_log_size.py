#!/usr/bin/python
import sys
import os
import re

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *

# Goal: follow the jitter of some nodes (ideal case, massive join/departure, presence of colluders)

print "Usage: ./7_log_size.py dir"

def roundId_jitteredList(dir):
   ''' Compute the jitter of nodes between nodeIdMin and nodeIdMax per round
   Return a list of the roundId, and the associated average jitter of considered nodes. '''
   
   roundList = []
   logSizeList = [] # Contains the proportion of jittered node per round
   totalSizeList = []
   nb_nodes = 0

   for filename in os.listdir(dir):
      if re.search("logSize", filename) == None:
         continue
      
      f = open(dir+"/"+filename, "r")
      
      nb_nodes += 1
      
      roundId = 1
      for line in f:
         line = map(int, line.split('\t'))
         logSize = line[0]
         totalSize = line[1]
      
         if not roundList.__contains__(roundId):
            roundList.append(roundId)
            roundList.sort()
            logSizeList.insert(roundList.index(roundId), logSize/1000000.0)
            totalSizeList.insert(roundList.index(roundId), totalSize/1000000.0)
         else:
            logSizeList[roundList.index(roundId)] += logSize/1000000.0
            totalSizeList[roundList.index(roundId)] += totalSize/1000000.0
         roundId += 1
            
      f.close()

   #for i in range(len(jitteredRoundsList)):
      #jitteredRoundsList[i] = (jitteredRoundsList[i] * 100) / nb_nodes

   return (roundList, logSizeList, totalSizeList)
   
(x, y, z) = roundId_jitteredList(sys.argv[1])
   
p1 = plot(x, y, 'k', linewidth=2, label="Log size") # k for black
p2 = plot(x, z, 'k:', linewidth=2, label="Total size") # k for black

#vlines(300, 0, 2000, color='k', linestyles='dashed')
#plt.xticks(tf) 
#xt = linspace(1, len(jitteredRoundsList), 4)
#xticks(xt)

#title('my plot')
tick_params(axis='both', which='major', labelsize=18)
ylabel('Memory in Mb', fontsize=18)
xlabel('Time in seconds', fontsize=18)
legend(loc="lower right", prop={'size':18})
#ylim(ymax=100, ymin=0)
#xlim(xmax=600, xmin=0)

show()
#savefig('percentageJitteredRounds.pdf')

#os.system("pdfcrop percentageNonJitteredRounds.pdf percentageNonJitteredRounds.pdf")
