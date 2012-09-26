#!/bin/bash
set -e

cd "`dirname $0`" && ./runBenchmarkSuite.sh +webserver $*

