#!/usr/bin/python

import sys

fin = open('nodes.txt', 'r')
fout = open('nodes_and_ports.txt', 'w')

nb_args = len(sys.argv)
args = sys.argv

nb_clients = int(sys.argv[1])

for line in fin:
    line = line.strip('\n')
    port = 2000
    for i in range(0,nb_clients+1):
       fout.write(line + '\n')
       fout.write(str(port) + '\n')
       port += 1
    
fin.close()
fout.close()
