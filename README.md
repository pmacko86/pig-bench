# PIG: Performance Introspection of Graph Databases

The explosion of graph data in social and biological networks, recommendation systems, provenance databases, etc. makes graph storage and processing of paramount importance.

PIG is a new graph benchmarking framework, which provides both a methodology for evaluating graph database performance and a mechanism to carry out such evaluations. It takes a hierarchical approach to benchmarking. The suite has three layers of benchmarks:

- Primitive operations such as reading and writing vertices and edges
- Composite access patterns such as extracting k-hop neighborhoods
- Graph algorithms such as shortest path finding and computing centrality metrics

This framework allows for comparisons between systems as well as single system introspection. Such introspection allows one to evaluate the degree to which systems exploit their knowledge of graph access patterns. The suite also comes with a web interface that makes it easy to run benchmarks and to visualize and analyze the collected data.

## Quick-Start

To run PIG, you will need:
- Linux, Java 1.6 or newer (JRockit JVM is *highly* recommended), Maven
- [Blueprints Extensions](https://github.com/pmacko86/blueprints-extensions)
- [Core Provenance Library](https://github.com/pmacko86/core-provenance-library)

After installing all the prerequisites and checking out the source code of PIG, cd into the graphdb-bench directory and type:
```
mvn install
```
You can then start the web interface using:
```
./runWebInterfaceServer.sh
```
This will start a server on port 8080. Or you can run the benchmark tools directly from the command-line using:
```
./runBenchmarkSuite.sh`
```
Use the `--help` option to get the list of available commands or +help to see advanced options and options for configuring the JVM.

## Configuration
To edit the configuration of PIG, please edit the following file:
```
graphdb-bench/src/main/resources/com/tinkerpop/bench/bench.properties`
```
You can also override many options using command-line arguments and/or the web interface.

## Datasets
You can generate your own datasets using fgftool distributed as a part of Blueprints Extensions (one of the prerequisites of PIG). You can also download datasets with up to 1 million nodes from here:

  https://drive.google.com/folderview?id=0B3jkRHQ7nKvnbDhsWHBySVV6VVk&usp=sharing

Place the datasets in the directory specified in the configuration file. The default is `data/datasets` in the project directory.

## Publications

- Peter Macko, Daniel Margo, and Margo Seltzer. Performance Introspection of Graph Databases. 6th International Systems and Storage Conference (SYSTOR '13), Haifa, Israel, June 2013. ([pdf](http://www.eecs.harvard.edu/~pmacko/papers/pig-systor13.pdf))
