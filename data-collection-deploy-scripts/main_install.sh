cat machines2.txt | while
	read MACHINE; do

	echo $MACHINE
	ssh -oStrictHostKeyChecking=no $MACHINE 'bash -s' < update_commands.sh
	scp ssh-key/*  $MACHINE:/home/maniche/.ssh/
	ssh $MACHINE 'bash -s' < clone_github.sh

done
