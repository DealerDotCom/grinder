CYGWIN_ANT_HOME=/opt/ant/ant1.2
export ANT_HOME=$(cygpath -w ${CYGWIN_ANT_HOME})
export JAVA_HOME=e:\jdk1.3
export PATH=$PATH:${CYGWIN_ANT_HOME}/bin

unset CLASSPATH

. /work/src/scripts/wls/wls5.1-client.bash

addToWindowsClasspath CLASSPATH \
    /opt/jakarta-regexp/jakarta-regexp-1.2/jakarta-regexp-1.2.jar \
    ${WLS_HOME}/examples/ejb/basic/statelessSession/build/ejb_basic_statelessSession.jar
