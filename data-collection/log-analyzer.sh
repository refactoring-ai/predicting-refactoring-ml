#!/bin/bash

outFile="statistics.txt"
thresholds=(20 30 40 50)

#General Statistics
echo "A total of " + + " projects was processed with an executiom time of " + + " of app">> $outFile
echo + " refactoring instances were found in total." >> $outFile
echo + " stable instances were found in total." >> $outFile

for i in $thresholds; do
	echo + " stablecommit instances were found for threshold ${i}" >> $outFile
done

#Exceptions
uniqueExceptions=$()
echo + " exceptions occured during runtime " + + " of these exceptions were unique" >> $outFile

for e in $uniqueExceptions; do
	echo + " ${e} occured during runtime." >> $outFile
done

#Individual project results
