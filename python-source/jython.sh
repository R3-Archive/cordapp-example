#!/usr/bin/env bash

# Edit the below as appropriate.
# Download jython from http://www.jython.org/
# We find 2.7.0 works acceptably

# Either put jython in your path or change the variable below to the correct location
export JYTHON=/usr/local/bin/jython

if ! [ -x "$(command -v ${JYTHON})" ]; then
  echo "jython not on path or \$JYTHON variable not set to the correct location (please edit $0)"
  exit
fi

export JYTHON_LIB_DIR=build/jythonDeps

if ! [ -d $JYTHON_LIB_DIR ]; then
    echo "Please run './gradlew installJythonDeps' in the root folder of this project"
    exit
fi

# Is build/libs/* required?
export CLASSPATH=$JYTHON_LIB_DIR/*:build/libs/*

# Can I do this using $*?
${JYTHON} $*