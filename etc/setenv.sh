GRINDER_HOME=/work/src/grinder3

#GRINDER=${GRINDER_HOME}/lib/grinder.jar
GRINDER=${GRINDER_HOME}/build/classes:${GRINDER_HOME}/lib/jakarta-oro-2.0.6.jar
GRINDER=${GRINDER_HOME}/build/tests-classes:${GRINDER} # for testing JUnit plugin

BSF=/opt/bsf/bsf-2_2-jython/build/lib/bsf.jar
JYTHON=/opt/jython/jython-2.0/jython.jar
JUNIT=/opt/junit/junit3.7/junit.jar
JTIDY=/opt/jtidy/jtidy-04aug2000r7-dev/build/Tidy.jar
XALAN=/opt/xalan/xalan-j_2_3_1/bin/xalan.jar:/opt/xalan/xalan-j_2_3_1/bin/xml-apis.jar

export CLASSPATH=$(cygpath -w -p "${GRINDER}:${JUNIT}:${JTIDY}${BSF}:${JYTHON}")

alias grind="java net.grinder.Grinder"
alias console="java net.grinder.Console"
alias sniff="java net.grinder.TCPSniffer"
