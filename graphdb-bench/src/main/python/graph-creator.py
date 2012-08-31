#!/usr/bin/python
#
# Generate Artificial Graphs
# 
# Authors:
#   Peter Macko (pmacko@eecs.harvard.edu)
#   Alex Averbuch (alex.averbuch@gmail.com)
#

from igraph import *
from pyjavaproperties import Properties
from random import *

import getopt
import os
import string
import sys


#
# Function: print the program usage
#

def usage():
    sys.stderr.write("Usage: " + os.path.basename(__file__) + " [options]\n\n")
    sys.stderr.write("Options:\n")
    sys.stderr.write("  -h, --help            Print this help message and exit\n")
    sys.stderr.write("  -M, --model MODEL     Set the graph generation model\n")
    sys.stderr.write("  -o, --output FILE     Set the output file\n")
    sys.stderr.write("\nOptions for generation model \"Barabasi\":\n")
    sys.stderr.write("  -n, --barabasi-n N    Specify the number of vertices\n")
    sys.stderr.write("  -m, --barabasi-m M    Specify the degree\n")


#
# Initialize
#

script_dir = os.path.dirname(os.path.realpath(__file__))

model = None
param_n = None
param_m = None
output_filename = None


#
# Parse the command-line arguments
#

options, remainder = getopt.gnu_getopt(sys.argv[1:], 'hM:n:m:o:',
        ['help', 'output=', 'model=', 'barabasi-n=', 'barabasi-m='])

for opt, arg in options:
    if opt in ('-h', '--help'):
        usage()
        sys.exit(0)
    elif opt in ('-o', '--output'):
        output_filename = arg
    elif opt in ('-M', '--model'):
        model = arg
    elif opt in ('-m', '--barabasi-m'):
        param_m = int(arg)
    elif opt in ('-n', '--barabasi-n'):
        param_n = int(arg)

if len(remainder) != 0:
    sys.stderr.write("Too many arguments (please use --help for help)\n")
    sys.exit(1)

if model is None:
    sys.stderr.write("Unspecified graph generation model (please use --help for help)\n")
    sys.exit(1)

if output_filename is None:
    # output_filename = os.path.join(script_dir, '../../../' + p['bench.datasets.directory'] + '/' + p['bench.graph.barabasi.file'])
    sys.stderr.write("The output file name is not specified (please use --help for help)\n")
    sys.exit(1)

model = string.lower(model)


#
# Load the properties
#

p = Properties()
p.load(open(os.path.join(script_dir, '../resources/com/tinkerpop/bench/bench.properties')))


#
# Model "Barabasi"
#

if model == "barabasi":
    
    if param_m is None:
        param_m = int(p['bench.graph.barabasi.degree'])
    if param_n is None:
        param_n = int(p['bench.graph.barabasi.vertices'])

    sys.stderr.write("Generating a Barabasi graph with n=%d, m=%d\n" % (param_n, param_m))

    g = Graph.Barabasi(n=param_n, m=param_m, power=1, directed=False, zero_appeal=8)


#
# Unknown model
#

else:

    sys.stderr.write("Unknown graph type or generation model \"" + model + "\"")
    sys.exit(1)


#
# Add vertex and edge properties
#

sys.stderr.write("Adding vertex and edge properties\n")

for v in g.vs:
    g.vs[v.index][p['bench.graph.property.id']] = "v" + str(v.index)
for e in g.es:
    g.es[e.index][p['bench.graph.property.id']] = "e" + str(e.index)
for e in g.es:
    if random() < 0.5:
        g.es[e.index][p['bench.graph.label']] = p['bench.graph.label.friend']
    else:
        g.es[e.index][p['bench.graph.label']] = p['bench.graph.label.family']


#
# Write the graphml
#

sys.stderr.write("Writing GraphML to %s\n" % output_filename)
g.write_graphml(output_filename)

