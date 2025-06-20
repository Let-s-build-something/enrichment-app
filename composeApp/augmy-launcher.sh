#!/bin/bash

# This file adds support to deep-linking for debug Linux JVM runs
# As of now, the app has to be restarted in order to open the deep-link, however, all of that is automatic with terminal logging

export JAVA_HOME=/snap/android-studio/187/jbr
pkill -f "gradlew :composeApp:run" || echo "No Gradle process to kill"
pkill -f "java.*MainKt" || echo "No MainKt process to kill"
pkill -f "java.*augmy" || echo "No Java process to kill"

sleep 1

cd /home/jacob/StudioProjects/enrichment-app

# Run the Gradle task with the URI as an argument (if provided)
./gradlew :composeApp:run --args="$1"
echo "Script finished. Press Enter to close the terminal..."
read -r