cat machines.txt | while read MACHINE
do
	echo $MACHINE
	ssh $MACHINE 'bash -s' < update_github_only_pull.sh &
done
