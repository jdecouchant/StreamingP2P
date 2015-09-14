#!/usr/bin/python

import sys
import os
import re

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *


print("Usage: ./2_average_bandwidth.py [nodeIdMin nodeIdMax dir] figure_name")

# Goal: follow the bandwidth of ancient nodes when a massive join/departure occurs, 
# or in perfect conditions.
def study_dir(nodeIdMin, nodeIdMax, dir):

   nb_args = len(sys.argv)
   list_args = sys.argv

   roundList = []
   nbNodesList = []
   bdwList = []
   peakBdwList = []
   bdwUpdatesList = []
   bdwLogList = []

   nb_nodes = 0
   for filename in os.listdir(dir):
      if re.search("uploadBandwidth", filename) == None:
         continue
   
      f = open(dir+"/"+filename, "r")
      
      array_line = map(int, f.readline().split(' '))
      nodeId = int(array_line[0])
      
      if nodeIdMin <= nodeId and nodeId <= nodeIdMax:
         nb_nodes += 1
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
      bdwList[i] /= nb_nodes
      bdwUpdatesList[i] /= nb_nodes
      bdwLogList[i] /= nb_nodes
      
   return (roundList, bdwList, bdwUpdatesList, bdwLogList, peakBdwList)

nb_dir = (len(sys.argv)-2)/4
id_dir = 1
res_list = []
names = []
for i in range(nb_dir):
   nodeIdMin = int(sys.argv[id_dir])
   nodeIdMax = int(sys.argv[id_dir+1])
   (x,a,b,c,d) = study_dir(nodeIdMin, nodeIdMax, sys.argv[id_dir+2])
   res_list.append((x,a,b,c,d))
   names.append(sys.argv[id_dir+3])
   id_dir += 4
   
figure_name = "./figures/"+sys.argv[len(sys.argv)-1]
     
for i in range(len(res_list)):
   if i==0:
      plot(res_list[i][0], res_list[i][1], 'k', linewidth=2, label=names[i])
   elif i==1:
      plot(res_list[i][0], res_list[i][1], 'k--', linewidth=2, label=names[i])
   elif i==2:
      plot(res_list[i][0], res_list[i][1], 'k:', linewidth=2, label=names[i])
   else:
      plot(res_list[i][0], res_list[i][1], 'k-.', linewidth=2, label=sys.argv[id_dir+3]) # k for black

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
