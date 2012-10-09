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
# Function find.benchmark.results.directory
#
# Description:
#   Finds the name of the result directory for the given database engine name,
#   and database instance name
#
find.benchmark.results.directory <- function(database.name, database.instance) {


	# Initialize

	if (is.null(database.instance)) {
		subdir <- database.name
	}
	else {
		subdir <- paste(database.name, "_", database.instance, sep="")
	}

	
	# Find the data directory
	
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
	
	dir
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
find.benchmark.results.file <- function(database.name, database.instance, workload.argument, specialized=NULL) {

	
	# Find the data directory
	
	dir <- find.benchmark.results.directory(database.name, database.instance)
	
	
	# List the files
	
	if (is.null(database.instance)) {
		files <- list.files(dir, paste("^", database.name,
					"__.*", "__", workload.argument, "__.*\\.csv", sep=""))
	}
	else {
		files <- list.files(dir, paste("^", database.name, "_", database.instance,
					"__.*", "__", workload.argument, "__.*\\.csv", sep=""))
	}
	
	
	# Filters
	
	if (!is.null(specialized)) {
	}
	
	
	# Get the latest file
	
	### Do it by time, not file name
	
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
load.benchmark.results <- function(database.name, database.instance, workload.argument,... ) {

	data <- read.csv(find.benchmark.results.file(database.name, database.instance, workload.argument,... ))
	
	
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
load.benchmark.results.khop <- function(database.name, database.instance, keep.outliers=FALSE, directed=TRUE,... ) {

	data <- load.benchmark.results(database.name, database.instance, "get-k",... )

	
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
load.benchmark.results.khop.undirected <- function(database.name, database.instance, keep.outliers=FALSE,... ) {

	load.benchmark.results.khop(database.name, database.instance, keep.outliers=keep.outliers, directed=FALSE,... )
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
load.benchmark.results.global.clustering.coefficient <- function(database.name, database.instance,... ) {

	data <- load.benchmark.results(database.name, database.instance, "clustering-coeff",... )

	
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
load.benchmark.results.network.average.clustering.coefficient <- function(database.name, database.instance,... ) {

	data <- load.benchmark.results(database.name, database.instance, "clustering-coeff",... )

	
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
load.benchmark.results.all.neighbors <- function(database.name, database.instance, keep.outliers=FALSE,... ) {

	data <- load.benchmark.results(database.name, database.instance, "get",... )


	# Isolate and parse OperationGetAllNeighbors

	data              <- data[data$name == "OperationGetAllNeighbors", ]
	data$unique.vertices <- as.numeric(as.character(data$result))
	
	if (!keep.outliers) {
		data <- data[!outlier(data$time, logical=TRUE), ]
	}

	data
}


#
# Function load.benchmark.results.shortest.path
#
# Description:
#   Load GraphDB benchmark results for OperationGetShortestPath or OperationGetShortestPathProperty
#
# Usage:
#   load.benchmark.results.shortest.path(database.name, database.instance)
#
load.benchmark.results.shortest.path <- function(database.name, database.instance, keep.outliers=FALSE, property=FALSE,... ) {
	
	# Isolate and parse OperationGetShortestPath or OperationGetShortestPathProperty
	
	if (property) {
		data <- load.benchmark.results(database.name, database.instance, "shortest-path-prop",... )
		data <- data[data$name == "OperationGetShortestPathProperty", ]
	}
	else {
		data <- load.benchmark.results(database.name, database.instance, "shortest-path",... )
		data <- data[data$name == "OperationGetShortestPath", ]
	}
	data$directed <- FALSE
	data$property <- property
	
	
	# Parse the statistics
	
	data$result            <- lapply(strsplit(as.character(data$result), ":"), as.numeric)
	data$result.size       <- as.numeric(lapply(data$result, function(x) x[1]))
	data$get.vertices      <- as.numeric(lapply(data$result, function(x) x[2]))
	data$get.vertices.next <- as.numeric(lapply(data$result, function(x) x[3]))
	
	data$path.length             <- data$result.size
	data$retrieved.vertices      <- data$get.vertices.next
	data$retrieved.neighborhoods <- data$get.vertices
	
	if (property) {
		# TODO
	}
	else {
		if (length(data$result[0]) > 3) {
			data$unique.vertices <- as.numeric(lapply(data$result, function(x) x[4]))
		}
	}
	
	
	# Remove outliers
	
	if (!keep.outliers) {
		outliers <- outlier(data$time)
		data <- data[!(data$time %in% outliers), ]
	}
	
	data
}


#
# Function load.benchmark.results.sssp
#
# Description:
#   Load GraphDB benchmark results for OperationGetSingleSourceShortestPath or OperationGetSingleSourceShortestPathProperty
#
# Usage:
#   load.benchmark.results.sssp(database.name, database.instance)
#
load.benchmark.results.sssp <- function(database.name, database.instance, keep.outliers=FALSE, property=FALSE,... ) {
	
	# Isolate and parse OperationGetShortestPath or OperationGetShortestPathProperty
	
	if (property) {
		data <- load.benchmark.results(database.name, database.instance, "sssp-prop",... )
		data <- data[data$name == "OperationGetSingleSourceShortestPathProperty", ]
	}
	else {
		data <- load.benchmark.results(database.name, database.instance, "sssp",... )
		data <- data[data$name == "OperationGetSingleSourceShortestPath", ]
	}
	data$directed <- FALSE
	data$property <- property
	
	
	# Parse the statistics
	
	data$result            <- lapply(strsplit(as.character(data$result), ":"), as.numeric)
	data$result.size       <- as.numeric(lapply(data$result, function(x) x[1]))
	data$get.vertices      <- as.numeric(lapply(data$result, function(x) x[2]))
	data$get.vertices.next <- as.numeric(lapply(data$result, function(x) x[3]))
	
	data$retrieved.vertices      <- data$get.vertices.next
	data$retrieved.neighborhoods <- data$get.vertices
	
	if (property) {
		# TODO
	}
	
	
	# Remove outliers
	
	if (!keep.outliers) {
		outliers <- outlier(data$time)
		data <- data[!(data$time %in% outliers), ]
	}
	
	data
}



################################################################################
##                                                                            ##
##  Models                                                                    ##
##                                                                            ##
################################################################################


#
# Function polynomial.model
#
# Description:
#   Create a polynomial model
#
# Usage:
#   polynomial.model(data, data$unique.vertices)
#
polynomial.model <- function(data, x, n=1, xmin=NA, xmax=NA, k=NA) {
	
	d <- data
	if (length(x) == 1 && is.character(x)) {
		x <- d[[x]]
	}
	
	if (!is.na(xmax)) {
		mask <- x <= xmax
		d <- d[mask,]
		x <- x[mask]
	}
	if (!is.na(xmin)) {
		mask <- x >= xmin
		d <- d[mask,]
		x <- x[mask]
	}
	
	if (!is.na(k)) {	# For k-hop operations
		mask <- d$k == k
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
apply.polynomial.model <- function(data, x, model) {
	
	d <- data
	if (length(x) == 1 && is.character(x)) {
		x <- d[[x]]
	}

	d$time.fit <- 0
	
	for (c in rev(model$coefficients)) {
		d$time.fit <- (d$time.fit * x) + c
	}
	
	d
}


#
# Function compare.polynomial.models
#
# Description:
#   Compare two polynomial models
#
compare.polynomial.models <- function(data, x, n1=1, n2=2, n3=NA, n4=NA, ...) {
	
	nm <- 1
	m1 <- polynomial.model(data, x, n=n1, ...)
	
	if (!is.na(n2)) {
		nm <- 2
		m2 <- polynomial.model(data, x, n=n2, ...)
	}
	if (!is.na(n3) && nm == 2) {
		nm <- 3
		m3 <- polynomial.model(data, x, n=n3, ...)
	}
	
	if (!is.na(n4) && nm == 3) {
		nm <- 4
		m4 <- polynomial.model(data, x, n=n4, ...)
	}
	
	switch (nm,
		anova(m1),
		anova(m1, m2),
		anova(m1, m2, m3),
		anova(m1, m2, m3, m4))
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

	if (is.null(x)) {
		x <- khop.data$unique.vertices
	}	
	apply.polynomial.model(khop.data, x, polynomial.model(khop.data, x, ...))
}



################################################################################
##                                                                            ##
##  Plotting                                                                  ##
##                                                                            ##
################################################################################


#
# Colors for k in k-hop plots
#

colors.rainbow5 <- c("violet", "blue", "green", "orange", "red")


#
# Function benchmark.results.generate.plot.default.subtitle
#
# Description:
#   Generate a default plot subtitle
#
benchmark.results.generate.plot.default.subtitle <- function(data) {
	unique.databse.names <- unique(data$database.name)
	unique.databse.instances <- unique(data$database.instance)
	
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
	
	if (length(unique.databse.names) == 1) {
		paste(unique.databse.names.string, ", ", unique.databse.instances.string, sep="")
	}
	else {
		paste(unique.databse.names.string, "; ", unique.databse.instances.string, sep="")
	}
}


#
# Function plot.results
#
# Description:
#   Plot the results
#
# Arguments:
#   data                the actual data
#   x                   the X values, must be a column or a derivation of a column of khop.data
#   y                   the Y values, must ve a column or a derivation of a column of khop.data
#   xlab                the label of the X axis
#   ylab                the label of the Y axis
#   log                 which axes should be log-scale (valid values are "", "x", "y", and "xy")
#   xmin                the minimum X value
#   ymin                the minimum Y value
#   xmax                the maximum X value
#   ymax                the maximum Y value
#   hold                FALSE to create a new plot, TRUE to add to the current plot
#   legend              whether to draw a legend with the different category values
#   legend.prefix       prefix for the categories in the legend
#   category.colors     set the vector of colors to use
#   category.column     the column name that specifies the category
#   category.stable     TRUE if the categories are small integers and their color associations need to be stable
#   order.asc           TRUE to draw the categories in the ascending order
#   type                the plot type ("p" for points, "l" for lines, etc.)
#   pch                 the points type
#   plot.title          the plot title
#   plot.subtitle       the plot subtitle
#
plot.results <- function(data, x, y=NULL, xlab=NA, ylab=NA, log="", xmin=NA, ymin=NA,
                         xmax=NA, ymax=NA,hold=FALSE, legend=TRUE, legend.prefix="", category.colors=NULL,
                         category.column=NULL, category.stable=FALSE, order.asc=TRUE,
                         type="p", pch=1, plot.title=NULL, plot.subtitle=NULL) {
	
	
	# Get the X and Y vectors
	
	if (is.null(y)) {
		y <- data$time
		ylab <- "Time (ms)"
	}
	
	if (length(x) == 1 && is.character(x)) {
		if (is.na(xlab)) { xlab = x; }
		x <- data[[x]]
	}
	
	if (length(y) == 1 && is.character(y)) {
		if (is.na(ylab)) { ylab = y; }
		y <- data[[y]]
	}

	
	# Filter the data
	
	mask <- !is.nan(x) & !is.nan(y)
	if (!is.na(xmax)) { mask <- mask & (x <= xmax) }
	if (!is.na(ymax)) { mask <- mask & (y <= ymax) }
	if (!is.na(xmin)) { mask <- mask & (x >= xmin) }
	if (!is.na(ymin)) { mask <- mask & (y >= ymin) }
	
	
	# Categories
	
	if (is.null(category.column)) {
		categories <- c(NULL)
	}
	else {
		categories <- sort(unique(data[mask,][[category.column]]))
	}
	
	
	# Get the colors
	
	colors <- category.colors
	if (is.null(colors)) {
		if (is.null(category.column)) {
			colors <- "black"
		}
		else {
			colors <- colors.rainbow5
		}
	}
	
	stopifnot(length(colors) >= 1)
	
	if (!is.null(category.column)) {
		if (category.stable) {
			need.colors <- max(categories)
		}
		else {
			need.colors <- length(categories)
		}
		while (need.colors > length(colors)) {
			colors <- c(colors, colors)
		}
	}

	
	# Create a new plot
	
	if (!hold) {
	
		plot(x[mask], y[mask], log=log, xlab=xlab, ylab=ylab, type=type, pch=pch)
		
		
		# Legend
		
		if (legend && !is.null(category.column)) {
			if (category.stable) {
				legend.category.colors <- colors[categories]
			}
			else {
				legend.category.colors <- colors
			}
			legend("topleft",
				legend=paste(legend.prefix, as.character(categories), sep=""),
				inset=0.01, pch=pch,
				col=legend.category.colors)
		}
	
	
		# Title
		
		if (is.null(plot.title)) {
			unique.names <- sort(unique(sub("-.*$", "", sub("^Operation", "", unique(data[mask,]$name)))))
			plot.title <- paste(unique.names, collapse=", ")
		}
		
		title(plot.title)
		if (!is.null(plot.subtitle)) {
			mtext(plot.subtitle)
		}
		else {
			mtext(benchmark.results.generate.plot.default.subtitle(data[mask,]))
		}
	}
	
	
	# Plot
	
	categories.in.drawing.order <- categories
	if (!is.null(category.column)) {
		categories.in.drawing.order <- sort(categories, decreasing=!order.asc)
	}
	
	for (category in categories.in.drawing.order) {
		if (is.null(category.column)) {
			m <- mask
			category.color <- colors[1]
		}
		else {
			m <- mask & (data[[category.column]] == category)
			if (category.stable) {
				category.color <- colors[category]
			}
			else {
				category.color <- colors[which(categories == category)]
			}
		}
		points(x[m], y[m], col=category.color, type=type, pch=pch)
	}
}


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
#   kmin            the minimum K value
#   kmax            the maximum K value
#   k               set the K value
#   real.hops       FALSE to color points by k, TRUE to color them by real.hops
#   colors          set the vector of colors to use for the various values of K
#   order.k.asc     TRUE to draw the various datasets by the increasing value of K
#   ...             arguments to plot.results
#
plot.khops <- function(khop.data, x, y=NULL, kmin=NA, kmax=NA, k=NULL, real.hops=FALSE,
                       colors=NULL, order.k.asc=FALSE, ...) {
	
	# Filter the data
	
	data <- khop.data
	
	if (real.hops) {
		data.k <- data$real.hops
		k.column <- "real.hops"
	}
	else {
		data.k <- data$k
		k.column <- "k"
	}
	
	mask <- !is.nan(x) & !is.nan(y)
	if (!is.null( k)) { mask <- mask & (data$k %in% k); }
	if (!is.na(kmax)) { mask <- mask & (data$k <= kmax) }
	if (!is.na(kmin)) { mask <- mask & (data$k >= kmin) }
	
	
	# Plot
	
	plot.results(data, x, y=y, legend.prefix="k = ",
	             category.colors=colors, category.stable=TRUE, category.column=k.column,
	             order.asc=order.k.asc, ...)
}


#
# Function plot.khops.time.vs.unique.vertices
#
# Description:
#   Plot time vs. unique vertices for khops
#
plot.khops.time.vs.unique.vertices <- function(khop.data, ...) {

	plot.khops(khop.data, khop.data$unique.vertices, xlab="Unique Vertices", ...)
}


#
# Function plot.khops.time.vs.retrieved.vertices
#
# Description:
#   Plot time vs. retrieved vertices for khops
#
plot.khops.time.vs.retrieved.vertices <- function(khop.data, ...) {
	
	plot.khops(khop.data, khop.data$get.vertices.next, xlab="Retrieved Vertices", ...)
}


#
# Function plot.khops.time.vs.retrieved.neighborhoods
#
# Description:
#   Plot time vs. retrieved neighborhoods for khops
#
plot.khops.time.vs.retrieved.neighborhoods <- function(khop.data, ...) {
	
	plot.khops(khop.data, khop.data$get.vertices, xlab="Retrieved Neighborhoods", ...)
}
