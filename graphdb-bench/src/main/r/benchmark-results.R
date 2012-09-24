#!/usr/bin/R --vanilla
#
# Functions for loading and working with benchmark results
#
# Author:
#   Peter Macko (http://eecs.harvard.edu/~pmacko)
#

library(outliers)


#
# Function install.prerequisites
#
# Description:
#   Installs are packages that are a prerequisite to this one
#
# Usage:
#   install.prerequisites()
#
install.prerequisites <- function() {

	install.packages("outliers")
}


#
# Function find.benchmark.results.file
#
# Description:
#   Finds the name of the result file for the given database engine name,
#   database instance name, and the workload argument.
#
# Usage:
#   find.benchmark.results.file(database.name, database.instance, workload.argument)
#
find.benchmark.results.file <- function(database.name, database.instance, workload.argument) {

	if (is.null(database.instance)) {
		subdir <- database.name
	}
	else {
		subdir <- paste(database.name, "_", database.instance, sep="")
	}

	if (grepl(paste("/", subdir, sep=""), getwd(), fixed=TRUE)) {
		dir <- "."
	}
	else {
		dir <- "."
		if (sum(grepl("/data", list.dirs(dir, recursive=FALSE), fixed=TRUE)) > 0) {
			dir <- paste(dir, "/data", sep="")
		}
		if (sum(grepl("/results", list.dirs(dir, recursive=FALSE), fixed=TRUE)) > 0) {
			dir <- paste(dir, "/results", sep="")
		}
		if (sum(grepl("/Micro", list.dirs(dir, recursive=FALSE), fixed=TRUE)) > 0) {
			dir <- paste(dir, "/Micro", sep="")
		}
		if (sum(grepl(paste("/", subdir, sep=""), list.dirs(dir, recursive=FALSE), fixed=TRUE)) > 0) {
			dir <- paste(dir, "/", subdir, sep="")
		}
	}
	
	if (is.null(database.instance)) {
		files <- list.files(dir, paste("^", database.name,
					"__.*", "__", workload.argument, "__.*\\.csv", sep=""))
	}
	else {
		files <- list.files(dir, paste("^", database.name, "_", database.instance,
					"__.*", "__", workload.argument, "__.*\\.csv", sep=""))
	}
	
	paste(dir, "/", sort(files, decreasing=TRUE)[1], sep="")
}


#
# Function load.benchmark.results
#
# Description:
#   Load GraphDB benchmark results for the given database engine name,
#   database instance name, and the workload argument and return them as
#   a data frame. Convert time to ms and memory to MB.
#
# Usage:
#   load.benchmark.results(database.name, database.instance, workload.argument)
#
load.benchmark.results <- function(database.name, database.instance, workload.argument) {

	data <- read.csv(find.benchmark.results.file(database.name, database.instance, workload.argument))
	
	# Convert the time to ms and memory to MB
	data$time   <- data$time / 1000000.0
	data$memory <- data$memory / 1000000.0
	
	data
}


#
# Function load.benchmark.results.khop
#
# Description:
#   Load GraphDB benchmark results for OperationGetKHopNeighbors
#
# Usage:
#   load.benchmark.results.khop(database.name, database.instance)
#
load.benchmark.results.khop <- function(database.name, database.instance, keep.outliers=FALSE) {

	data <- load.benchmark.results(database.name, database.instance, "get-k")

	
	# Isolate and parse OperationGetKHopNeighbors
	
	data.get.khop.all      <- data[substr(data$name, 0, 26) == "OperationGetKHopNeighbors-", ]
	data.get.khop          <- data.get.khop.all[!grepl("undirected", data.get.khop.all$name), ]
	data.get.khop$k        <- as.numeric(substring(data.get.khop$name, 27))
	data.get.khop$directed <- TRUE

	data.get.khop$result        <- lapply(strsplit(as.character(data.get.khop$result), ":"), as.numeric)
	data.get.khop$unique.nodes  <- as.numeric(lapply(data.get.khop$result, function(x) x[1]))
	data.get.khop$real.hops     <- as.numeric(lapply(data.get.khop$result, function(x) x[2]))
	data.get.khop$get.out.edges <- as.numeric(lapply(data.get.khop$result, function(x) x[3]))
	data.get.khop$get.in.vertex <- as.numeric(lapply(data.get.khop$result, function(x) x[4]))
	
	    
	if (!keep.outliers) {
		for (k in unique(data.get.khop$k)) {
			outliers <- outlier(data.get.khop[data.get.khop$k == k, ]$time)
			data.get.khop <- data.get.khop[!(data.get.khop$time %in% outliers), ]
		}
	}
	
	data.get.khop
}


#
# Function load.benchmark.results.khop.undirected
#
# Description:
#   Load GraphDB benchmark results for OperationGetKHopNeighbors
#
# Usage:
#   load.benchmark.results.khop.undirected(database.name, database.instance)
#
load.benchmark.results.khop.undirected <- function(database.name, database.instance, keep.outliers=FALSE) {

	data <- load.benchmark.results(database.name, database.instance, "get-k")

	
	# Isolate and parse OperationGetKHopNeighbors
	
	data.get.khop.all                 <- data[substr(data$name, 0, 26) == "OperationGetKHopNeighbors-", ]
	data.get.khop.undirected          <- data.get.khop.all[grepl("undirected", data.get.khop.all$name), ]
	data.get.khop.undirected$k        <- as.numeric(substring(sub("-undirected", "", data.get.khop.undirected$name), 27))
	data.get.khop.undirected$directed <- FALSE

	data.get.khop.undirected$result        <- lapply(strsplit(as.character(data.get.khop.undirected$result), ":"), as.numeric)
	data.get.khop.undirected$unique.nodes  <- as.numeric(lapply(data.get.khop.undirected$result, function(x) x[1]))
	data.get.khop.undirected$real.hops     <- as.numeric(lapply(data.get.khop.undirected$result, function(x) x[2]))
	data.get.khop.undirected$get.out.edges <- as.numeric(lapply(data.get.khop.undirected$result, function(x) x[3]))
	data.get.khop.undirected$get.in.vertex <- as.numeric(lapply(data.get.khop.undirected$result, function(x) x[4]))
	    
	if (!keep.outliers) {
		for (k in unique(data.get.khop.undirected$k)) {
			outliers <- outlier(data.get.khop.undirected[data.get.khop.undirected$k == k, ]$time)
			data.get.khop.undirected <- data.get.khop.undirected[!(data.get.khop.undirected$time %in% outliers), ]
		}
	}
	
	data.get.khop.undirected
}


#
# Function load.benchmark.results.parse.op.stat(data, start.index=2)
#
# Description:
#   Parse GraphUtils.OpStat structure in data$result
#
# Usage:
#   load.benchmark.results.parse.op.stat(data)
#
load.benchmark.results.parse.op.stat <- function(data, start.index=2) {
	
	stopifnot(unlist(strsplit(unlist(data[1,]$result)[start.index + 5],"="))[1] == "getAllVerticesNext")
	data$get.all.vertices.next <- as.numeric(lapply(data$result, function(x) unlist(strsplit(x[start.index + 5], "="))[2]))
	
	stopifnot(unlist(strsplit(unlist(data[1,]$result)[start.index + 6],"="))[1] == "uniqueVertices")
	data$unique.nodes <- as.numeric(lapply(data$result, function(x) unlist(strsplit(x[start.index + 6], "="))[2]))
	
	data
}


#
# Function load.benchmark.results.global.clustering.coefficient
#
# Description:
#   Load GraphDB benchmark results for OperationGlobalClusteringCoefficient
#
# Usage:
#   load.benchmark.results.global.clustering.coefficient(database.name, database.instance)
#
load.benchmark.results.global.clustering.coefficient <- function(database.name, database.instance) {

	data <- load.benchmark.results(database.name, database.instance, "clustering-coeff")

	
	# Isolate and parse OperationGlobalClusteringCoefficient
	
	data <- data[data$name == "OperationGlobalClusteringCoefficient", ]
	
	data$result <- strsplit(as.character(data$result), ":")
	data$coefficient  <- as.numeric(lapply(data$result, function(x) x[1]))
	
	data <- load.benchmark.results.parse.op.stat(data)

	data
}


#
# Function load.benchmark.results.network.average.clustering.coefficient
#
# Description:
#   Load GraphDB benchmark results for OperationNetworkAverageClusteringCoefficient
#
# Usage:
#   load.benchmark.results.network.average.clustering.coefficient(database.name, database.instance)
#
load.benchmark.results.network.average.clustering.coefficient <- function(database.name, database.instance) {

	data <- load.benchmark.results(database.name, database.instance, "clustering-coeff")

	
	# Isolate and parse OperationNetworkAverageClusteringCoefficient
	
	data <- data[data$name == "OperationNetworkAverageClusteringCoefficient", ]
	
	data$result <- strsplit(as.character(data$result), ":")
	data$coefficient  <- as.numeric(lapply(data$result, function(x) x[1]))
	
	data
}


#
# Function load.benchmark.results.all.neighbors
#
# Description:
#   Load GraphDB benchmark results for OperationGetAllNeighbors
#
# Usage:
#   load.benchmark.results.all.neighbors(database.name, database.instance)
#
load.benchmark.results.all.neighbors <- function(database.name, database.instance, keep.outliers=FALSE) {

	data <- load.benchmark.results(database.name, database.instance, "get")


	# Isolate and parse OperationGetAllNeighbors

	data              <- data[data$name == "OperationGetAllNeighbors", ]
	data$unique.nodes <- as.numeric(as.character(data$result))
	
	if (!keep.outliers) {
		data <- data[!outlier(data$time, logical=TRUE), ]
	}

	data
}


#
# Function khop.linear.model
#
# Description:
#   Create a linear model fit to k-hop data, and optionally filter the data
#
# Usage:
#   khop.linear.model(data)
#
khop.linear.model <- function(khop.data, limit=NaN) {
	
	d <- khop.data
	if (!is.nan(limit)) {
		d <- d[d$unique.nodes < limit,]
	}
	#d <- d[!outlier(d$time, logical=TRUE), ]
	
	lm(d$time ~ d$unique.nodes + d$unique.nodes, na.action=na.exclude)
}


#
# Function with.khop.linear.fit
#
# Description:
#   Add a linear model fit to k-hop data, and optionally filter the data
#
# Usage:
#   data <- with.khop.linear.fit(data)
#
with.khop.linear.fit <- function(khop.data, limit=NaN) {
	
	l <- khop.linear.model(khop.data, limit)
	d <- khop.data
	d$time.fit <- l$coefficients[1] + (l$coefficients[2] * d$unique.nodes)
	
	d
}


#
# Function khop.quadratic.model
#
# Description:
#   Create a quadratic model fit to k-hop data, and optionally filter the data
#
# Usage:
#   khop.quadratic.model(data)
#
khop.quadratic.model <- function(khop.data, limit=NaN) {
	
	d <- khop.data
	if (!is.nan(limit)) {
		d <- d[d$unique.nodes < limit,]
	}
	#d <- d[!outlier(d$time, logical=TRUE), ]
	
	lm(d$time ~ d$unique.nodes + I(d$unique.nodes^2), na.action=na.exclude)
}


#
# Function with.khop.quadratic.fit
#
# Description:
#   Add a linear model fit to k-hop data, and optionally filter the data
#
# Usage:
#   data <- with.khop.quadratic.fit(data)
#
with.khop.quadratic.fit <- function(khop.data, limit=NaN) {
	
	l <- khop.quadratic.model(khop.data, limit)
	d <- khop.data
	d$time.fit <- l$coefficients[1] + (l$coefficients[2] * d$unique.nodes) + (l$coefficients[3] * I(d$unique.nodes^2))
	
	d
}
