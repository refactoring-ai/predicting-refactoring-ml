#! /bin/bash

infoFile="logs/data-collection_INFO.log"
debugFile="logs/data-collection_DEBUG.log"
errorFile="logs/data-collection_ERROR.log"
outFile="logs/statistics.txt"

#General Statistics
echo "------------------------General Statistics------------------------" > $outFile
totalTime=$(grep -o '.git in [0-9]*.[0-9]*' $infoFile | grep -Eo '[+-]?[0-9]+([.][0-9]+)?' | awk '{s+=$1} END {print s}')
totalProjects=$(grep 'refactoring- and ' $infoFile | wc -l)
echo "A total of ${totalProjects} projects were processed with a total App executiom time of ${totalTime} minutes." >> $outFile
totalRefactorings=$(grep 'refactoring- and ' $infoFile | sed -e 's/Found \(.*\) refactoring-.*/\1/' | awk '{s+=$1} END {print s}')
echo "${totalRefactorings} refactoring instances were found in total." >> $outFile
totalStableInstances=$(grep 'refactoring- and' $infoFile | sed -e 's/Found [0-9]* refactoring- and \(.*\) stable.*/\1/' | awk '{s+=$1} END {print s}')
echo "${totalStableInstances} stable instances were found in total." >> $outFile

for ((i=20;i<=50;i+=10)); do
	currentStable=$(grep "stable instances in the project with threshold: ${i}" $infoFile | sed -e 's/Found \(.*\) stable.*/\1/' | awk '{s+=$1} END {print s}')
	echo -e "\t${currentStable} stablecommit instances were found for threshold ${i}" >> $outFile
done
echo -e "\n" >> $outFile

echo "------------------------Detailed Statistics------------------------" >> $outFile
commitCount=$(grep "Processing commit " $debugFile | wc -l)
fastestCommitProcessingTime=$(grep "Processing commit " $debugFile | sed 's/^.*took/took/' | sed -e 's/took \(.*\) milliseconds.*/\1/' | sort -n | head -1)
longestCommitProcessingTime=$(grep "Processing commit " $debugFile | sed 's/^.*took/took/' | sed -e 's/took \(.*\) milliseconds.*/\1/' | sort -n | tail -1)
fastestCommitHash=$(grep "took ${fastestCommitProcessingTime} milliseconds." $debugFile | sed 's/^.*commit/commit/' | sed -e 's/commit \(.*\) took.*/\1/')
longestCommitHash=$(grep "took ${longestCommitProcessingTime} milliseconds." $debugFile | sed 's/^.*commit/commit/' | sed -e 's/commit \(.*\) took.*/\1/')
totalCommitProcessingTime=$(grep "Processing commit " $debugFile | sed 's/^.*took/took/' | sed -e 's/took \(.*\) milliseconds.*/\1/' | awk '{s+=$1} END {print s}')
averageCommitProcessingTime=$(($totalCommitProcessingTime / $commitCount))

echo "In total ${commitCount} commits were processed in ${totalCommitProcessingTime} milliseconds, thus the average time is ${averageCommitProcessingTime} milliseconds." >> $outFile
echo "The fastest commit was processed in ${fastestCommitProcessingTime} millisecond(s) with id: ${fastestCommitHash}" >> $outFile
echo "The slowest commit was processed in ${longestCommitProcessingTime} milliseconds with id: ${longestCommitHash}" >> $outFile
echo -e "\n" >> $outFile

#Exceptions
echo "------------------------Exceptions------------------------" >> $outFile
exceptionCount=$(grep 'Exception:' $errorFile |wc -l)
uniqueExceptions=$(grep 'Exception:' $errorFile | sed 's/: .*//' | sort --unique)
uniqueExceptionsCount=$(grep 'Exception:' $errorFile | sed 's/: .*//' | sort --unique | wc -l)
echo "${exceptionCount} exceptions occured during runtime, ${uniqueExceptionsCount} of these exceptions were unique" >> $outFile

for e in $uniqueExceptions; do
	currentExceptionCount=$(grep "${e}" $errorFile | wc -l)
	echo -e "\t${currentExceptionCount} ${e}s occured during runtime." >> $outFile
done
echo -e "\n" >> $outFile

#Individual project results
echo "------------------------Individual Project Results------------------------" >> $outFile
grep -A 5 'refactoring- and ' $infoFile >> $outFile
echo -e "\n" >> $outFile
