export ANT_HOME=/opt/ant/ant1.3
export JAVA_HOME=/opt/jdk1.3
export PATH=$PATH:${ANT_HOME}/bin

BSF=/opt/bsf/bsf-2_2-jython/build/lib/bsf.jar
J2EE=/opt/bea/wlserver6.1/lib/weblogic.jar
JAKARTA_REGEXP=/opt/jakarta-regexp/jakarta-regexp-1.2/jakarta-regexp-1.2.jar
JUNIT=/opt/junit/junit3.5/junit.jar

export CLASSPATH=$(cygpath -w -p "${JAKARTA_REGEXP}:${JUNIT}:${J2EE}:${BSF}")
