. /src/scripts/wls/wls5.1-client.bash

GRINDER_HOME=/src/grinder

#GRINDER_JAR=${GRINDER_HOME}/lib/grinder.jar
GRINDER_JAR=${GRINDER_HOME}/build/classes

addToWindowsClasspath CLASSPATH \
    ${GRINDER_JAR} \
    .

alias grind="java net.grinder.Grinder"
alias console="java net.grinder.console.Console"
