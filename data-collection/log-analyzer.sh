#! /bin/bash
infoFile="logs/data-collection_[a-zA-Z0-9_-]*_INFO.log*"
debugFile="logs/data-collection_[a-zA-Z0-9_-]*_DEBUG.log*"
errorFile="logs/data-collection_[a-zA-Z0-9_-]*_ERROR.log*"
terminalFile="logs/docker-terminal.log"
outFile="logs/run_statistics.md"
outFileProjects="logs/project_statistics.md"

#Individual project results
echo "## Individual Project Results" > $outFileProjects
egrep -A 15 'Finished mining http.+ in [0-9]+.[0-9]+ minutes' $infoFile >> $outFileProjects


#General Statistics
echo "## General Statistics" > $outFile
totalTime=$(egrep -oh ' in [0-9]*.[0-9]*' $infoFile | egrep -o '[+-]?[0-9]+([.][0-9]+)?' | awk '{s+=$1} END {print s}')
totalProjects=$(egrep -oh 'Finished mining http.+ in [0-9]+.[0-9]+ minutes' $infoFile | wc -l)
echo "A total of **${totalProjects} projects** were processed with a total App execution time of **${totalTime} minutes**.  " >> $outFile
totalRefactorings=$(egrep 'refactoring- and ' $infoFile | sed -e 's/.*Found \(.*\) refactoring-.*/\1/' | awk '{s+=$1} END {print s}')
echo "**${totalRefactorings} refactoring** instances were found in total.  " >> $outFile
totalStableInstances=$(egrep 'refactoring- and' $infoFile | sed -e 's/.*Found [0-9]* refactoring- and \(.*\) stable.*/\1/' | awk '{s+=$1} END {print s}')
echo "**${totalStableInstances} stable** instances were found in total." >> $outFile

for ((i=15;i<=50;i+=5)); do
	currentStable=$(egrep "stable instances in the project with threshold: ${i}" $infoFile | sed -e 's/.*Found \(.*\) stable.*/\1/' | awk '{s+=$1} END {print s}')
	echo -e " 1. ${currentStable} stablecommit instances were found for threshold ${i}" >> $outFile
done
for ((i=60;i<=100;i+=10)); do
        currentStable=$(egrep "stable instances in the project with threshold: ${i}" $infoFile | sed -e 's/.*Found \(.*\) stable.*/\1/' | awk '{s+=$1} END {print s}')
        echo -e " 1. ${currentStable} stablecommit instances were found for threshold ${i}" >> $outFile
done

#Exceptions
echo "## Exceptions" >> $outFile
exceptionCount=$(egrep -oh '^([a-zA-Z]+.)+Exception:' $errorFile |wc -l)
uniqueExceptions=$(egrep -oh '^([a-zA-Z]+.)+Exception:' $errorFile | sort --unique)
uniqueExceptionsCount=$(echo "${uniqueExceptions}" | wc -l)
echo -e "**${exceptionCount} exceptions** occurred during runtime, **${uniqueExceptionsCount}** of these exceptions were **unique**.  " >> $outFile

for e in $uniqueExceptions; do
	currentExceptionCount=$(egrep "${e}" $errorFile | sed 's/: .*//' | egrep 'Exception' | wc -l)
	echo -e " 1. **${currentExceptionCount} ${e}** occurred during runtime." >> $outFile
done

#Errors
echo "## Errors" >> $outFile
oomeCount=$(egrep -oh 'java.lang.OutOfMemoryError:' $terminalFile | wc -l )
errorCount=$(($(egrep -oh '[a-zA-Z0-9_-]+ [0-9-]+ [0-9:]+ ERROR [a-zA-Z]+:[0-9]+' $errorFile | wc -l) + $oomeCount))
uniqueOOME=$(egrep -oh 'java.lang.OutOfMemoryError:' $terminalFile | sort --unique)
uniqueErrorsDebug=$(egrep '[a-zA-Z0-9_-]+ [0-9-]+ [0-9:]+ ERROR [a-zA-Z]+:[0-9]+' $errorFile | egrep -oh '[a-zA-Z]+:[0-9]+' | sort --unique)
uniqueErrors="${uniqueOOME} ${uniqueErrorsDebug}"
uniqueOOMECount=$(echo "${uniqueOOME}" | sort --unique | wc -l)
uniqueErrorsCount=$(($(echo "${uniqueErrors}" | sort --unique | wc -l) + $uniqueOOMECount))
echo "**${errorCount} errors** occurred during runtime, **${uniqueErrorsCount}** of these errors were at **unique** places.  " >> $outFile

for e in $uniqueErrors; do
	currentErrorCount=$(egrep "${e}" $errorFile $terminalFile | wc -l)
	echo -e " 1. **${currentErrorCount}** errors at **${e}** occurred during runtime." >> $outFile
done

#Project statistics
echo "## Detailed Statistics from Project Statistics" >> $outFile
commitCount=$(egrep -oh "commits=[0-9]+" $outFileProjects | sed -e 's/^commits=\([0-9]*\).*$/\1/' | awk '{s+=$1} END {print s}')
averageCommitProcessingTime=$(echo "scale=5; ${totalTime} / ${commitCount} * 1000 * 60" | bc -l)
productionFilesCount=$(egrep -oh "numberOfProductionFiles=[0-9]+" $outFileProjects | sed -e 's/^numberOfProductionFiles=\([0-9]*\).*$/\1/' | awk '{s+=$1} END {print s}')
testFilesCount=$(egrep -oh "numberOfTestFiles=[0-9]+" $outFileProjects | sed -e 's/^numberOfTestFiles=\([0-9]*\).*$/\1/' | awk '{s+=$1} END {print s}')
javaLoc=$(egrep -oh "javaLoc=[0-9]+" $outFileProjects | sed -e 's/^javaLoc=\([0-9]*\).*$/\1/' | awk '{s+=$1} END {print s}')
echo "In **total ${commitCount} commits** were processed with an average time of ${averageCommitProcessingTime} milliseconds. (lossy conversion)" >> $outFile
echo -e "A total of **${productionFilesCount} production files** and a total of **${testFilesCount} test files** with a total of **${javaLoc} LOC** were processed." >> $outFile


#Debug logs
echo "## Detailed Statistics from Debug Logs" >> $outFile
commitProcessStatistics=$(egrep -oh "Processing commit [a-zA-Z0-9]+ took [0-9]+ milliseconds." $debugFile)
commitProcessingTimes=$(echo "${commitProcessStatistics}" | egrep -oh " [0-9]+ " | sort -n)
commitCount=$(echo "${commitProcessStatistics}" | wc -l)
fastestCommitProcessingTime=$(echo "${commitProcessingTimes}" | head -1)
longestCommitProcessingTime=$(echo "${commitProcessingTimes}" | tail -1)
fastestCommitHash=$(echo "${commitProcessStatistics}" | egrep "took${fastestCommitProcessingTime}milliseconds." | sed 's/^.*commit/commit/' | sed -e 's/commit \(.*\) took.*/\1/')
longestCommitHash=$(echo "${commitProcessStatistics}" | egrep "took${longestCommitProcessingTime}milliseconds." | sed 's/^.*commit/commit/' | sed -e 's/commit \(.*\) took.*/\1/')
totalCommitProcessingTime=$(echo "${commitProcessingTimes}" | awk '{s+=$1} END {print s}')
averageCommitProcessingTime=$(($totalCommitProcessingTime / $commitCount))

echo "In **total ${commitCount} commits** were processed in **${totalCommitProcessingTime} milliseconds**, thus the average time is ${averageCommitProcessingTime} milliseconds.  " >> $outFile
echo -e "The **fastest commit** was processed in **${fastestCommitProcessingTime} millisecond(s)** with id(s):  " >> $outFile
for commit in $fastestCommitHash; do
	echo -e " 1. ${commit}  " >> $outFile
done
echo -e "The **slowest commit** was processed in **${longestCommitProcessingTime} milliseconds** with id(s):  " >> $outFile
for commit in $longestCommitHash; do
	echo -e " * ${commit}" >> $outFile
done
