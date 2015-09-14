#!/bin/sh

for pid in $(ps -ef | grep "DeployColluders" | awk '{print $2}'); do kill $pid; done

for pid in $(ps -ef | grep "DeployBARGossip" | awk '{print $2}'); do kill $pid; done
