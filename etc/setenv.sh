. /work/src/scripts/wls/wls5.1-client.bash

GRINDER_HOME=/work/src/grinder

#GRINDER_JAR=${GRINDER_HOME}/lib/grinder.jar
GRINDER_JAR=${GRINDER_HOME}/build/classes

addToWindowsClasspath CLASSPATH \
    ${GRINDER_JAR} \
    /opt/jakarta-regexp/jakarta-regexp-1.2/jakarta-regexp-1.2.jar

alias grind="java net.grinder.Grinder"
alias console="java net.grinder.Console"
alias sniff="java net.grinder.TCPSniffer"
