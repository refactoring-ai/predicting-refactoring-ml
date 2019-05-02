cat machines.txt | while
	read MACHINE; do

	echo $MACHINE
	ssh $MACHINE 'bash -s' < kill_command.sh

done
