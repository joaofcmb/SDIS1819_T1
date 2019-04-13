#!/bin/sh

CLASS_PATH="./build/production/SDIS1819_T1"
PEER_MAIN="peer.Peer"
RMI_NAME="accesspoint"

PEER_NUM=$1
VERSION=$2

for i in `seq 1 $PEER_NUM`
do
	gnome-terminal -- java --class-path $CLASS_PATH $PEER_MAIN $VERSION $i $RMI_NAME$i 224.0.0.2 4441 224.0.0.3 442 224.0.0.4 4443
done

$SHELL
