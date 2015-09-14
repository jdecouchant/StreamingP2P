#!/usr/bin/python

import sys
import os

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *

nb_args = len(sys.argv)
list_args = sys.argv

jitteredRoundsList = [] # Contains the proportion of non jittered round per nodeId
nbWaitingRoundsList = []

for filename in list_args[1:]:
   
   f = open(filename, "r")
   
   
   line = map(int, f.readline().split(' '))
   rte = line[0]
   nbUpdatesPerRound = line[1]
   
   nbJitteredRounds = 0
   nbNonJitteredRounds = 0
   
   nbWaitingRounds = 0
   
   for line in f:
      array_line = map(int, line.split(' '))
      nodeState = array_line[0]
      nbDeletedUpdates = array_line[1]
      
      # node state: 0 -> waiting, 1 -> absent, 2 -> present

      if nodeState==0 or nodeState==2:  
         if nbNonJitteredRounds == 0:
            nbWaitingRounds+=1
            
         if nbDeletedUpdates >= int(0.92 * nbUpdatesPerRound):
            nbNonJitteredRounds+=1
         elif nodeState==2:
            nbJitteredRounds+=1
      
   if nbJitteredRounds != 0 or nbNonJitteredRounds != 0:
      percentageJitteredRounds = 100 * float(nbJitteredRounds) / float(nbNonJitteredRounds + nbJitteredRounds)
   
      jitteredRoundsList.append(percentageJitteredRounds)
      nbWaitingRoundsList.append(max(0,nbWaitingRounds - rte))
   f.close()
   
print "waiting rounds", nbWaitingRoundsList
print "jittered rounds %", jitteredRoundsList
print "average jitter", sum(jitteredRoundsList)/len(jitteredRoundsList)

x = linspace(1, len(jitteredRoundsList), len(jitteredRoundsList))
p1 = plot(x, jitteredRoundsList, 'k', linewidth=2, label="CoFree") # k for black

#plt.xticks(tf) 
xt = linspace(1, len(jitteredRoundsList), 4)
xticks(xt)

#title('my plot')
tick_params(axis='both', which='major', labelsize=18)
ylabel('Proportion of jittered rounds (%)', fontsize=18)
xlabel('Peer Id', fontsize=18)
legend(loc="lower right", prop={'size':18})

show()
#savefig('percentageJitteredRounds.pdf')

#os.system("pdfcrop percentageNonJitteredRounds.pdf percentageNonJitteredRounds.pdf")
