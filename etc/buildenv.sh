export ANT_HOME=/opt/ant/ant1.4.1
export JAVA_HOME=/opt/jdk1.3.1_02
export PATH=$PATH:${ANT_HOME}/bin

J2EE=/opt/bea/wlserver6.1/lib/weblogic.jar
JAKARTA_REGEXP=/opt/jakarta-regexp/jakarta-regexp-1.2/jakarta-regexp-1.2.jar
JTIDY=/opt/jtidy/jtidy-04aug2000r7-dev/build/Tidy.jar
JUNIT=/opt/junit/junit3.5/junit.jar
XALAN=/opt/xalan/xalan-j_2_3_1/bin/xalan.jar:/opt/xalan/xalan-j_2_3_1/bin/xml-apis.jar

export CLASSPATH=$(cygpath -w -p "${JAKARTA_REGEXP}:${JUNIT}:${J2EE}:${JTIDY}:${XALAN}")
