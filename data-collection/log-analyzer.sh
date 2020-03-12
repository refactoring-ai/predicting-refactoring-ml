#! /bin/bash

infoFile="logs/data-collection_INFO.txt*"
debugFile="logs/data_collection_DEBUG.txt*"
errorFile="logs/data-collection_ERROR.txt*"
terminalFile="docker-terminal.log"
outFile="logs/statistics.txt"

#General Statistics
echo "------------------------General Statistics------------------------" > $outFile
totalTime=$(grep -oh '.git in [0-9]*.[0-9]*' $infoFile | grep -Eo '[+-]?[0-9]+([.][0-9]+)?' | awk '{s+=$1} END {print s}')
totalProjects=$(egrep -oh 'Finished mining http.+ in [0-9]+.[0-9]+ minutes' $infoFile | wc -l)
echo "A total of ${totalProjects} projects were processed with a total App executiom time of ${totalTime} minutes." >> $outFile
totalRefactorings=$(grep 'refactoring- and ' $infoFile | sed -e 's/Found \(.*\) refactoring-.*/\1/' | awk '{s+=$1} END {print s}')
echo "${totalRefactorings} refactoring instances were found in total." >> $outFile
totalStableInstances=$(grep 'refactoring- and' $infoFile | sed -e 's/Found [0-9]* refactoring- and \(.*\) stable.*/\1/' | awk '{s+=$1} END {print s}')
echo "${totalStableInstances} stable instances were found in total." >> $outFile

for ((i=15;i<=50;i+=5)); do
	currentStable=$(grep "stable instances in the project with threshold: ${i}" $infoFile | sed -e 's/Found \(.*\) stable.*/\1/' | awk '{s+=$1} END {print s}')
	echo -e "\t${currentStable} stablecommit instances were found for threshold ${i}" >> $outFile
done
echo -e "\n" >> $outFile

echo "------------------------Detailed Statistics------------------------" >> $outFile
commitProcessStatistics=$(egrep -oh "Processing commit [a-zA-Z0-9]+ took [0-9]+ milliseconds." $debugFile)
commitProcessingTimes=$(echo "${commitProcessStatistics}" | egrep -oh " [0-9]+ " | sort -n)

commitCount=$(echo "${commitProcessStatistics}" | wc -l)
fastestCommitProcessingTime=$(echo "${commitProcessingTimes}" | head -1)
longestCommitProcessingTime=$(echo "${commitProcessingTimes}" | tail -1)
fastestCommitHash=$(echo "${commitProcessStatistics}" | egrep "took${fastestCommitProcessingTime}milliseconds." | sed 's/^.*commit/commit/' | sed -e 's/commit \(.*\) took.*/\1/')
longestCommitHash=$(echo "${commitProcessStatistics}" | egrep "took${longestCommitProcessingTime}milliseconds." | sed 's/^.*commit/commit/' | sed -e 's/commit \(.*\) took.*/\1/')
totalCommitProcessingTime=$(echo "${commitProcessingTimes}" | awk '{s+=$1} END {print s}')
averageCommitProcessingTime=$(($totalCommitProcessingTime / $commitCount))

echo "In total ${commitCount} commits were processed in ${totalCommitProcessingTime} milliseconds, thus the average time is ${averageCommitProcessingTime} milliseconds." >> $outFile
echo -e "The fastest commit was processed in ${fastestCommitProcessingTime} millisecond(s) with id(s):" >> $outFile
for commit in $fastestCommitHash; do
	echo -e "\t ${commit}" >> $outFile
done
echo -e "The slowest commit was processed in ${longestCommitProcessingTime} milliseconds with id(s):" >> $outFile
for commit in $longestCommitHash; do
	echo -e "\t ${commit}" >> $outFile
done
echo -e "\n" >> $outFile

#Exceptions
echo "------------------------Exceptions------------------------" >> $outFile
exceptionCount=$(egrep -oh '^([a-zA-Z]+.)+Exception:' $errorFile |wc -l)
uniqueExceptions=$(egrep -oh '^([a-zA-Z]+.)+Exception:' $errorFile | sort --unique)
uniqueExceptionsCount=$(echo "${uniqueExceptions}" | wc -l)
echo "${exceptionCount} exceptions occured during runtime, ${uniqueExceptionsCount} of these exceptions were unique" >> $outFile

for e in $uniqueExceptions; do
	currentExceptionCount=$(grep "${e}" $errorFile | sed 's/: .*//' | grep 'Exception' | wc -l)
	echo -e "\t${currentExceptionCount} ${e} occured during runtime." >> $outFile
done
echo -e "\n" >> $outFile

#Errors
echo "------------------------Errors------------------------" >> $outFile
oomeCount=$(egrep -oh 'java.lang.OutOfMemoryError:' $terminalFile |wc -l )
errorCount=$(($(egrep -oh '^[0-9-]+ [0-9:]+ ERROR [a-zA-Z]+:[0-9]+' $errorFile |wc -l) + $oomeCount))
uniqueOOME=$(egrep -oh 'java.lang.OutOfMemoryError:' $terminalFile | sort --unique)
uniqueErrorsDebug=$(egrep '^[0-9-]+ [0-9:]+ ERROR [a-zA-Z]+:[0-9]+' $errorFile | egrep -oh '[a-zA-Z]+:[0-9]+' | sort --unique)
uniqueErrors="${uniqueOOME} ${uniqueErrorsDebug}"
uniqueOOMECount=$(echo "${uniqueOOME}" | sort --unique | wc -l)
uniqueErrorsCount=$(($(echo "${uniqueErrors}" | sort --unique | wc -l) + $uniqueOOMECount))
echo "${errorCount} errors occured during runtime, ${uniqueErrorsCount} of these errors were at unique places" >> $outFile

for e in $uniqueErrors; do
	currentErrorCount=$(grep "${e}" $errorFile $terminalFile | wc -l)
	echo -e "\t${currentErrorCount} Errors at ${e} occured during runtime." >> $outFile
done
echo -e "\n" >> $outFile

#Individual project results
echo "------------------------Individual Project Results------------------------" >> $outFile
egrep -A 10 'Finished mining http.+ in [0-9]+.[0-9]+ minutes' $infoFile >> $outFile
echo -e "\n" >> $outFile
