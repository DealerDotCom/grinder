GRINDER_HOME=/work/src/grinder3

#GRINDER=${GRINDER_HOME}/lib/grinder.jar
GRINDER=${GRINDER_HOME}/build/classes:${GRINDER_HOME}/lib/jython.jar
GRINDER=${GRINDER_HOME}/build/tests-classes:${GRINDER} # for testing JUnit plugin

JUNIT=/opt/junit/junit3.7/junit.jar
XALAN=/opt/xalan/xalan-j_2_3_1/bin/xalan.jar:/opt/xalan/xalan-j_2_3_1/bin/xml-apis.jar

WLS=/opt/bea/weblogic700/server/lib/weblogic.jar
WLS_EXAMPLES=/opt/bea/weblogic700/samples/server/stage/examples/clientclasses/ejb20_basic_statefulSession_client.jar

export CLASSPATH=$(cygpath -w -p "${GRINDER}:${JUNIT}:${JTIDY}:${WLS}:${WLS_EXAMPLES}")

alias grind="java net.grinder.Grinder"
alias console="java net.grinder.Console"
alias sniff="java net.grinder.TCPProxy"
