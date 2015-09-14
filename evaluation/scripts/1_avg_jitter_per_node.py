#!/usr/bin/python

import sys
import os
import re

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *

nb_args = len(sys.argv)
list_args = sys.argv


print "Usage: ./1_avg_jitter_per_node nodeIdMin nodeIdMax dir"


jitteredRoundsList = [] # Contains the proportion of non jittered round per nodeId
nbWaitingRoundsList = []

nodeIdMin = int(sys.argv[1])
nodeIdMax = int(sys.argv[2])
dir = sys.argv[3]

nb_nodes = 0

for filename in os.listdir(dir):
   if re.search("nbDeleted", filename) == None:
      continue

   f = open("./"+dir+"/"+filename, "r")
   
   
   line = map(int, f.readline().split(' '))
   nodeId = line[0]
   rte = line[1]
   nbUpdatesPerRound = line[2]
   
   nbJitteredRounds = 0
   nbNonJitteredRounds = 0
   
   isWaiting = False
   nbWaitingRounds = 0
   
   if (nodeIdMin <= nodeId and nodeId <= nodeIdMax):
      nb_nodes += 1
      for line in f:
         array_line = map(int, line.split(' '))
         roundId = array_line[0]
         nodeState = array_line[1]
         nbDeletedUpdates = array_line[2]
         
         # node state: 0 -> waiting, 1 -> absent, 2 -> present
         isWaiting = (nodeState==0)

         if nodeState==0 or nodeState==2:  
            if isWaiting and nbDeletedUpdates==0:
               nbWaitingRounds+=1
               
            if nbDeletedUpdates >= int(0.92 * nbUpdatesPerRound):
               nbNonJitteredRounds+=1
            elif nodeState==2 and nbNonJitteredRounds != 0:
               nbJitteredRounds+=1
         
      if nbJitteredRounds != 0 or nbNonJitteredRounds != 0:
         percentageJitteredRounds = 100 * float(nbJitteredRounds) / float(nbNonJitteredRounds + nbJitteredRounds)
      
         jitteredRoundsList.append(percentageJitteredRounds)
         nbWaitingRoundsList.append(max(0,nbWaitingRounds - rte))
   f.close()
   
print "\nWaiting rounds", nbWaitingRoundsList
print "Jittered rounds %"
for i in range(len(jitteredRoundsList)):
   if i==0:
      print "[{0:.2f},".format(jitteredRoundsList[i]),
   elif i < len(jitteredRoundsList)-1:
      print "{0:.2f},".format(jitteredRoundsList[i]),
   else:
      print "{0:.2f}]".format(jitteredRoundsList[i])

if (len(jitteredRoundsList) != 0):
   print "Average jitter\n", sum(jitteredRoundsList)/len(jitteredRoundsList), "\n"
else:
   print "Average jitter\n0.0\n"

print "Number of nodes ", nb_nodes

x = linspace(1, len(jitteredRoundsList), len(jitteredRoundsList))
p1 = plot(x, jitteredRoundsList, 'k', linewidth=2, label="CoFree") # k for black

#plt.xticks(tf) 
xt = linspace(1, nb_nodes, 4)
xticks(xt)

#title('my plot')
tick_params(axis='both', which='major', labelsize=18)
ylabel('Proportion of jittered rounds (%)', fontsize=18)
xlabel('Peer Id', fontsize=18)
legend(loc="lower right", prop={'size':18})

show()
#savefig('percentageJitteredRounds.pdf')

#os.system("pdfcrop percentageNonJitteredRounds.pdf percentageNonJitteredRounds.pdf")
