#!/usr/bin/python

import csv
import os
import os.path
import re
import sys

global re_is_summary_file
re_is_summary_file = re.compile(r"[a-z]+-summary_.*\.csv")

global re_extract_size_annotation
re_extract_size_annotation = re.compile(r".*annotationSIZE(\d+)_\.*")

global re_extract_base_annotation
re_extract_base_annotation = re.compile(r".*annotationBASE(\d+)_\.*")


##############################################################################
## Operation Information                                                    ##
##############################################################################

class OperationInfo:

    def __init__(self, db, db_size, operation, mean, stdev, min, max):
        self.db = db
        self.db_size = int(db_size)
        self.operation = operation
        self.mean = float(mean)
        self.stdev = float(stdev)
        self.min = float(min)
        self.max = float(max)


##############################################################################
## Main function                                                            ##
##############################################################################

def main():
    databases = os.listdir("Micro-2k-incremental/")
    databases = remove_from_list(databases, "hollow")
    databases.sort()
    
    
    # Load the data
    
    # Produce the following data structure:
    #   database -> size -> operation -> OperationInfo
    # Where size is the incremental size, or a negative number in the case of
    # the base size.
    
    data = dict()
    sizes = []
    
    for d in databases:
        data[d] = load_db_data(d)
        if len(data.keys()) == 1:
            sizes = data[d].keys()
            sizes.sort()
        else:
            sizes_x = data[databases[0]].keys()
            sizes_x.sort()
            if sizes != sizes_x:
                print "Error: Different sets of sizes between %s and %s" \
                        % (databases[0], d)
                sys.exit(1)


    # Assertions on the database sizes

    if len(sizes) <= 1 or sizes[0] > 0:
        print "Error: Need to have at least one base and one incremental run"
        sys.exit(1)
    if sizes[1] <= 0:
        print "Error: Cannot have more than one base run"
        sys.exit(1)
    
    sizes_plus_base = list(sizes)
    base_size_element = sizes[0]
    base_size = -base_size_element
    del sizes[0]
    
    if sizes[len(sizes) - 1] != base_size:
        print "Error: The final sizes of the base and the last incremental run"\
                + " need to match"
        sys.exit(1)
    
    
    # Get the set of operations
    
    operations = None
    for d in databases:
        for s in sizes_plus_base:
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
    
    result_dir = "results-incremental/"
    if not os.path.isdir(result_dir):
        os.makedirs(result_dir)
    
    
    # Write out the results - one result file per operation
    
    csv_header = ["size"]
    csv_header.extend(databases)
    
    for operation in operations:
        
        csv_data = []
        csv_data.append(csv_header)
        
        for size in sizes_plus_base:
            if size < 0:
                s_size = "BASE"
            else:
                s_size = str(size)
            
            row = [s_size]
            for d in databases:
                info = data[d][size][operation]
                row.append("%f" % (info.mean / (1000.0 * 1000.0))) # us ---> ms
            csv_data.append(row)
            
        write_csv(os.path.join(result_dir, operation) + ".csv", csv_data, ";")
        
        
        # For comparison to base
        
        csv_data = []
        csv_data.append(["database", "base", "incremental"])
        
        for d in databases:
            row = [d]
            info = data[d][base_size_element][operation]
            row.append("%f" % (info.mean / (1000.0 * 1000.0))) # us ---> ms
            info = data[d][base_size][operation]
            row.append("%f" % (info.mean / (1000.0 * 1000.0))) # us ---> ms
            csv_data.append(row)
            
        write_csv(os.path.join(result_dir, "base-" +  operation) + ".csv", csv_data, ";")
    
    
    # Write out the plot files and generate the plots
    
    print "Generating plots:"
    
    for operation in operations:
        print "  %s" % operation

        plt_name = os.path.join(result_dir, operation) + ".plt"
        f = open(plt_name, "w")
        
        f.write("set autoscale\n")
        f.write("unset log\n")
        f.write("unset label\n")
        f.write("set xtic auto\n")
        f.write("set ytic auto\n")
        f.write("set xlabel 'Database Size'\n")
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
            f.write(("  '<sed \"1,2d\" %s.csv' using 1:%d title '%s' with lines"
                + " lt %d lw 2") % (operation, i + 2, database, lt_i))
            if i + 1 < len(databases):
                f.write(", \\\n")
            else:
                f.write("\n")
        f.close()
        
        os.system("bash -c 'cd %s && gnuplot %s.plt'" % (result_dir, operation))
        os.system("bash -c 'cd %s && epspdf %s.eps'" % (result_dir, operation))
        
        
        # For comparison to base
    
        plt_name = os.path.join(result_dir, "base-" + operation) + ".plt"
        f = open(plt_name, "w")
        
        f.write("set autoscale\n")
        f.write("unset log\n")
        f.write("unset label\n")
        f.write("set xtic auto\n")
        f.write("set ytic auto\n")
        f.write("set xlabel 'Database'\n")
        f.write("set ylabel 'Time (ms)'\n")
        f.write("set style data histogram\n")
        f.write("set style histogram cluster gap 1\n")
        f.write("set style fill solid border -1\n")
        f.write("set key top left\n")
        f.write("\n")
        f.write("set datafile separator ';'\n")
        f.write("set output 'base-%s.eps'\n" % operation)
        f.write("set terminal postscript enhanced eps color 'Helvetica' 20 size 5,3.5 dl 3\n")
        f.write("\n")

        f.write("plot\\\n")
        f.write(("  '<sed \"1,1d\" base-%s.csv' using 2:xtic(1) title 'base' lc 3, \\\n")
                    % (operation))
        f.write(("  '<sed \"1,1d\" base-%s.csv' using 3:xtic(1) title 'incremental' lc 1\n")
                    % (operation))
        f.close()
        
        os.system("bash -c 'cd %s && gnuplot base-%s.plt'" % (result_dir, operation))
        os.system("bash -c 'cd %s && epspdf base-%s.eps'" % (result_dir, operation))
    
    
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
    global re_extract_size_annotation, re_extract_base_annotation


    # Find the list of summary files in both the incremental and the final run

    incremental_dir = "Micro-2k-incremental/" + dbname + "/";
    incremental_files = os.listdir(incremental_dir)
    incremental_summaries = find_in_list(incremental_files, re_is_summary_file)
    incremental_summaries = add_prefix_to_all(incremental_summaries, incremental_dir)

    final_dir = "Micro-2k/" + dbname + "/";
    final_files = os.listdir(final_dir)
    final_summaries = find_in_list(final_files, re_is_summary_file)
    final_summaries = add_prefix_to_all(final_summaries, final_dir)
    
    if len(final_summaries) != 1:
        print "Error: Expected to find exactly 1 summary file in %s but %d found"\
                % (final_dir, len(final_summaries))
        sys.exit(1)
    
    summaries = list(incremental_summaries)
    summaries.append(final_summaries[0])
    
        
    # Process each summary file
    
    # Produce the following data structure:
    #   size -> operation -> OperationInfo
    # Where size is the incremental size, or a negative number in the case of
    # the base size.
    
    data = dict()
    for summary in summaries:
        m_size = re_extract_size_annotation.match(summary)
        if m_size is None:
            m_base = re_extract_base_annotation.match(summary)
            if m_base is None:
                print "Error: Cannot find the size or the base annotation in " \
                    + "the file name \"%s\"" % (summary)
                sys.exit(1)
            else:
                base_size = int(m_base.group(1))
                size = -base_size
        else:
            size = int(m_size.group(1))
        
        summary_data = dict()
        data[size] = summary_data
        
        first = True
        f = csv.read(open(summary, 'r'))
        
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
                OperationInfo(dbname, size, operation, mean, stdev, min, max)
        
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
