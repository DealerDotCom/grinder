export ANT_HOME=/opt/ant/ant1.3
export JAVA_HOME=/opt/jdk1.3.1_02
export PATH=$PATH:${ANT_HOME}/bin

JAKARTA_REGEXP=/opt/jakarta-regexp/jakarta-regexp-1.2/jakarta-regexp-1.2.jar
JUNIT=/opt/junit/junit3.5/junit.jar
J2EE=/opt/bea/wlserver6.1/lib/weblogic.jar

export CLASSPATH=$(cygpath -w -p "${JAKARTA_REGEXP}:${JUNIT}:${J2EE}")
