export ANT_HOME=/opt/ant/ant1.3
export JAVA_HOME=/opt/jdk1.3
export PATH=$PATH:${ANT_HOME}/bin

JAKARTA_REGEXP=/opt/jakarta-regexp/jakarta-regexp-1.2/jakarta-regexp-1.2.jar
JUNIT=/opt/junit/junit3.5/junit.jar

export CLASSPATH=$(cygpath -w -p "${JAKARTA_REGEXP}:${JUNIT}")
