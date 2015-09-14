#!/usr/bin/python
import sys
import os
import re

import scipy.stats as stats
from matplotlib.pyplot import *
from numpy import *

import tkFont

fig, ax = subplots()
fig.subplots_adjust(bottom=0.2)

#ax.set_xticklabels(['100', '500', '3.000', '22.100', '162.800'])

x = [0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50]

jitter = [0, 0, 1, 2, 3, 4, 5, 8, 9, 10, 11]

p1 = plot(x, jitter, 'k-o', linewidth=2, label="Bandwidth") # k for black

#vlines(300, 0, 2000, color='k', linestyles='dashed')
#plt.xticks(tf) 
#yt = [0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100]
#yticks(yt)

#xticks([2, 3, 4, 5, 6], rotation=30)
#yticks([0,1,2,3,4,5,6])

#vlines(300, 0, 2000, color='k', linestyles='dashed')

#title('my plot')
tick_params(axis='both', which='major', labelsize=20)
ylabel('Jitter (%)', fontsize=20)
xlabel('% Churn per epoch', fontsize=20)
#legend(loc="upper left", prop={'size':18})
#ylim(ymax=7, ymin=0)
#xlim(xmax=600, xmin=0)
text(3000, 600, 'CoFree', fontsize=20)
#LogScale(ax)

font = {'family' : 'serif', 'size'   : 20}
rc('font', **font)
        
show()
#savefig('scalability.pdf', dpi=600)
#os.system("pdfcrop scalability.pdf scalability.pdf")
