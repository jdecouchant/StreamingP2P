#!/usr/bin/python
import sys
import os
import re

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *

import tkFont

x1 = [1, 4, 7, 10, 13, 16, 19, 22, 25, 28]#, 31, 34, 37]
x2 = [2, 5, 8, 11, 14, 17, 20, 23, 26, 29]#, 32, 35, 38]

xlabels = [0, 5, 10, 15, 20, 25, 30, 35, 40, 45]#, 50, 55, 60]

barg = [0.23,1.81,5.27,8.91,15.03,23.49,35.41,46.14,58.31,73.46]#,84.32,92.21,98.01]
cofree = [0.23, 0.23, 0.23, 0.23, 0.23, 0.23, 0.23, 0.23, 0.23, 0.23]#, 0, 0, 0]

bar(x1, cofree, color='k', label="CoFree")
bar(x2, barg, color='LightGrey', label="BAR Gossip")

#p1 = plot(x, bar, 'k-o', linewidth=2, label="BAR Gossip") # k for black
#p2 = plot(x, cofree, 'k:^', linewidth=2, label="CoFree") # k for black

#vlines(300, 0, 2000, color='k', linestyles='dashed')
#plt.xticks(tf) 
yt = [0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100]
yticks(yt)

xticks(x2, xlabels)
#yticks([0,1,2,3,4,5,6])

#vlines(300, 0, 2000, color='k', linestyles='dashed')

#title('my plot')
tick_params(axis='both', which='major', labelsize=20)
ylabel('Correct nodes jitter (%)', fontsize=20)
xlabel('Percentage of colluders in session', fontsize=20)
l = legend(loc="upper left", prop={'size':20})
l.draw_frame(False)
ylim(ymax=100, ymin=-1)
xlim(xmax=31, xmin=0)

font = {'family' : 'serif', 'size'   : 20}

rc('font', **font)
        
#show()

savefig('colluders_jitter_vs.pdf', dpi=600)
os.system("pdfcrop colluders_jitter_vs.pdf colluders_jitter_vs.pdf")
