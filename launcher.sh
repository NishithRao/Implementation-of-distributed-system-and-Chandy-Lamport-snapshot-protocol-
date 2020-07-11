#!/bin/bash
#takes two parameters --> config filename and netID

# Root directory of your project
#PROJDIR=$HOME/final/ChandyLamport/
#
# This assumes your config file is named "config.txt"
# and is located in your project directory
#
#CONFIG=$PROJDIR/config.txt
#
# Directory your java classes are in
#
#BINDIR=$PROJDIR
#
# Your main project class
#
PROG=Lamport_Main
NETID=$2
PROGRAM_PATH=$(pwd)

#remove output file if any and compile the java files
rm *.class *.out /
javac *.java

cat $1 | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read i
    totalNodes=$( echo $i | awk '{ print $1 }' )
    
    for ((a=1; a <= $totalNodes ; a++))
    do
    	read line 
	nodeId=$( echo $line | awk '{ print $1 }' )
       	host=$( echo $line | awk '{ print $2 }' )
	echo $nodeId
	echo $host
	echo $NETID
	sshpass -p *password* ssh -o StrictHostKeyChecking=no -l "$NETID" "$host" "cd $PROGRAM_PATH;java $PROG $nodeId $1" &
	
    done
   
)


