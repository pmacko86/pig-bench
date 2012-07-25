#!/usr/bin/python

import csv
import sys
import os.path
import os

from collections import defaultdict


##############################################################################
## Main function                                                            ##
##############################################################################

def main(filename):
    operations = set()
    
    for line in csv.reader(open(filename, 'r')):
        _,name,_,args,time,result,_ = line
        operations.add(name)

        if name == 'OperationGetFirstNeighbor':
            op_get_first_neighbor(time, result)
        elif name == 'OperationGetRandomNeighbor':
            op_get_random_neighbor(time, result)
        elif name == 'OperationGetAllNeighbors':
            op_get_all_neighbors(time, result)
        elif name.startswith('OperationGetKFirstNeighbors'):
	    operations.add('OperationGetKFirstNeighbors')
            op_get_k_first_neighbors(args, time, result)
	elif name.startswith('OperationGetKRandomNeighbors'):
	    operations.add('OperationGetKRandomNeighbors')
	    op_get_k_random_neighbors(args, time, result)
	elif name.startswith('OperationGetKHopNeighbors'):
	    operations.add('OperationGetKHopNeighbors')
	    op_get_k_hop_neighbors(args, time, result)
        elif name == 'OperationGetShortestPath':
            op_get_shortest_path(time, result)
        elif name == 'OperationGetShortestPathProperty':
            op_get_shortest_path_property(time, result)
        elif name == 'OperationGlobalClusteringCoefficient':
            op_global_clustering_coefficient(time, result)
        elif name == 'OperationNetworkAverageClusteringCoefficient':
            op_network_average_clustering_coefficient(time, result)

    dirname,_ = os.path.splitext(filename)
    if not os.path.isdir(dirname):
        os.makedirs(dirname)

    if 'OperationGetFirstNeighbor' in operations:
        print_get_first_neighbor(dirname + '/get_first_neighbor')
    if 'OperationGetRandomNeighbor' in operations:
        print_get_random_neighbor(dirname + '/get_random_neighbor')
    if 'OperationGetAllNeighbors' in operations:
        print_get_all_neighbors(dirname + '/get_all_neighbors')
    if 'OperationGetKFirstNeighbors' in operations:
        print_get_k_first_neighbors(dirname + '/get_k_first_neighbors')
    if 'OperationGetKRandomNeighbors' in operations:
        print_get_k_random_neighbors(dirname + '/get_k_random_neighbors')
    if 'OperationGetKHopNeighbors' in operations:
        print_get_k_hop_neighbors(dirname + '/get_k_hop_neighbors')
    if 'OperationGetShortestPath' in operations:
        print_get_shortest_path(dirname + '/get_shortest_path')
    if 'OperationGetShortestPathProperty' in operations:
        print_get_shortest_path_property(dirname + '/get_shortest_path_property')
    if 'OperationGlobalClusteringCoefficient' in operations:
        print_global_clustering_coefficient(dirname + '/global_clustering_coefficient')
    if 'OperationNetworkAverageClusteringCoefficient' in operations:
        print_network_average_clustering_coefficient(dirname + '/network_average_clustering_coefficient')



##############################################################################
## Helpers and Utilities                                                    ##
##############################################################################

def key_value_pairs_to_dict(string, skip=0, with_count=True, separator=':', separator_kv='='):
    result = dict()
    kv_pairs = string.split(separator)
    for kv in kv_pairs:
        if skip > 0:
            skip -= 1
            continue
        k, v = kv.split(separator_kv)
        if '.' in v:
            result[k] = float(v)
        else:
            result[k] = int(v)
        if with_count:
            result[k + "##COUNT"] = 1
    return result


def add_dict(d1, d2):
    for k in d2.keys():
        if k in d1.keys():
            d1[k] += d2[k]
        else:
            d1[k] = d2[k]


def avg_dict_item(d, key):
    if key not in d.keys(): return 0
    if key + "##COUNT" not in d.keys(): return 0
    count = int(d[key + "##COUNT"])
    if count == 0: return 0
    return float(d[key]) / float(count)


def safe_div(a, b):
    if b == 0:
        return 0
    else:
        return a / b



##############################################################################
## Call-back and print functions for each operator                          ##
##############################################################################

op_get_first_neighbor_sum = 0.0
op_get_first_neighbor_total = 0.0
op_get_first_neighbor_dne_sum = 0.0
op_get_first_neighbor_dne_total = 0.0

def op_get_first_neighbor(time, result):
    global op_get_first_neighbor_sum, op_get_first_neighbor_total, op_get_first_neighbor_dne_sum, op_get_first_neighbor_dne_total
    time = int(time)

    if result != 'DNE':
        op_get_first_neighbor_sum += time
        op_get_first_neighbor_total += 1.0
    else:
        op_get_first_neighbor_dne_sum += time
        op_get_first_neighbor_dne_total += 1.0

def print_get_first_neighbor(filename):
    f = open(filename, 'w')

    f.write('mean_without_dne;mean_dne;mean\n')
    f.write('%f;' % (op_get_first_neighbor_sum / op_get_first_neighbor_total))
    f.write('%f;' % (op_get_first_neighbor_dne_sum / op_get_first_neighbor_dne_total if op_get_first_neighbor_dne_total != 0 else 1))
    f.write('%f\n' % ((op_get_first_neighbor_total + op_get_first_neighbor_dne_total) / (op_get_first_neighbor_sum + op_get_first_neighbor_dne_sum)))

    f.close()


op_get_random_neighbor_sum = 0.0
op_get_random_neighbor_total = 0.0
op_get_random_neighbor_dne_sum = 0.0
op_get_random_neighbor_dne_total = 0.0

def op_get_random_neighbor(time, result):
    global op_get_random_neighbor_sum, op_get_random_neighbor_total, op_get_random_neighbor_dne_sum, op_get_random_neighbor_dne_total
    time = int(time)

    if result != 'DNE':
        op_get_random_neighbor_sum += time
        op_get_random_neighbor_total += 1.0
    else:
        op_get_random_neighbor_dne_sum += time
        op_get_random_neighbor_dne_total += 1.0

def print_get_random_neighbor(filename):
    f = open(filename, 'w')

    f.write('mean_without_dne;mean_dne;mean\n')
    f.write('%f;' % (op_get_random_neighbor_sum / op_get_random_neighbor_total))
    f.write('%f;' % (op_get_random_neighbor_dne_sum / op_get_random_neighbor_dne_total if op_get_random_neighbor_dne_total != 0 else 1))
    f.write('%f\n' % ((op_get_random_neighbor_total + op_get_random_neighbor_dne_total) / (op_get_random_neighbor_sum + op_get_random_neighbor_dne_sum)))

    f.close()


op_get_all_neighbors_list = []
op_get_all_neighbors_sum = 0.0
op_get_all_neighbors_weighted_sum = 0.0
op_get_all_neighbors_weighted_total = 0.0

def op_get_all_neighbors(time, result):
    global op_get_all_neighbors_list, op_get_all_neighbors_sum, op_get_all_neighbors_weighted_sum, op_get_all_neighbors_weighted_total
    time = int(time)
    result = int(result)

    op_get_all_neighbors_list.append((time, result))

    op_get_all_neighbors_sum += time
    op_get_all_neighbors_weighted_sum += time / result if result != 0.0 else 0.0
    op_get_all_neighbors_weighted_total += result


def print_get_all_neighbors(filename):
    f = open(filename, 'w')

    f.write('mean;weighted_mean_per_op;weighted_mean_over_total\n')
    f.write('%f;' % (op_get_all_neighbors_sum / len(op_get_all_neighbors_list)))
    f.write('%f;' % (op_get_all_neighbors_weighted_sum / len(op_get_all_neighbors_list)))
    f.write('%f\n' % (op_get_all_neighbors_sum / op_get_all_neighbors_weighted_total))

    f.write('time;neighbor_count\n')
    for (time, result) in op_get_all_neighbors_list:
        f.write('%f;%f\n' % (time, result))

    f.close()
    

op_get_k_first_neighbors_list = defaultdict(list)
op_get_k_first_neighbors_sum = defaultdict(float)
op_get_k_first_neighbors_weighted_sum = defaultdict(float)
op_get_k_first_neighbors_weighted_total = defaultdict(float)

def op_get_k_first_neighbors(args, time, result):
    global op_get_k_first_neighbors_list, op_get_k_first_neighbors_sum, op_get_k_first_neighbors_weighted_sum, op_get_k_first_neighbors_weighted_total
    _, k = args.strip('[]').split(',')
    k = int(k)
    time = int(time)
    result = int(result)

    op_get_k_first_neighbors_list[k].append((time, result))

    op_get_k_first_neighbors_sum[k] += time
    op_get_k_first_neighbors_weighted_sum[k] += time / result if result != 0.0 else 0.0
    op_get_k_first_neighbors_weighted_total[k] += result

def print_get_k_first_neighbors(filename):
    f = open(filename, 'w')

    f.write('k;mean;weighted_mean_per_op;weighted_mean_over_total\n')
    for k in op_get_k_first_neighbors_sum.keys():
        f.write('%d;' % k)
        f.write('%f;' % (op_get_k_first_neighbors_sum[k] / len(op_get_k_first_neighbors_list[k])))
        f.write('%f;' % (op_get_k_first_neighbors_weighted_sum[k] / len(op_get_k_first_neighbors_list[k])))
        f.write('%f\n' % (op_get_k_first_neighbors_sum[k] / op_get_k_first_neighbors_weighted_total[k]))

    f.write('k;time;neighbor_count\n')
    for (k, l) in op_get_k_first_neighbors_list.items():
        for (time, result) in l:
            f.write('%f;%f;%f\n' % (k, time, result))

    f.close()


op_get_k_random_neighbors_list = defaultdict(list)
op_get_k_random_neighbors_sum = defaultdict(float)
op_get_k_random_neighbors_weighted_by_nodecount_sum = defaultdict(float)
op_get_k_random_neighbors_weighted_by_getcount_sum = defaultdict(float)
op_get_k_random_neighbors_nodecount_total = defaultdict(float)
op_get_k_random_neighbors_getcount_total = defaultdict(float)

def op_get_k_random_neighbors(args, time, result):
    global op_get_k_random_neighbors_list, op_get_k_random_neighbors_sum, op_get_k_random_neighbors_weighted_by_nodecount_sum, op_get_k_random_neighbors_weighted_by_getcount_sum, op_get_k_random_neighbors_nodecount_total, op_get_k_random_neighbors_getcount_total
    _, k = args.strip('[]').split(',')
    k = int(k)    
    time = int(time)
    (nodecount, getcount) = result.split(':')
    nodecount = int(nodecount)
    getcount = int(getcount)

    op_get_k_random_neighbors_list[k].append((time, nodecount, getcount))
    #op_get_k_random_neighbors_list.append((time, getcount))

    op_get_k_random_neighbors_sum[k] += time
    op_get_k_random_neighbors_weighted_by_nodecount_sum[k] += time / nodecount if nodecount != 0.0 else 0.0
    op_get_k_random_neighbors_weighted_by_getcount_sum[k] += time / getcount if getcount != 0.0 else 0.0
    op_get_k_random_neighbors_nodecount_total[k] += nodecount
    op_get_k_random_neighbors_getcount_total[k] += getcount

def print_get_k_random_neighbors(filename):
    f = open(filename, 'w')

    f.write('k;mean;weighted_by_nodecount_per_op;weighted_by_nodecount_over_total;weighted_by_getcount_per_op;weighted_by_getcount_over_total\n')
    #f.write('mean;weighted_by_getcount_per_op;weighted_by_getcount_over_total\n')
    for k in op_get_k_random_neighbors_sum.keys():
        f.write('%d;' % k)
        f.write('%f;' % (op_get_k_random_neighbors_sum[k] / len(op_get_k_random_neighbors_list[k])))
        f.write('%f;' % (op_get_k_random_neighbors_weighted_by_nodecount_sum[k] / len(op_get_k_random_neighbors_list[k])))
        f.write('%f;' % (op_get_k_random_neighbors_sum[k] / op_get_k_random_neighbors_nodecount_total[k]))
        f.write('%f;' % (op_get_k_random_neighbors_weighted_by_getcount_sum[k] / len(op_get_k_random_neighbors_list[k])))
        f.write('%f\n' % (op_get_k_random_neighbors_sum[k] / op_get_k_random_neighbors_getcount_total[k]))

    f.write('k;time;nodecount;getcount')
    for (k, l) in op_get_k_random_neighbors_list.items():
        for (time, nodecount, getcount) in l:
            f.write('%f;%f;%f;%f\n' % (k, time, nodecount, getcount))
    #f.write('time;getcount\n')
    #for (time, getcount) in op_get_k_random_neighbors_list:
    #    f.write('%f;%f\n' % (time, getcount))

    f.close()


op_get_k_hop_neighbors_list = defaultdict(list)
op_get_k_hop_neighbors_sum = defaultdict(float)

op_get_k_hop_neighbors_weighted_by_dedup_sum = defaultdict(float)
op_get_k_hop_neighbors_weighted_by_realhop_sum = defaultdict(float)
op_get_k_hop_neighbors_weighted_by_getcount_sum = defaultdict(float)
op_get_k_hop_neighbors_weighted_by_nodecount_sum = defaultdict(float)

op_get_k_hop_neighbors_dedup_total = defaultdict(float)
op_get_k_hop_neighbors_realhop_total = defaultdict(float)
op_get_k_hop_neighbors_getcount_total = defaultdict(float)
op_get_k_hop_neighbors_nodecount_total = defaultdict(float)

def op_get_k_hop_neighbors(args, time, result):
    global op_get_k_hop_neighbors_list, op_get_k_hop_neighbors_sum, \
        op_get_k_hop_neighbors_weighted_by_dedup_sum, op_get_k_hop_neighbors_weighted_by_realhop_sum, op_get_k_hop_neighbors_weighted_by_getcount_sum, op_get_k_hop_neighbors_weighted_by_nodecount_sum, \
        op_get_k_hop_neighbors_dedup_total, op_get_k_hop_neighbors_realhop_total, op_get_k_hop_neighbors_getcount_total, op_get_k_hop_neighbors_nodecount_total

    _, k = args.strip('[]').split(',')
    k = int(k)
    time = int(time)
    (dedup, realhops, getcount, nodecount) = result.split(':')
    dedup = int(dedup)
    realhops = int(realhops)
    getcount = int(getcount)
    nodecount = int(nodecount)

    op_get_k_hop_neighbors_list[k].append((time, dedup, realhops, getcount, nodecount))

    op_get_k_hop_neighbors_sum[k] += time

    op_get_k_hop_neighbors_weighted_by_dedup_sum[k] += time / dedup if dedup != 0.0 else 0.0
    op_get_k_hop_neighbors_weighted_by_realhop_sum[k] += time / realhops if realhops != 0.0 else 0.0
    op_get_k_hop_neighbors_weighted_by_getcount_sum[k] += time / getcount if getcount != 0.0 else 0.0
    op_get_k_hop_neighbors_weighted_by_nodecount_sum[k] += time / nodecount if nodecount != 0.0 else 0.0

    op_get_k_hop_neighbors_dedup_total[k] += dedup
    op_get_k_hop_neighbors_realhop_total[k] += realhops
    op_get_k_hop_neighbors_getcount_total[k] += getcount
    op_get_k_hop_neighbors_nodecount_total[k] += nodecount

def print_get_k_hop_neighbors(filename):
    f = open(filename, 'w')

    f.write('k;mean;\
weighted_by_dedup_per_op;weighted_by_dedup_over_total;\
weighted_by_realhop_per_op;weighted_by_realhop_over_total;\
weighted_by_getcount_per_op;weighted_by_getcount_over_total;\
weighted_by_nodecount_per_op;weighted_by_nodecount_over_total\n')
    for k in op_get_k_hop_neighbors_sum.keys():
        f.write('%d;' % k)
        f.write('%f;' % (op_get_k_hop_neighbors_sum[k] / len(op_get_k_hop_neighbors_list[k])))

        f.write('%f;' % (op_get_k_hop_neighbors_weighted_by_dedup_sum[k] / len(op_get_k_hop_neighbors_list[k])))
        f.write('%f;' % (op_get_k_hop_neighbors_sum[k] / op_get_k_hop_neighbors_dedup_total[k]))

        f.write('%f;' % (op_get_k_hop_neighbors_weighted_by_realhop_sum[k] / len(op_get_k_hop_neighbors_list[k])))
        f.write('%f;' % (op_get_k_hop_neighbors_sum[k] / op_get_k_hop_neighbors_realhop_total[k]))

        f.write('%f;' % (op_get_k_hop_neighbors_weighted_by_getcount_sum[k] / len(op_get_k_hop_neighbors_list[k])))
        f.write('%f;' % (op_get_k_hop_neighbors_sum[k] / op_get_k_hop_neighbors_getcount_total[k]))

        f.write('%f;' % (op_get_k_hop_neighbors_weighted_by_nodecount_sum[k] / len(op_get_k_hop_neighbors_list[k])))
        f.write('%f\n' % (op_get_k_hop_neighbors_sum[k] / op_get_k_hop_neighbors_nodecount_total[k]))


    f.write('k;time;dedup;realhops;getcount;nodecount\n')
    for (k, l) in op_get_k_hop_neighbors_list.items():
        for (time, dedup, realhops, getcount, nodecount) in l:
            f.write('%f;%f;%f;%f;%f;%f\n' % (k, time, dedup, realhops, getcount, nodecount))

    f.close()


op_get_shortest_path_list = []
op_get_shortest_path_sum = 0.0
op_get_shortest_path_weighted_by_pathlen_sum = 0.0
op_get_shortest_path_weighted_by_getcount_sum = 0.0
op_get_shortest_path_weighted_by_nodecount_sum = 0.0
op_get_shortest_path_pathlen_total = 0.0
op_get_shortest_path_getcount_total = 0.0
op_get_shortest_path_nodecount_total = 0.0

def op_get_shortest_path(time, result):
    global op_get_shortest_path_list, op_get_shortest_path_sum, op_get_shortest_path_weighted_by_pathlen_sum, op_get_shortest_path_weighted_by_getcount_sum, op_get_shortest_path_weighted_by_nodecount_sum, op_get_shortest_path_pathlen_total, op_get_shortest_path_getcount_total, op_get_shortest_path_nodecount_total
    time = int(time)
    (pathlen, getcount, nodecount) = result.split(':')
    pathlen = int(pathlen)
    getcount = int(getcount)
    nodecount = int(nodecount)

    op_get_shortest_path_list.append((time, pathlen, getcount, nodecount))

    op_get_shortest_path_sum += time
    op_get_shortest_path_weighted_by_pathlen_sum += time / pathlen if pathlen != 0.0 else 0.0
    op_get_shortest_path_weighted_by_getcount_sum += time / getcount if getcount != 0.0 else 0.0
    op_get_shortest_path_weighted_by_nodecount_sum += time / nodecount if nodecount != 0.0 else 0.0
    op_get_shortest_path_pathlen_total += pathlen
    op_get_shortest_path_getcount_total += getcount
    op_get_shortest_path_nodecount_total += nodecount

def print_get_shortest_path(filename):
    f = open(filename, 'w')

    f.write('mean;weighted_by_pathlen_per_op;weighted_by_pathlen_over_total;weighted_by_getcount_per_op;weighted_by_getcount_over_total;weighted_by_nodecount_per_op;weighted_by_nodecount_over_total\n')
    f.write('%f;' % (op_get_shortest_path_sum / len(op_get_shortest_path_list)))
    f.write('%f;' % (op_get_shortest_path_weighted_by_pathlen_sum / len(op_get_shortest_path_list)))
    f.write('%f;' % (op_get_shortest_path_sum / op_get_shortest_path_pathlen_total))
    f.write('%f;' % (op_get_shortest_path_weighted_by_getcount_sum / len(op_get_shortest_path_list)))
    f.write('%f;' % (op_get_shortest_path_sum / op_get_shortest_path_getcount_total))
    f.write('%f;' % (op_get_shortest_path_weighted_by_nodecount_sum / len(op_get_shortest_path_list)))
    f.write('%f\n' % (op_get_shortest_path_sum / op_get_shortest_path_nodecount_total))

    f.write('time;pathlen;getcount;nodecount\n')
    for (time, pathlen, getcount, nodecount) in op_get_shortest_path_list:
        f.write('%f;%f;%f;%f\n' % (time, pathlen, nodecount, getcount))

    f.close()


def op_get_shortest_path_property(time, result):
    return

def print_get_shortest_path_property(filename):
    return


op_global_clustering_coefficient_list = []
op_global_clustering_coefficient_sum = dict()

def op_global_clustering_coefficient(time, result):
    r = float(result.split(':')[0])
    d = key_value_pairs_to_dict(result, 1)
    d['time'] = time
    d['time##COUNT'] = 1
    add_dict(op_global_clustering_coefficient_sum, d)
    op_global_clustering_coefficient_list.append([r, float(time), d])
    return

def print_global_clustering_coefficient(filename):
    f = open(filename, 'w')
    d_sum = op_global_clustering_coefficient_sum 

    f.write('mean\n')
    f.write('%f\n' % avg_dict_item(d_sum, "time"))

    f.write('time;result;get_inout_vertex_count;get_vertices_next_count;get_inout_edge_count;node_count\n')
    for (r, time, d) in op_global_clustering_coefficient_list:
        f.write('%f;'  % time)
        f.write('%f;'  % r)
        f.write('%d;'  % (d['getInVertex'] + d['getOutVertex']))
        f.write('%d;'  %  d['getVerticesNext'])
        f.write('%d;'  % (d['getInEdges'] + d['getOutEdges']))
        f.write('%d\n' %  d['uniqueVertices'])

    f.close()
    return


op_network_average_clustering_coefficient_list = []
op_network_average_clustering_coefficient_sum = dict()

def op_network_average_clustering_coefficient(time, result):
    r = float(result.split(':')[0])
    d = key_value_pairs_to_dict(result, 1)
    d['time'] = time
    d['time##COUNT'] = 1
    add_dict(op_network_average_clustering_coefficient_sum, d)
    op_network_average_clustering_coefficient_list.append([r, float(time), d])
    return

def print_network_average_clustering_coefficient(filename):
    f = open(filename, 'w')
    d_sum = op_network_average_clustering_coefficient_sum

    f.write('mean\n')
    f.write('%f\n' % avg_dict_item(d_sum, "time"))

    f.write('time;result;get_inout_vertex_count;get_vertices_next_count;get_inout_edge_count;node_count\n')
    for (r, time, d) in op_global_clustering_coefficient_list:
        f.write('%f;'  % time)
        f.write('%f;'  % r)
        f.write('%d;'  % (d['getInVertex'] + d['getOutVertex']))
        f.write('%d;'  %  d['getVerticesNext'])
        f.write('%d;'  % (d['getInEdges'] + d['getOutEdges']))
        f.write('%d\n' %  d['uniqueVertices'])

    f.close()
    return



##############################################################################
## The script startup                                                       ##
##############################################################################

if __name__ == '__main__':
    if len(sys.argv) == 2:
        main(sys.argv[1])
    else:
        print 'usage: script.py filename'

