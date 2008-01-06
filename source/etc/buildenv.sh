export ANT_HOME=/opt/ant/apache-ant-1.6.2
export JAVA_HOME=/opt/bea-816/jdk142_11
#export JAVAHL_HOME=/opt/eclipse/3.2/eclipse/plugins/org.tigris.subversion.javahl.win32_1.0.4/
export PATH=${ANT_HOME}/bin:${JAVA_HOME}/bin:$PATH

JUNIT=/opt/junit/junit3.8.1/junit.jar
# Clover home also needs to be set in localpaths.properties.
CLOVER=/opt/clover/clover-ant-2.0.3/lib/clover.jar
XALAN=/opt/xalan/xalan-j_2_3_1/bin/xalan.jar:/opt/xalan/xalan-j_2_3_1/bin/xml-apis.jar

export CLASSPATH="${JUNIT}:${CLOVER}:${XALAN}"
