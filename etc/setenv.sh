GRINDER_HOME=/work/src/grinder3

#GRINDER=${GRINDER_HOME}/lib/grinder.jar
GRINDER=${GRINDER_HOME}/build/classes
GRINDER=${GRINDER}:${GRINDER_HOME}/build/tests-classes # for testing JUnit plugin

BSF=/opt/bsf/bsf-2_2-jython/build/lib/bsf.jar
JAKARTA_REGEXP=/opt/jakarta-regexp/jakarta-regexp-1.2/jakarta-regexp-1.2.jar
JYTHON=/opt/jython/jython-2.0/jython.jar
JUNIT=/opt/junit/junit3.5/junit.jar

export CLASSPATH=$(cygpath -w -p "${GRINDER}:${JAKARTA_REGEXP}:${JUNIT}:${BSF}:${JYTHON}")

alias grind="java net.grinder.Grinder"
alias console="java net.grinder.Console"
alias sniff="java net.grinder.TCPSniffer"
