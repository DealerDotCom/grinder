export ANT_HOME=/opt/ant/ant1.4.1
export JAVA_HOME=/opt/jdk1.3.1_02
export PATH=$PATH:${ANT_HOME}/bin

JUNIT=/opt/junit/junit3.7/junit.jar

export CLASSPATH=$(cygpath -w -p "${JUNIT}")
