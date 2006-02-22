export ANT_HOME=/opt/ant/apache-ant-1.6.2
export JAVA_HOME=/opt/bea-815/jdk142_08
export PATH=${ANT_HOME}/bin:${JAVA_HOME}/bin:$PATH

JSSE_HOME=/opt/jsse/jsse1.0.2/lib/
JSSE=${JSSE_HOME}/jsse.jar:${JSSE_HOME}/jnet.jar:${JSSE_HOME}/jcert.jar
JUNIT=/opt/junit/junit3.8.1/junit.jar
# Clover home also needs to be set in localpaths.properties.
CLOVER=/opt/clover/clover-1.3.5/lib/clover.jar
XALAN=/opt/xalan/xalan-j_2_3_1/bin/xalan.jar:/opt/xalan/xalan-j_2_3_1/bin/xml-apis.jar

# Ant now handles cygpath -w if necessary.
export CLASSPATH="${JUNIT}:${CLOVER}:${JSSE}:${XALAN}"
