GRINDER_HOME=/work/src/grinder3

#GRINDER=${GRINDER_HOME}/lib/grinder.jar
GRINDER=${GRINDER_HOME}/build/classes:${GRINDER_HOME}/lib/jakarta-oro-2.0.6.jar:${GRINDER_HOME}/lib/jython.jar
GRINDER=${GRINDER_HOME}/build/tests-classes:${GRINDER} # for testing JUnit plugin

JUNIT=/opt/junit/junit3.7/junit.jar
XALAN=/opt/xalan/xalan-j_2_3_1/bin/xalan.jar:/opt/xalan/xalan-j_2_3_1/bin/xml-apis.jar

BEA_HOME=/system/bea-7.0.2.0
WLS=${BEA_HOME}/weblogic700/server/lib/weblogic.jar
#WLS=/opt/bea/weblogic700/server/lib/webserviceclient.jar
WLS_EXAMPLES_DIR=${BEA_HOME}/weblogic700/samples/server/stage/examples/clientclasses/
WLS_EXAMPLES=${WLS_EXAMPLES_DIR}/ejb20_basic_statefulSession_client.jar:${WLS_EXAMPLES_DIR}/HelloWorld_client.jar

JAVA_HOME=/system/bea-8.1b/jdk141_02/
#JAVA_HOME=/opt/Program\ Files/JRockit/7.0/1.3.1/
export PATH=${ANT_HOME}/bin:${JAVA_HOME}/bin:$PATH

export CLASSPATH=$(cygpath -w -p "${GRINDER}:${JUNIT}:${WLS}:${WLS_EXAMPLES}")

alias grind="java net.grinder.Grinder"
alias console="java net.grinder.Console"
alias sniff="java net.grinder.TCPProxy"

# My amazon license D1JM5WG5VP8KO1
