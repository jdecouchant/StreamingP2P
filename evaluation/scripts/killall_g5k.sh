#!/bin/sh

source ../util.sh

#oarprint host > ./scripts/nodes.txt

while read line
do
echo $line
ssh -n ${LOGIN}@$line "${ROOT_DIRECTORY}/scripts/killall.sh"
done < ~/nodes.txt
