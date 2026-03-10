#!/bin/bash
# Get the directory where this script is located
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Navigate to that directory
cd "$DIR"

# Execute the maven command using the downloaded maven instance
./apache-maven-3.9.9/bin/mvn compile exec:java -Dexec.cleanupDaemonThreads=false
