GRINDER_HOME=/work/src/grinder

#GRINDER=${GRINDER_HOME}/lib/grinder.jar
GRINDER=${GRINDER_HOME}/build/classes
GRINDER=${GRINDER}:${GRINDER_HOME}/build/tests-classes # for testing JUnit plugin

JAKARTA_ORO=/opt/jakarta-oro/jakarta-oro-2.0.6/jakarta-oro-2.0.6.jar
JUNIT=/opt/junit/junit3.7/junit.jar

JTIDY=/opt/jtidy/jtidy-04aug2000r7-dev/build/Tidy.jar
XALAN=/opt/xalan/xalan-j_2_3_1/bin/xalan.jar:/opt/xalan/xalan-j_2_3_1/bin/xml-apis.jar

export CLASSPATH=$(cygpath -w -p "${GRINDER}:${JAKARTA_ORO}ht:${JAKARTA_REGEXP}:${JUNIT}:${JTIDY}:${XALAN}")

alias grind="java net.grinder.Grinder"
alias console="java net.grinder.Console"
alias sniff="java net.grinder.TCPSniffer"
