#!/usr/bin/python
import sys
import os
import re

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *

# Goal: follow the jitter of some nodes (ideal case, massive join/departure, presence of colluders)

print "Usage: ./3_avg_jitter_per_round.py nodeIdMin nodeIdMax dir1 ... dirN"

def roundId_jitteredList(nodeIdMin, nodeIdMax, dir):
   ''' Compute the jitter of nodes between nodeIdMin and nodeIdMax per round
   Return a list of the roundId, and the associated average jitter of considered nodes. '''
   
   roundList = []
   jitteredRoundsList = [] # Contains the proportion of jittered node per round
   nb_nodes = 0.0

   for filename in os.listdir(dir):
      if re.search("nbDeleted", filename) == None:
         continue
      
      f = open(dir+"/"+filename, "r")
         
      line = map(int, f.readline().split(' '))
      nodeId = line[0]
      rte = line[1]
      nbUpdatesPerRound = line[2]
      
      if (nodeIdMin <= nodeId and nodeId <= nodeIdMax):
         nb_nodes += 1
         nbNonJitteredRounds = 0
         for line in f:
            array_line = map(int, line.split(' '))
            roundId = array_line[0]
            nodeState = array_line[1]
            nbDeletedUpdates = array_line[2]
         
            # node state: 0 -> waiting, 1 -> absent, 2 -> present

            if nodeState==0 or nodeState==2:  
               if nbDeletedUpdates >= int(0.92 * nbUpdatesPerRound):
                  nbNonJitteredRounds+=1
         
            if (nodeState==0 or nodeState==2) and nbNonJitteredRounds != 0:
               if not roundList.__contains__(roundId):
                  roundList.append(roundId)
                  roundList.append(roundId+1-0.0001)
                  roundList.sort()
                  jitteredRoundsList.insert(roundList.index(roundId), 0.0)
                  jitteredRoundsList.insert(roundList.index(roundId), 0.0)
               if nbDeletedUpdates < int(0.92 * nbUpdatesPerRound): # round is jittered
                  jitteredRoundsList[roundList.index(roundId)] += 1.0
                  jitteredRoundsList[roundList.index(roundId+1-0.0001)] += 1.0

      f.close()

   #for i in range(len(jitteredRoundsList)):
      #jitteredRoundsList[i] = (jitteredRoundsList[i] * 100) / nb_nodes

   return (roundList, jitteredRoundsList)
   
(x, y) = ([],[])
for dir in sys.argv[3:]:
   (x, y) = roundId_jitteredList(int(sys.argv[1]), int(sys.argv[2]), dir)
   
p1 = plot(x, y, 'k', linewidth=2, label="Average jitter of nodes per round.") # k for black

vlines(300, 0, 2000, color='k', linestyles='dashed')
#plt.xticks(tf) 
#xt = linspace(1, len(jitteredRoundsList), 4)
#xticks(xt)

#title('my plot')
tick_params(axis='both', which='major', labelsize=18)
ylabel('Proportion of jittered nodes (%)', fontsize=18)
xlabel('Round Id', fontsize=18)
legend(loc="upper left", prop={'size':10})
ylim(ymax=100, ymin=0)
xlim(xmax=600, xmin=0)

show()
#savefig('percentageJitteredRounds.pdf')

#os.system("pdfcrop percentageNonJitteredRounds.pdf percentageNonJitteredRounds.pdf")
