#!/bin/bash
set -e

export MAVEN_OPTS="-server -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -Xms128M -Xmx128M"
mvn jetty:run -Dexec.args="$*"

