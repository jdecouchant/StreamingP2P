#!/usr/bin/python
import sys
import os
import re

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *

import tkFont

x = [0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60]

correct = [0.23,1.81,5.27,8.91,15.03,23.49,35.41,46.14,58.31,73.46,84.32,92.21,98.01]
colluders = [0.23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]


p1 = plot(x, correct, 'k-o', linewidth=2, label="Correct nodes") # k for black
p2 = plot(x, colluders, 'k:^', linewidth=2, label="Colluding nodes") # k for black

#vlines(300, 0, 2000, color='k', linestyles='dashed')
#plt.xticks(tf) 
yt = [0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100]
yticks(yt)

#vlines(300, 0, 2000, color='k', linestyles='dashed')

#title('my plot')
tick_params(axis='both', which='major', labelsize=22)
ylabel('Jitter (%)', fontsize=22)
xlabel('Percentage of colluders', fontsize=22)
l = legend(loc="upper left", prop={'size':22})
l.draw_frame(False)
ylim(ymax=100, ymin=-1)
#xlim(xmax=600, xmin=0)

font = {'family' : 'serif', 'size'   : 22}

rc('font', **font)
        
#show()
#savefig('percentageJitteredRounds.pdf')

savefig('delivery_rate_250.pdf', dpi=600)
os.system("pdfcrop delivery_rate_250.pdf delivery_rate_250.pdf")
