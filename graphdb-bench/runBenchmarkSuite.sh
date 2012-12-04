#!/bin/bash

#
# runBenchmarkSuite.sh
# GraphDB Benchmark Suite -- Command-Line Launcher
#
# Copyright 2012
#      The President and Fellows of Harvard College.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
# 1. Redistributions of source code must retain the above copyright
#    notice, this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in the
#    documentation and/or other materials provided with the distribution.
# 3. Neither the name of the University nor the names of its contributors
#    may be used to endorse or promote products derived from this software
#    without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
# OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
# HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
# OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
#
# Contributor(s): Peter Macko
#

set -e
PROGRAM="`basename $0`"


#
# Options
#

MAIN_CLASS="com.tinkerpop.bench.BenchmarkSuite"

OPT_MEMORY_SIZE=1G
OPT_MEMORY_SIZE_IS_DEFAULT=yes
OPT_GC_MAIN="-XX:+UseConcMarkSweepGC -XX:+UseParNewGC"

MAVEN_TARGET=exec:java
MAVEN_CMD_OPTS=
MAVEN_D_OPTS=
MAVEN_OPTS=-server
MAVEN_OUTPUT_LEVEL_OPTS=


#
# Profilers
#

JRAT=/usr/local/java/shiftone-jrat.jar
JRAT_OPTS="-Djrat.factory=org.shiftone.jrat.provider.tree.TreeMethodHandlerFactory"

PROFILER4J=/usr/local/java/profiler4j/agent.jar
PROFILER4J_OPTS=


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
	echo "  +debug:jit     Debug the usage of the JIT compiler" >&2
	echo "  +help          Print this help information" >&2
	echo "  +itc           Use initialization time compilation (requires Oracle Java RTS)" >&2
	echo "  +main:CLASS    Set a custom main class" >&2
	echo "  +memory:SIZE   Set the Java memory (heap) size" >&2
	echo "  +ocsf          Run the optimal cache settings finder" >&2
	echo "  +profile:jrat  Profile the benchmark using JRat" >&2
	echo "  +profile:p4j   Profile the benchmark using Profiler4j" >&2
	echo "  +quiet         Run Maven quietly - show only errors" >&2
	echo "  +webserver     Run the web interface server instead of the benchmark" >&2
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
	# Option: Debug the usage of the JIT compiler
	#

	if [ $ARG = "+debug:jit" ]; then
		# References:
		#   http://java.dzone.com/articles/just-time-compiler-jit-hotspot
		#   https://gist.github.com/1165804#file_notes.md
		MAVEN_OPTS="$MAVEN_OPTS -XX:+PrintCompilation"
		#MAVEN_OPTS="$MAVEN_OPTS -XX:+PrintInlining"
		continue
	fi
	
	
	#
	# Option: Debug the usage of the JIT compiler -- with more details (Oracle JVM only)
	#

	if [ $ARG = "+debug:jitjit" ]; then
		MAVEN_OPTS="$MAVEN_OPTS -XX:+LogCompilation -XX:+UnlockDiagnosticVMOptions"
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
	# Option: initialization time compilation 
	#

	if [ $ARG = "+itc" ]; then
		MAVEN_OPTS="$MAVEN_OPTS -XX:+UseITC -XX:+ITCJLT"
		continue
	fi
	
	
	#
	# Option: custom main class
	#

	if [ ${ARG:0:6} = "+main:" ]; then
		MAIN_CLASS=${ARG:6}
		continue
	fi
	
	
	#
	# Option: memory size
	#

	if [ ${ARG:0:8} = "+memory:" ]; then
		OPT_MEMORY_SIZE=${ARG:8}
		OPT_MEMORY_SIZE_IS_DEFAULT=no
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
	
	
	#
	# Option: profile the benchmark suite using Profiler4j
	#
	
	if [ $ARG = "+profile:p4j" ]; then
		if [ ! -f $PROFILER4J ]; then
			echo "Profiler4j not found: Please download it from http://profiler4j.sourceforge.net/"
			echo "and copy the .jar file to $PROFILER4J"
			exit 1
		fi
		MAVEN_OPTS="-javaagent:$PROFILER4J $PROFILER4J_OPTS $MAVEN_OPTS"
		continue
	fi
	
	
	#
	# Option: run quietly - show only errors
	#

	if [ $ARG = "+quiet" ]; then
		MAVEN_OUTPUT_LEVEL_OPTS=-q
		continue
	fi
	
	
	#
	# Option: the web interface server
	#

	if [ $ARG = "+webserver" ]; then
		MAVEN_TARGET=jetty:run
		if [ x$OPT_MEMORY_SIZE_IS_DEFAULT = xyes ]; then
			OPT_MEMORY_SIZE=256M
		fi
		continue
	fi
	

	#
	# Invalid option
	#

	echo "$PROGRAM: Invalid option \"$ARG\" (use +help for a list of valid options)" >&2
	exit 1
done


#
# Run the benchmark suite
#

MAVEN_OPTS="$MAVEN_OPTS -Xms$OPT_MEMORY_SIZE -Xmx$OPT_MEMORY_SIZE"
MAVEN_OPTS="$MAVEN_OPTS $OPT_GC_MAIN"

if [ "x$MAVEN_TARGET" == "xexec:java" ]; then
	MAVEN_D_OPTS="$MAVEN_D_OPTS -Dexec.mainClass=$MAIN_CLASS"
fi

MAVEN_OPTS="$MAVEN_OPTS" \
	mvn $MAVEN_OUTPUT_LEVEL_OPTS $MAVEN_CMD_OPTS $MAVEN_TARGET $MAVEN_D_OPTS \
		-Dexec.args="$*"

