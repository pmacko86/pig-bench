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



################################################################################
##                                                                            ##
##  Loading and Parsing                                                       ##
##                                                                            ##
################################################################################


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
	
	
	# Database name and instance
	
	data$database.name <- database.name
	data$database.instance <- database.instance
	
	
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
load.benchmark.results.khop <- function(database.name, database.instance, keep.outliers=FALSE, directed=TRUE) {

	data <- load.benchmark.results(database.name, database.instance, "get-k")

	
	# Isolate and parse OperationGetKHopNeighbors
	
	if (directed) {
		data          <- data[substr(data$name, 0, 26) == "OperationGetKHopNeighbors-", ]
		data          <- data[!grepl("undirected", data$name), ]
		data$k        <- as.numeric(substring(data$name, 27))
		data$directed <- TRUE
	}
	else {
		data          <- data[substr(data$name, 0, 26) == "OperationGetKHopNeighbors-", ]
		data          <- data[grepl("undirected", data$name), ]
		data$k        <- as.numeric(substring(sub("-undirected", "", data$name), 27))
		data$directed <- FALSE
	}
	
	
	# Parse the statistics
	
	data$result            <- lapply(strsplit(as.character(data$result), ":"), as.numeric)
	data$unique.vertices   <- as.numeric(lapply(data$result, function(x) x[1]))
	data$real.hops         <- as.numeric(lapply(data$result, function(x) x[2]))
	data$get.vertices      <- as.numeric(lapply(data$result, function(x) x[3]))
	data$get.vertices.next <- as.numeric(lapply(data$result, function(x) x[4]))
	
	data$retrieved.vertices      <- data$get.vertices.next
	data$retrieved.neighborhoods <- data$get.vertices
	
	
	# Remove outliers
	
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

	load.benchmark.results.khop(database.name, database.instance, keep.outliers=keep.outliers, directed=FALSE)
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



################################################################################
##                                                                            ##
##  Models                                                                    ##
##                                                                            ##
################################################################################


#
# Function khop.polynomial.model
#
# Description:
#   Create a polynomial model fit to k-hop data
#
# Usage:
#   khop.polynomial.model(data)
#
khop.polynomial.model <- function(khop.data, x=NULL, n=1, limit=NA) {
	
	d <- khop.data	
	if (is.null(x)) {
		x <- d$unique.vertices
	}
	if (!is.na(limit)) {
		mask <- x < limit
		d <- d[mask,]
		x <- x[mask]
	}
	
	switch (n,
		lm(d$time ~ x, na.action=na.exclude),
		lm(d$time ~ x + I(x^2), na.action=na.exclude),
		lm(d$time ~ x + I(x^2) + I(x^3), na.action=na.exclude),
		lm(d$time ~ x + I(x^2) + I(x^3) + I(x^4), na.action=na.exclude),
		lm(d$time ~ x + I(x^2) + I(x^3) + I(x^4) + I(x^5), na.action=na.exclude))
}


#
# Function with.khop.polynomial.fit
#
# Description:
#   Apply a polynomial model
#
khop.polynomial.fit <- function(khop.data, model, x=NULL) {
	
	d <- khop.data
	if (is.null(x)) {
		x <- d$unique.vertices
	}

	d$time.fit <- 0
	
	for (c in rev(model$coefficients)) {
		d$time.fit <- (d$time.fit * x) + c
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
with.khop.polynomial.fit <- function(khop.data, x=NULL, ...) {
	
	khop.polynomial.fit(khop.data, khop.polynomial.model(khop.data, x=x, ...), x=x)
}



################################################################################
##                                                                            ##
##  Plotting                                                                  ##
##                                                                            ##
################################################################################


#
# Colors for k in k-hop plots
#

khop.colors      <- c("violet", "blue", "green", "orange", "red")
khop.dark.colors <- c("darkviolet", "darkblue", "darkgreen", "darkorange", "darkred")


#
# Function plot.khops
#
# Description:
#   Plot a khops plot
#
# Arguments:
#   khop.data       the actual data
#   x               the X values, must be a column or a derivation of a column of khop.data
#   y               the Y values, must ve a column or a derivation of a column of khop.data
#   xlab            the label of the X axis
#   ylab            the label of the Y axis
#   log             which axes should be log-scale (valid values are "", "x", "y", and "xy")
#   xmin            the minimum X value
#   ymin            the minimum Y value
#   kmin            the minimum K value
#   xmax            the maximum X value
#   ymax            the maximum Y value
#   kmax            the maximum K value
#   hold            FALSE to create a new plot, TRUE to add to the current plot
#   legend          whether to draw a legend with the different values of K
#   dark            whether to use dark colors instead of the default colors
#   real.hops       FALSE to color points by k, TRUE to color them by real.hops
#
plot.khops <- function(khop.data, x, y=NULL, xlab=NA, ylab=NA, log="", xmin=NA, ymin=NA, kmin=NA,
                       xmax=NA, ymax=NA, kmax=NA, hold=FALSE, legend=TRUE, dark=FALSE, real.hops=FALSE) {
	
	
	# Get the X and Y vectors
	
	if (is.null(y)) {
		y <- khop.data$time
		ylab <- "Time (ms)"
	}
	
	if (length(x) == 1 && is.character(x)) {
		if (is.na(xlab)) { xlab = x; }
		x <- khop.data[[x]]
	}
	
	if (length(y) == 1 && is.character(y)) {
		if (is.na(ylab)) { ylab = y; }
		y <- khop.data[[y]]
	}

	
	# Filter the data
	
	data <- khop.data
	mask <- !is.nan(x) & !is.nan(y)
	if (!is.na(xmax)) { mask <- mask & (x <= xmax) }
	if (!is.na(ymax)) { mask <- mask & (y <= ymax) }
	if (!is.na(kmax)) { mask <- mask & (data$k <= kmax) }
	if (!is.na(xmin)) { mask <- mask & (x >= xmin) }
	if (!is.na(ymin)) { mask <- mask & (y >= ymin) }
	if (!is.na(kmin)) { mask <- mask & (data$k >= kmin) }
	
	if (real.hops) {
		data.k <- data$real.hops
	}
	else {
		data.k <- data$k
	}
	k.values <- unique(data.k[mask])
	
	
	# Get the colors
	
	colors <- khop.colors
	if (dark) {
		colors <- khop.dark.colors
	}
	
	
	# Create a new plot
	
	if (!hold) {
	
		plot(x[mask], y[mask], log=log, xlab=xlab, ylab=ylab)
		
		
		# Legend
		
		if (legend) {
		
			legend("topleft",
				legend=paste("k =", as.character(k.values)),
				inset=0.01, pch=c(1),
				col=colors[k.values])
		}
	
	
		# Title
		
		unique.databse.names <- unique(khop.data[mask,]$database.name)
		unique.databse.instances <- unique(khop.data[mask,]$database.instance)
		
		if (length(unique.databse.names) == 1) {
			unique.databse.names.string = paste("Database", paste(unique.databse.names, collapse=", "))
		}
		else {
			unique.databse.names.string = paste("Databases", paste(unique.databse.names, collapse=", "))
		}
		
		if (length(unique.databse.instances) == 1) {
			unique.databse.instances.string = paste("Instance", paste(unique.databse.instances, collapse=", "))
		}
		else {
			unique.databse.instances.string = paste("Instances", paste(unique.databse.instances, collapse=", "))
		}
		
		title("GetKHopNeighbors")
		
		if (length(unique.databse.names) == 1) {
			mtext(paste(unique.databse.names.string, ", ", unique.databse.instances.string, sep=""))
		}
		else {
			mtext(paste(unique.databse.names.string, "; ", unique.databse.instances.string, sep=""))
		}
	}
	
	
	# Plot
	
	for (k in sort(k.values, decreasing=TRUE)) {
		m <- mask & (data.k == k)
		points(x[m], y[m], col=colors[k])
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
