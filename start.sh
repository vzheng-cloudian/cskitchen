#!/usr/bin/env /bin/bash


# Use JAVA_HOME if set, otherwise look for java in PATH
if [ -x $JAVA_HOME/bin/java ]; then
    JAVA=$JAVA_HOME/bin/java
else
    JAVA=`which java`
fi

pathsep=":"
function append_jars_onto_classpath() {
    local JARS
    JARS=`find $1/*.jar 2> /dev/null || true`
    for i in $JARS; do
        if [ -n "$CLASSPATH" ]; then
            CLASSPATH="$CLASSPATH${pathsep}${i}";
        else
            CLASSPATH=${i}
        fi
    done
}

function build_classpath() {
  # generic path... Here
  BINPATH=`dirname $0`

  CLASSPATH="./";
  append_jars_onto_classpath "./"
  export CLASSPATH
}
#set -x
build_classpath

$JAVA -Xmx2g -Xmn40m -Dlog4j.configurationFile=log4j2.xml com.css.cloudkitchen.CSKitchen $@
