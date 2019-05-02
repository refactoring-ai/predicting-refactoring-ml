cat machines.txt | while
	read MACHINE; do

	echo $MACHINE
	ssh $MACHINE 'bash -s' < clean_command.sh

done
