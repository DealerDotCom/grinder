#!/bin/sh

if [ "$JAVA_HOME" = "" ] ; then
  echo "ERROR: JAVA_HOME not found in your environment."
  echo
  echo "Please, set the JAVA_HOME variable in your environment to match the"
  echo "location of the Java Virtual Machine you want to use."
  exit 1
fi

ANT_HOME=./etc/ant

if [ -n "${CYGWIN}" ]
then
    ANT_HOME=`cygpath -w "${ANT_HOME}"`
fi

LOCALCLASSPATH="${ANT_HOME}/ant_1_1.jar;${ANT_HOME}/xerces_1_2.jar;${JAVA_HOME}/lib/tools.jar;$CLASSPATH"

${JAVA_HOME}/bin/java -Dant.home=$ANT_HOME -classpath $LOCALCLASSPATH org.apache.tools.ant.Main -buildfile etc/build.xml $*
