#!/bin/bash
set -e

PROGRAM="`basename $0`"
MAVEN_OPTS=-server

MAIN_CLASS="com.tinkerpop.bench.BenchmarkSuite"

OPT_MEMORY_SIZE=1G
OPT_GC_MAIN="-XX:+UseConcMarkSweepGC -XX:+UseParNewGC"


# JRat configuration

JRAT=/usr/local/java/shiftone-jrat.jar
JRAT_OPTS="-Djrat.factory=org.shiftone.jrat.provider.tree.TreeMethodHandlerFactory"


#
# Function: print the usage information
#

usage() {
	echo "A Benchmark Suite for GraphDBs" >&2
	echo " " >&2
	echo "Usage: $PROGRAM [SCRIPT_OPTIONS...] BENCHMARK_OPTIONS..." >&2
	echo " " >&2
	echo "Script options:" >&2
	echo "  +debug:gc      Debug the memory usage and the garbage collector" >&2
	echo "  +help          Print this help information" >&2
	echo "  +memory:SIZE   Set the Java memory (heap) size" >&2
	echo "  +ocsf          Run the optimal cache settings finder" >&2
	echo "  +profile:jrat  Profile the benchmark using JRat" >&2
}


#
# Isolate and process the arguments for this script
#

MY_ARGS=
while [ "x${1:0:1}" = "x+" ]; do
	ARG=$1
	MY_ARGS="$MY_ARGS $ARG"
	shift
	
	
	#
	# Option: debug the garbage collector and the memory usage
	#

	if [ $ARG = "+debug:gc" ]; then
		MAVEN_OPTS="$MAVEN_OPTS -XX:+HeapDumpOnOutOfMemoryError -verbose:gc"
		continue
	fi
	
	
	#
	# Option: help
	#

	if [ $ARG = "+help" ]; then
		usage
		exit
	fi
	
	
	#
	# Option: memory size
	#

	if [ ${ARG:0:8} = "+memory:" ]; then
		OPT_MEMORY_SIZE=${ARG:8}
		continue
	fi
	
	
	#
	# Option: the optimal cache settings finder
	#

	if [ $ARG = "+ocsf" ]; then
		MAIN_CLASS="com.tinkerpop.bench.benchmark.OptimalCacheSettingsFinder"
		continue
	fi
	
	
	#
	# Option: profile the benchmark suite using JRat
	#
	
	if [ $ARG = "+profile:jrat" ]; then
		if [ ! -f $JRAT ]; then
			echo "JRat not found: Please download it from http://jrat.sourceforge.net/"
			echo "and copy the .jar file to $JRAT"
			exit 1
		fi
		MAVEN_OPTS="-javaagent:$JRAT $JRAT_OPTS $MAVEN_OPTS"
		continue
	fi
	
	echo "$PROGRAM: Invalid option \"$ARG\" (use +help for a list of valid options)" >&2
	exit 1
done


#
# Run the benchmark suite
#

MAVEN_OPTS="$MAVEN_OPTS -Xms$OPT_MEMORY_SIZE -Xmx$OPT_MEMORY_SIZE"
MAVEN_OPTS="$MAVEN_OPTS $OPT_GC_MAIN"

MAVEN_OPTS="$MAVEN_OPTS" \
	mvn -e exec:java -Dexec.mainClass=$MAIN_CLASS -Dexec.args="$*"

