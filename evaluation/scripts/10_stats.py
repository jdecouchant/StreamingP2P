#!/usr/bin/python

import sys
import os
import re

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *


print("Usage: ./10_stats.py dir")

# Goal: follow the bandwidth of ancient nodes when a massive join/departure occurs, 
# or in perfect conditions.
def study_dir(dir):

   nb_args = len(sys.argv)
   list_args = sys.argv

   roundList = []
   
   bdwList = []
   bdwUpdatesList = []
   bdwLogList = []
   bdwMsgList = []
   bdwProposeList = []
   bdwRequestList = []

   nb_nodes = 0
   for filename in os.listdir(dir):
      if re.search("stats", filename) == None:
         continue
   
      f = open(dir+"/"+filename, "r")
      
      nb_nodes += 1
      for line in f:
         array_line = map(int, line.split(' '))
         roundId = array_line[0]
         nodeState = array_line[1]
         bdwTotal = array_line[2]
         bdwUpdates = array_line[3]
         bdwLog = array_line[4]
         bdwMsg = array_line[5]
         bdwAudit = array_line[6]
         bdwPropose = array_line[7]
         bdwRequest = array_line[8]
      
         if not roundList.__contains__(roundId):
            if nodeState == 2:
               roundList.append(roundId)
               roundList.sort()
               bdwList.append(bdwTotal)
               bdwUpdatesList.append(bdwUpdates)
               bdwLogList.append(bdwLog)
               bdwMsgList.append(bdwMsg)
               bdwProposeList.append(bdwPropose)
               bdwRequestList.append(bdwRequest)
         else:
            if nodeState == 2:
               bdwList[roundList.index(roundId)] += bdwTotal 
               bdwUpdatesList[roundList.index(roundId)] += bdwUpdates
               bdwLogList[roundList.index(roundId)] += bdwLog
               bdwMsgList[roundList.index(roundId)] += bdwMsg
               bdwProposeList[roundList.index(roundId)] += bdwPropose
               bdwRequestList[roundList.index(roundId)] += bdwRequest
            
      f.close()
      
   for i in range(len(bdwList)):
      bdwList[i] /= float(nb_nodes)
      bdwUpdatesList[i] /= float(nb_nodes)
      bdwLogList[i] /= float(nb_nodes)
      bdwMsgList[i] /= float(nb_nodes)
      bdwProposeList[i] /= float(nb_nodes)
      bdwRequestList[i] /= float(nb_nodes)
      
      
   return (roundList, bdwList, bdwUpdatesList, bdwLogList, bdwMsgList, bdwProposeList, bdwRequestList)


(x,a,b,c,d, e, f) = study_dir(sys.argv[1])

plot(x, a, 'k', linewidth=2, label='Total')
plot(x, b, 'k--', linewidth=2, label='Updates')
plot(x, c, 'k:', linewidth=2, label='Log')
plot(x, d, 'k-.', linewidth=2, label='Messages') # k for black

print 'Avg total', sum(a)/len(a)
print 'Avg Updates', sum(b)/len(b)
print 'Avg Log', sum(c)/len(c)
print 'Avg Message', sum(d)/len(d)
print 'Avg Propose', float(sum(e))/float(len(e))
print 'Avg Request', sum(f)/len(f)

#plt.xticks(tf) 
#xt = linspace(1, len(jitteredRoundsList), 4)
#xticks(xt)

#title('my plot')
tick_params(axis='both', which='major', labelsize=18)
ylabel('Bandwidth in kbps', fontsize=18)
xlabel('Round Id', fontsize=18)
legend(loc="upper left", prop={'size':10})

show()
#savefig('2_average_bandwidth.pdf')

#os.system("pdfcrop percentageNonJitteredRounds.pdf percentageNonJitteredRounds.pdf")
