CYGWIN_ANT_HOME=/opt/ant/ant1.2
export ANT_HOME=$(cygpath -w ${CYGWIN_ANT_HOME})
export JAVA_HOME=e:\jdk1.3
export PATH=$PATH:${CYGWIN_ANT_HOME}/bin

export CLASSPATH=$(cygpath -w -p "/opt/jakarta-regexp/jakarta-regexp-1.2/jakarta-regexp-1.2.jar:/opt/junit/junit3.4/junit.jar")
