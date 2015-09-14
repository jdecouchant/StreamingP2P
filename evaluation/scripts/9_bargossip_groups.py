#!/usr/bin/python
import sys
import os
import re

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *

import tkFont

x1 = [1, 4, 7, 10, 13]
x2 = [2, 5, 8, 11, 14]

xlabels = [2, 4, 5, 10, 20]

correct = [4.987, 4.975, 5.103, 4.963, 5.297]
colluders = [0.256, 0.172, 0.328, 0.288, 0.247]

bar(x1, correct, color='k', label="Correct nodes")
bar(x2, colluders, color='LightGrey', label="Colluding nodes")

#p1 = plot(x, correct, 'k-o', linewidth=2, label="Correct nodes") # k for black
#p2 = plot(x, colluders, 'k:^', linewidth=2, label="Colluding nodes") # k for black

#vlines(300, 0, 2000, color='k', linestyles='dashed')
#plt.xticks(tf) 
#yt = [0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100]
#yticks(yt)

xticks(x2, xlabels)
yticks([0,1,2,3,4,5,6])

#vlines(300, 0, 2000, color='k', linestyles='dashed')

#title('my plot')
tick_params(axis='both', which='major', labelsize=22)
ylabel('Jitter (%)', fontsize=22)
xlabel('Size of colluding groups', fontsize=22)
l = legend(loc="upper left", prop={'size':22})
ylim(ymax=7, ymin=0)
#xlim(xmax=600, xmin=0)

l.draw_frame(False)

font = {'family' : 'serif', 'size'   : 20}
rc('font', **font)
        
#show()
savefig('delivery_rate_250_several_group.pdf', dpi=600)

os.system("pdfcrop delivery_rate_250_several_group.pdf delivery_rate_250_several_group.pdf")
