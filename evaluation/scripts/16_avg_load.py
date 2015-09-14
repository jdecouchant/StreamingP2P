#!/usr/bin/python

import sys
import os
import re

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *

nb_args = len(sys.argv)
list_args = sys.argv


print "Usage: ./16_avg_load.py nodeIdMin nodeIdMax dir"

nodeIdMin = int(sys.argv[1])
nodeIdMax = int(sys.argv[2])
dir = sys.argv[3]

nb_nodes = 0

avg_list = []

for filename in os.listdir(dir):
   if re.search("_load", filename) == None:
      continue
   
   f = open("./"+dir+"/"+filename, "r")

   avg_node = 0
   nb_rounds = 0
   
   first = True
   correct = True
   for line in f:
      if first:
         nodeId = int(line)
         if not (nodeIdMin <= nodeId and nodeId <= nodeIdMax):
            correct = False
         first = False
      elif (correct):
         val = int(line)
         avg_node += val
         nb_rounds += 1
   
   if (correct and nb_rounds != 0):
      avg_list.append(avg_node / nb_rounds)
      
   f.close()
   
print 'Number of nodes: ', len(avg_list)
print sum(avg_list)/len(avg_list)
