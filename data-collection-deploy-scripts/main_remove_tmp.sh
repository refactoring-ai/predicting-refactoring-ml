cat machines.txt | while read MACHINE
do
	echo $MACHINE
	ssh $MACHINE 'rm -rf /tmp/15*' &
done
