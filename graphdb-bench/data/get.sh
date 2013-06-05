#!/bin/bash

#
# A script to get information out of the log files
#

if [ "x$3" == "x" -o "x$4" != "x" ]; then
	echo "Usage: get.sh FILE OPERATION WHAT"
	echo ""
	echo "Where WHAT can be:"
	echo "  [t] time"
	echo "  [u] unique"
	echo "  [k] neighborhoods"
	echo "  [n] nodes"
	exit 1
fi

FILE="$1"
OPERATION="$2"
WHAT="$3"
FIELD="x"

if [ "$WHAT" == "t" -o "$WHAT" == "time" ]; then
	FIELD="5"
elif [ "$WHAT" == "u" -o "$WHAT" == "unique" ]; then
	FIELD="6"
elif [ "$WHAT" == "k" -o "$WHAT" == "neighborhoods" ]; then
	FIELD="8"
elif [ "$WHAT" == "n" -o "$WHAT" == "nodes" ]; then
	FIELD="9"
else
	echo "Wrong WHAT argument: $WHAT"
	exit 1
fi

cat "$FILE" | grep -E ",\"?Operation$OPERATION\"?," \
		| sed -E 's/(\[[^]]*)[,:]/\1*/g' \
		| sed -E 's/(\[[^]]*)[,:]/\1*/g' \
		| sed -E 's/(\[[^]]*)[,:]/\1*/g' \
		| sed -E 's/(\[[^]]*)[,:]/\1*/g' \
		| sed -E 's/(\[[^]]*)[,:]/\1*/g' \
		| sed -E 's/(\[[^]]*)[,:]/\1*/g' \
		| tr ',:' '\t' | tr -d '"' \
		| sed 's/\*/,/g' \
		| cut -d '	' -f $FIELD

