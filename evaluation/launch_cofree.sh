#!/bin/bash

# RTE=12
# FANOUT_CLI=2
# FANOUT_SVR=5
# PERIOD=3
# EPOCH=40
# SCENARIO=6
# SESSION_DURATION=300
# 
# NBNODES=50

RTE=12
FANOUT_CLI=2
FANOUT_SVR=5
PERIOD=3
EPOCH=40
SCENARIO=0
SESSION_DURATION=300

NBNODES=50

DIR="./results-cofree/$RTE.$FANOUT_CLI.$FANOUT_SVR.$PERIOD.$EPOCH.$SCENARIO.$SESSION_DURATION.$NBNODES"
mkdir -p $DIR

rm $DIR/*

# WARNING: Must be removed for g5K
echo "localhost" > ./scripts/nodes.txt

./scripts/add_port_to_nodes.py $NBNODES ./scripts/nodes.txt ./scripts/nodes_and_ports.txt

# launch X instances of CoFree (one is the server, others are peers) 
for id in $(seq 0 $NBNODES)
do
   #xterm -e  java -jar ./jar/DeployColluders.jar $id $RTE $FANOUT_CLI $FANOUT_SVR $PERIOD $EPOCH $SCENARIO $SESSION_DURATION ./scripts/nodes_and_ports.txt &
   java -jar ./jar/DeployColluders.jar $id $RTE $FANOUT_CLI $FANOUT_SVR $PERIOD $EPOCH $SCENARIO $SESSION_DURATION ./scripts/nodes_and_ports.txt > /dev/null &
done

wait

mv *nbDeleted* $DIR/
mv *logSize* $DIR/
mv *downloadBandwidth* $DIR/
mv 0* $DIR/
