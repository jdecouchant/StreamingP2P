#!/bin/bash

GROUP_SIZE=0
GROUP_NBR=0
RTE=10
BAL_SIZE=400
PUSH_SIZE=400
PUSH_AGE=10
FANOUT=5 # For the server only
SESSION_DURATION=150

NBNODES=50

DIR="./results-bargossip/$BAL_SIZE.$PUSH_SIZE.$PUSH_AGE.$FANOUT.$RTE.$SESSION_DURATION.$NBNODES"
mkdir -p $DIR

rm -f $DIR/*

# WARNING: Must be removed for g5K
echo "localhost" > ./scripts/nodes.txt

./scripts/add_port_to_nodes.py $NBNODES ./scripts/nodes.txt ./scripts/nodes_and_ports.txt

# launch X instances of CoFree (one is the server, others are peers) 
for id in $(seq 0 $NBNODES)
do
   #xterm -e java -jar ./jar/DeployBARGossipColluders.jar $id $GROUP_SIZE $GROUP_NBR $RTE $BAL_SIZE $PUSH_SIZE $PUSH_AGE $FANOUT $SESSION_DURATION ./scripts/nodes_and_ports.txt &
   java -jar ./jar/DeployBARGossipColluders.jar $id $GROUP_SIZE $GROUP_NBR $RTE $BAL_SIZE $PUSH_SIZE $PUSH_AGE $FANOUT $SESSION_DURATION ./scripts/nodes_and_ports.txt > /dev/null &
done

wait

mv *nbDeleted* $DIR/
mv *Bandwidth* $DIR/
