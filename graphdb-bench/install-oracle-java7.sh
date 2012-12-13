#!/bin/sh
set -e

# For Ubuntu only

sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java7-installer
sudo update-java-alternatives -s java-7-oracle

