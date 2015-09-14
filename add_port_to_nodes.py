#!/usr/bin/python

fin = open('nodes.txt', 'r')
fout = open('nodes_and_ports.txt', 'w')

for line in fin:
    line = line.strip('\n')
    for port in range(2000,2200,50):
        fout.write(line + '\n');
        fout.write(str(port) + '\n')

fin.close()
fout.close()
