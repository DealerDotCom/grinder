export ANT_HOME=/opt/ant/jakarta-ant-1.5.1
export JAVA_HOME=/opt/j2sdk1.4.1
export PATH=${ANT_HOME}/bin:${JAVA_HOME}/bin:$PATH

JAKARTA_REGEXP=/opt/jakarta-regexp/jakarta-regexp-1.2/jakarta-regexp-1.2.jar
JUNIT=/opt/junit/junit3.7/junit.jar

export CLASSPATH=$(cygpath -w -p "${JUNIT}")
