#!/usr/bin/python
import sys
import os
import re

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *

# Goal: follow the average bandwidth of all nodes over time

if len(sys.argv) == 1:
   print "Goal: Give the cdf of the average bandwidth of each node during the overall session"
   print "Usage: ./4_cdf_bandwidth.py bargossip_dir bargossip_label cofree_dir cofree_label"
   sys.exit()

def x_cdf(dir):
   
   avg_list = []
   nb_nodes = 0
   for filename in os.listdir(dir):
      if re.search("downloadBandwidth", filename) == None:
         continue
      
      avg_bdw = 0
      f = open(dir+"/"+filename, "r")
      
      array_line = map(int, f.readline().split(' '))
      nodeId = int(array_line[0])
      
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
(x0,y0) = x_cdf(sys.argv[1])
(x1,y1) = x_cdf(sys.argv[3])

plot(x0, y0, 'k', linewidth=2, label=sys.argv[2]) # k for black
plot(x1, y1, 'k:', linewidth=2, label=sys.argv[4]) # k for black
   
#p2 = plot(roundList, bdwUpdatesList, 'k--', linewidth=2, label="Updates part")
#p3 = plot(roundList, bdwLogList, 'k:', linewidth=2, label="Log part")

#plt.xticks(tf) 
#xt = linspace(1, len(jitteredRoundsList), 4)
#xticks(xt)

#title('my plot')
tick_params(axis='both', which='major', labelsize=18)
ylabel('Percentage of nodes (cumulative distribution)', fontsize=18)
xlabel('Bandwidth in kbps', fontsize=18)
legend(loc="lower right", prop={'size':18})
ylim(ymax=100, ymin=0.1)
xlim(xmax=700, xmin=450)

show()
#savefig('2_average_bandwidth.pdf')

#os.system("pdfcrop percentageNonJitteredRounds.pdf percentageNonJitteredRounds.pdf")
