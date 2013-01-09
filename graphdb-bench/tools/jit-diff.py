#!/usr/bin/python

import os
import re
import sys


if len(sys.argv) != 3:
	sys.exit("Usage: %s FILE1 FILE2" % sys.argv[0])

files = [sys.argv[1], sys.argv[2]]


#
# Determine which methods are JIT-ed
#

data = dict()

r_jit = re.compile("\d+\s+(\d+)[% ][s ][! ][b ][n ]  ([\w\.:\$]+) \(\d+ bytes\)\n$")
r_not_entrant = re.compile("\d+\s+(\d+)[% ][s ][! ][b ][n ] made not entrant  ([\w\.:\$]+) \(\d+ bytes\)\n$")
r_zombie = re.compile("\d+\s+(\d+)[% ][s ][! ][b ][n ] made zombie  ([\w\.:\$]+) \(\d+ bytes\)\n$")

for f in files:
	fh = open(f)
	jitted = dict()
	removed = dict()
	
	for line in fh:
		if not line.endswith("bytes)\n"): continue

		m = r_jit.search(line)
		if m is not None:
			cid = m.group(1)
			name = m.group(2)
			jitted[cid] = name

		m = r_not_entrant.search(line)
		if m is not None:
			cid = m.group(1)
			name = m.group(2)
			del jitted[cid]
			removed[cid] = name
	
	fh.close()
	data[f] = [jitted, removed]


#
# Print the differences
#

jitted0 = set(data[files[0]][0].values())
jitted1 = set(data[files[1]][0].values())

only0 = jitted0.difference(jitted1)
only1 = jitted1.difference(jitted0)
common = jitted0.intersection(jitted1)

print "Common:"
for x in sorted(common):
	print x

print ""
print "Only in %s:" % files[0]
for x in sorted(only0):
	print x

print ""
print "Only in %s:" % files[1]
for x in sorted(only1):
	print x

