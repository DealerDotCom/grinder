export ANT_HOME=/opt/ant/jakarta-ant-1.5.1
export JAVA_HOME=/opt/jdk1.3.1_02/
export PATH=${ANT_HOME}/bin:${JAVA_HOME}/bin:$PATH

CHECKSTYLE=/opt/checkstyle/checkstyle-3.2/checkstyle-all-3.2.jar
JAKARTA_REGEXP=/opt/jakarta-regexp/jakarta-regexp-1.2/jakarta-regexp-1.2.jar
JSSE_HOME=/opt/jsse/jsse1.0.2/lib/
JSSE=${JSSE_HOME}/jsse.jar:${JSSE_HOME}/jnet.jar:${JSSE_HOME}/jcert.jar
JUNIT=/opt/junit/junit3.7/junit.jar

export CLASSPATH=$(cygpath -w -p "${JUNIT}:${CHECKSTYLE}:${JSSE}")
