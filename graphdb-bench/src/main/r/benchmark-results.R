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
	
	data          <- data[substr(data$name, 0, 26) == "OperationGetKHopNeighbors-", ]
	data          <- data[!grepl("undirected", data$name), ]
	data$k        <- as.numeric(substring(data$name, 27))
	data$directed <- TRUE

	data$result            <- lapply(strsplit(as.character(data$result), ":"), as.numeric)
	data$unique.vertices   <- as.numeric(lapply(data$result, function(x) x[1]))
	data$real.hops         <- as.numeric(lapply(data$result, function(x) x[2]))
	data$get.vertices      <- as.numeric(lapply(data$result, function(x) x[3]))
	data$get.vertices.next <- as.numeric(lapply(data$result, function(x) x[4]))
	
	    
	if (!keep.outliers) {
		for (k in unique(data$k)) {
			outliers <- outlier(data[data$k == k, ]$time)
			data <- data[!(data$time %in% outliers), ]
		}
	}
	
	data
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
	
	data          <- data[substr(data$name, 0, 26) == "OperationGetKHopNeighbors-", ]
	data          <- data[grepl("undirected", data$name), ]
	data$k        <- as.numeric(substring(sub("-undirected", "", data$name), 27))
	data$directed <- FALSE

	data$result            <- lapply(strsplit(as.character(data$result), ":"), as.numeric)
	data$unique.vertices   <- as.numeric(lapply(data$result, function(x) x[1]))
	data$real.hops         <- as.numeric(lapply(data$result, function(x) x[2]))
	data$get.vertices      <- as.numeric(lapply(data$result, function(x) x[3]))
	data$get.vertices.next <- as.numeric(lapply(data$result, function(x) x[4]))
	
	
	if (!keep.outliers) {
		for (k in unique(data$k)) {
			outliers <- outlier(data[data$k == k, ]$time)
			data <- data[!(data$time %in% outliers), ]
		}
	}
	
	data
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
	
	stopifnot(unlist(strsplit(unlist(data[1,]$result)[start.index + 0],"="))[1] == "getVertices")
	data$get.vertices <- as.numeric(lapply(data$result, function(x) unlist(strsplit(x[start.index + 0], "="))[2]))
	
	stopifnot(unlist(strsplit(unlist(data[1,]$result)[start.index + 1],"="))[1] == "getVerticesNext")
	data$get.vertices.next <- as.numeric(lapply(data$result, function(x) unlist(strsplit(x[start.index + 1], "="))[2]))
	
	stopifnot(unlist(strsplit(unlist(data[1,]$result)[start.index + 2],"="))[1] == "getEdges")
	data$get.edges <- as.numeric(lapply(data$result, function(x) unlist(strsplit(x[start.index + 2], "="))[2]))
	
	stopifnot(unlist(strsplit(unlist(data[1,]$result)[start.index + 3],"="))[1] == "getEdgesNext")
	data$get.edges.next <- as.numeric(lapply(data$result, function(x) unlist(strsplit(x[start.index + 3], "="))[2]))
	
	stopifnot(unlist(strsplit(unlist(data[1,]$result)[start.index + 4],"="))[1] == "getAllVertices")
	data$get.all.vertices <- as.numeric(lapply(data$result, function(x) unlist(strsplit(x[start.index + 4], "="))[2]))
	
	stopifnot(unlist(strsplit(unlist(data[1,]$result)[start.index + 5],"="))[1] == "getAllVerticesNext")
	data$get.all.vertices.next <- as.numeric(lapply(data$result, function(x) unlist(strsplit(x[start.index + 5], "="))[2]))
	
	stopifnot(unlist(strsplit(unlist(data[1,]$result)[start.index + 6],"="))[1] == "uniqueVertices")
	data$unique.vertices <- as.numeric(lapply(data$result, function(x) unlist(strsplit(x[start.index + 6], "="))[2]))
	
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
	
	data <- load.benchmark.results.parse.op.stat(data)
	
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
	data$unique.vertices <- as.numeric(as.character(data$result))
	
	if (!keep.outliers) {
		data <- data[!outlier(data$time, logical=TRUE), ]
	}

	data
}


#
# Function khop.linear.model
#
# Description:
#   Create a linear model fit to k-hop data
#
# Usage:
#   khop.linear.model(data)
#
khop.linear.model <- function(khop.data, limit=NaN) {
	
	d <- khop.data
	if (!is.nan(limit)) {
		d <- d[d$unique.vertices < limit,]
	}
	#d <- d[!outlier(d$time, logical=TRUE), ]
	
	lm(d$time ~ d$unique.vertices + d$unique.vertices, na.action=na.exclude)
}


#
# Function khop.quadratic.model
#
# Description:
#   Create a quadratic model fit to k-hop data
#
# Usage:
#   khop.quadratic.model(data)
#
khop.quadratic.model <- function(khop.data, limit=NaN) {
	
	d <- khop.data
	if (!is.nan(limit)) {
		d <- d[d$unique.vertices < limit,]
	}
	#d <- d[!outlier(d$time, logical=TRUE), ]
	
	lm(d$time ~ d$unique.vertices + I(d$unique.vertices^2), na.action=na.exclude)
}


#
# Function with.khop.polynomial.fit
#
# Description:
#   Apply a polynomial model
#
with.khop.polynomial.fit <- function(khop.data, model) {
	
	d <- khop.data
	d$time.fit <- 0
	
	for (c in rev(model$coefficients)) {
		d$time.fit <- (d$time.fit * d$unique.vertices) + c
	}
	
	d
}


#
# Function with.khop.linear.fit
#
# Description:
#   Add a linear model fit to k-hop data
#
# Usage:
#   data <- with.khop.linear.fit(data)
#
with.khop.linear.fit <- function(khop.data, limit=NaN) {
	
	l <- khop.linear.model(khop.data, limit)
	d <- khop.data
	d$time.fit <- l$coefficients[1] + (l$coefficients[2] * d$unique.vertices)
	
	d
}


#
# Function with.khop.quadratic.fit
#
# Description:
#   Add a linear model fit to k-hop data
#
# Usage:
#   data <- with.khop.quadratic.fit(data)
#
with.khop.quadratic.fit <- function(khop.data, limit=NaN) {
	
	l <- khop.quadratic.model(khop.data, limit)
	d <- khop.data
	d$time.fit <- l$coefficients[1] + (l$coefficients[2] * d$unique.vertices) + (l$coefficients[3] * I(d$unique.vertices^2))
	
	d
}


##
##
## Plotting
##
##

#
# Colors for k in k-hop plots
#

khop.colors <- c("violet", "blue", "green", "orange", "red")


#
# Function plot.khops
#
# Description:
#   Plot a khops plot
#
plot.khops <- function(khop.data, x, y, xlab, ylab, log="", xmax=NaN, ymax=NaN, kmax=NaN, hold=FALSE) {
	
	data <- khop.data
	mask <- !is.nan(x) & !is.nan(y)
	if (!is.nan(xmax)) {
		mask <- mask & (x <= xmax)
	}
	if (!is.nan(ymax)) {
		mask <- mask & (y <= ymax)
	}
	if (!is.nan(kmax)) {
		mask <- mask & (data$k <= kmax)
	}
	
	if (!hold) {
		plot(x[mask], y[mask], log=log, xlab=xlab, ylab=ylab)
	}
	
	k.values <- unique(data$k)
	for (k in sort(k.values, decreasing=TRUE)) {
		m <- mask & (data$k == k)
		points(x[m], y[m], col=khop.colors[k])
	}
}


#
# Function plot.khops.time.vs.unique.vertices
#
# Description:
#   Plot time vs. unique vertices for khops
#
plot.khops.time.vs.unique.vertices <- function(khop.data, ...) {

	plot.khops(khop.data, khop.data$unique.vertices, khop.data$time, "Unique Vertices", "Time (ms)", ...)
}


#
# Function plot.khops.time.vs.retrieved.vertices
#
# Description:
#   Plot time vs. retrieved vertices for khops
#
plot.khops.time.vs.retrieved.vertices <- function(khop.data, ...) {
	
	plot.khops(khop.data, khop.data$get.vertices.next, khop.data$time, "Retrieved Vertices", "Time (ms)",...)
}


#
# Function plot.khops.time.vs.retrieved.neighborhoods
#
# Description:
#   Plot time vs. retrieved neighborhoods for khops
#
plot.khops.time.vs.retrieved.neighborhoods <- function(khop.data, ...) {
	
	plot.khops(khop.data, khop.data$get.vertices, khop.data$time, "Retrieved Neighborhoods", "Time (ms)", ...)
}
