#!/usr/bin/python
import sys
import os
import re

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *

# Goal: follow the average bandwidth of all nodes over time

print "Usage: ./4_cdf_bandwidth.py nodeIdMin nodeIdMax dir1 ... dirN"

def x_cdf(nodeIdMin, nodeIdMax, dir):
   
   avg_list = []
   nb_nodes = 0
   for filename in os.listdir(dir):
      if re.search("downloadBandwidth", filename) == None:
         continue
      
      avg_bdw = 0
      f = open(dir+"/"+filename, "r")
      
      array_line = map(int, f.readline().split(' '))
      nodeId = int(array_line[0])
      
      if nodeIdMin <= nodeId and nodeId <= nodeIdMax:
         nb_nodes += 1
         nb_round = 0
         for line in f:
            array_line = map(int, line.split(' '))
            roundId = array_line[0]
            nodeState = array_line[1]
            bdwTotal = array_line[2]
            bdwUpdates = array_line[3]
            bdwLog = array_line[4]
            
            nb_round += 1
            avg_bdw += bdwTotal 
            
         avg_list.append(avg_bdw / nb_round)         
      f.close()


   precision = 0.1
   max_bdw = 1400.0
   min_bdw = 0.0

   res = [0] * int(((max_bdw - min_bdw)/precision))

   for avg in avg_list:
      index = 0
      value = min_bdw
      while avg > value and index < ((max_bdw - min_bdw)/precision):
         res[index] += 1
         value += precision
         index += 1
         
   for i in range(len(res)):
      res[i] = 100 - (res[i] * 100)/nb_nodes

   x = [0] * int(((max_bdw - min_bdw)/precision))
   value = min_bdw
   for index in range(len(res)):
      x[index] = value
      value += precision
   
   return (x, res)
   
# Main code
nodeIdMin = int(sys.argv[1])
nodeIdMax = int(sys.argv[2])
xy_list = []
for dir in sys.argv[3:]:
   (x,y) = x_cdf(nodeIdMin, nodeIdMax, dir)
   xy_list.append((x,y))
   
for xy in xy_list:
   plot(xy[0], xy[1], 'k', linewidth=2, label="cdf of bandwidth") # k for black
   
#p2 = plot(roundList, bdwUpdatesList, 'k--', linewidth=2, label="Updates part")
#p3 = plot(roundList, bdwLogList, 'k:', linewidth=2, label="Log part")

#plt.xticks(tf) 
#xt = linspace(1, len(jitteredRoundsList), 4)
#xticks(xt)

#title('my plot')
tick_params(axis='both', which='major', labelsize=18)
ylabel('% of nodes', fontsize=18)
xlabel('Bandwidth in kbps', fontsize=18)
legend(loc="upper left", prop={'size':10})

show()
#savefig('2_average_bandwidth.pdf')

#os.system("pdfcrop percentageNonJitteredRounds.pdf percentageNonJitteredRounds.pdf")
