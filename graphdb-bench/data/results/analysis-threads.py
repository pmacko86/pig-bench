#!/usr/bin/python

import csv
import os
import os.path
import re
import sys

global re_is_summary_file
re_is_summary_file = re.compile(r"[a-z]+-summary_.*threads\d+.*\.csv")

global re_extract_threads
re_extract_threads = re.compile(r".*threads(\d+)_\.*")


##############################################################################
## Operation Information                                                    ##
##############################################################################

class OperationInfo:

    def __init__(self, db, db_threads, operation, mean, stdev, min, max):
        self.db = db
        self.db_threads = int(db_threads)
        self.operation = operation
        self.mean = float(mean)
        self.stdev = float(stdev)
        self.min = float(min)
        self.max = float(max)


##############################################################################
## Main function                                                            ##
##############################################################################

def main():
    databases = os.listdir("Micro-smalldata-threads/")
    databases = remove_from_list(databases, "hollow")
    
    
    # Load the data
    
    # Produce the following data structure:
    #   database -> threads -> operation -> OperationInfo
    
    data = dict()
    threads = []
    
    for d in databases:
        data[d] = load_db_data(d)
        if len(data.keys()) == 1:
            threads = data[d].keys()
            threads.sort()
        else:
            threads_x = data[databases[0]].keys()
            threads_x.sort()
            if threads != threads_x:
                print "Error: Different sets of threads counts between %s and %s" \
                        % (databases[0], d)
                sys.exit(1)


    # Assertions on the database thread counts

    if len(threads) < 1:
        print "Error: Need to have at least one run"
        sys.exit(1)
    
    
    # Get the set of operations
    
    operations = None
    for d in databases:
        for s in threads:
            operations_x = data[d][s].keys()
            operations_x = remove_from_list(operations_x, "OperationDeleteGraph")
            operations_x = replace_in_list_startswith(operations_x, \
                              "OperationLoadGraphML", "OperationLoadOrGrow")
            operations_x = replace_in_list_startswith(operations_x, \
                              "OperationGenerateGraph", "OperationLoadOrGrow")
            operations_x.sort()
            if operations is None:
                operations = operations_x
            else:
                if operations != operations_x:
                    print "Error: The operation sets need to match"
                    print operations
                    print operations_x
                    sys.exit(1)
    operations = remove_from_list(operations, "OperationLoadOrGrow")
    operations = remove_from_list(operations, "OperationOpenGraph")
    operations = remove_from_list(operations, "OperationShutdownGraph")
    operations = remove_from_list(operations, "OperationDoGC")

    
    # Make sure the directory exists
    
    result_dir = "results-threads/"
    if not os.path.isdir(result_dir):
        os.makedirs(result_dir)
    
    
    # Write out the results - one result file per operation
    
    csv_header = ["threads"]
    csv_header.extend(databases)
    
    for operation in operations:
        
        csv_data = []
        csv_data.append(csv_header)
        
        for t in threads:
            row = [str(t)]
            for d in databases:
                info = data[d][t][operation]
                row.append("%f" % (info.mean / (1000.0 * 1000.0))) # us ---> ms
            csv_data.append(row)
            
        write_csv(os.path.join(result_dir, operation) + ".csv", csv_data, ";")
    
    
    # Write out the plot files and generate the plots
    
    print "Generating plots:"
    
    for operation in operations:
        print "  %s" % operation

        plt_name = os.path.join(result_dir, operation) + ".plt"
        f = open(plt_name, "w")
        
        f.write("set autoscale\n")
        f.write("unset log\n")
        f.write("unset label\n")
        f.write("set logscale x 2\n")
        f.write("set xtic auto\n")
        f.write("set ytic auto\n")
        f.write("set xlabel 'Number of Threads'\n")
        f.write("set ylabel 'Time (ms)'\n")
        f.write("set key top left\n")
        f.write("\n")
        f.write("set datafile separator ';'\n")
        f.write("set output '%s.eps'\n" % operation)
        f.write("set terminal postscript enhanced eps color 'Helvetica' 20 size 5,3.5 dl 3\n")
        f.write("\n")

        f.write("plot\\\n")
        lt_i = 0
        for i in xrange(0, len(databases)):
            database = databases[i]
            lt_i += 1
            if lt_i == 5: lt_i += 1
            if lt_i == 6: lt_i += 1
            f.write(("  '<sed \"1,1d\" %s.csv' using 1:%d title '%s' with lines"
                + " lt %d lw 2") % (operation, i + 2, database, lt_i))
            if i + 1 < len(databases):
                f.write(", \\\n")
            else:
                f.write("\n")
        f.close()
        
        os.system("bash -c 'cd %s && gnuplot %s.plt'" % (result_dir, operation))
        os.system("bash -c 'cd %s && epspdf %s.eps'" % (result_dir, operation))
    
    
    # Finish
    
    if not os.path.isdir(os.path.join(result_dir, "pdf")):
        os.makedirs(os.path.join(result_dir, "pdf"))
    os.system("bash -c 'cd %s && /bin/mv -f *.pdf pdf'" % result_dir)
    
    if not os.path.isdir(os.path.join(result_dir, "eps")):
        os.makedirs(os.path.join(result_dir, "eps"))
    os.system("bash -c 'cd %s && /bin/mv -f *.eps eps'" % result_dir)



##############################################################################
## Load information for a single database                                   ##
##############################################################################

def load_db_data(dbname):
    global re_is_summary_file
    global re_extract_threads


    # Find the list of summary files in both the incremental and the final run

    dir = "Micro-smalldata-threads/" + dbname + "/";
    files = os.listdir(dir)
    summaries = find_in_list(files, re_is_summary_file)
    summaries = add_prefix_to_all(summaries, dir)
    
        
    # Process each summary file
    
    # Produce the following data structure:
    #   threads -> operation -> OperationInfo
    
    data = dict()
    for summary in summaries:
        m_threads = re_extract_threads.match(summary)
        if m_threads is None:
            print "Error: Cannot find the number of threads in " \
                + "the file name \"%s\"" % (summary)
            sys.exit(1)
        else:
            threads = int(m_threads.group(1))
        
        summary_data = dict()
        data[threads] = summary_data
        
        first = True
        f = csv.reader(open(summary, 'r'))
        
        for line in f:
            operation,mean,stdev,min,max = line
            if first:
                first = False
                continue
            
            key = operation
            if key.startswith("OperationLoadGraphML"):
                key = "OperationLoadOrGrow"
            if key.startswith("OperationGenerateGraph"):
                key = "OperationLoadOrGrow"
            
            summary_data[key] = \
                OperationInfo(dbname, threads, operation, mean, stdev, min, max)
        
        f.close()

    return data


##############################################################################
## Helpers and Utilities                                                    ##
##############################################################################

def find_in_list(list, regexp):
    r = []
    for s in list:
        if regexp.match(s) is not None:
            r.append(s)
    return r

def add_prefix_to_all(list, prefix):
    r = []
    for s in list:
        r.append(prefix + s)
    return r

def remove_from_list(list, what):
    r = []
    for s in list:
        if s != what:
            r.append(s)
    return r

def replace_in_list(list, what, replacement):
    r = []
    for s in list:
        if s != what:
            r.append(s)
        else:
            r.append(replacement)
    return r

def replace_in_list_startswith(list, what, replacement):
    r = []
    for s in list:
        if not s.startswith(what):
            r.append(s)
        else:
            r.append(replacement)
    return r

def write_csv(file, data, separator=","):
    f = open(file, "w")
    for row in data:
        first = True
        for x in row:
            if first:
                first = False
            else:
                f.write(separator)
            f.write(str(x))
        f.write("\n")
    f.close()


##############################################################################
## The script startup                                                       ##
##############################################################################

if __name__ == '__main__':
    if len(sys.argv) == 1:
        main()
    else:
        print 'usage: script.py'
