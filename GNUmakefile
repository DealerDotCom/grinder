GRINDER_HOME   = /src/Grinder
WLS_SERVER     = //e/mywls/5.1/myserver
WLS_EJBDIR = ${WLS_SERVER}/ejb

CLASSES = $(GRINDER_HOME)/classes
PRJSRC = $(GRINDER_HOME)/java
#PRJDOC = $(GRINDER_HOME)/doc
PRJLIB = $(GRINDER_HOME)/lib
PRJJAR = $(PRJLIB)/grinder.jar

WLS_HOME = //e/wls5.1
WLS_SP = 6
WLS_SP_HOME = //e/wls5.1/sp$(WLS_SP)

CLASSPATH = \
$(CLASSES):$(WLS_SP_HOME)/weblogic510sp$(WLS_SP).jar:$(WLS_HOME)/classes:$(WLS_HOME)/lib/weblogicaux.jar://e/jdk1.3/jre/lib/rt.jar

# $(WLS_EJBDIR)/ejb_basic_statelessSession.jar:

JAR = jar
JAVAC = jikes
JAVADOC = javadoc

JAVACOPTIONS = -deprecation -classpath $(CLASSPATH) -d $(CLASSES)

all: library


UTIL_J	= \
	$(PRJSRC)/com/ejbgrinder/util/HttpMsg.java \
	$(PRJSRC)/com/ejbgrinder/util/Util.java

UTIL_C	= \
	$(CLASSES)/com/ejbgrinder/util/HttpMsg.class \
	$(CLASSES)/com/ejbgrinder/util/Util.class

util: $(UTIL_C)

$(UTIL_C): $(UTIL_J)
	@echo building util...
	$(JAVAC) $(JAVACOPTIONS) $(UTIL_J)		


GRINDER_C = \
	$(CLASSES)/com/ejbgrinder/grinder/Chronus.class \
	$(CLASSES)/com/ejbgrinder/grinder/StatInfo.class \
	$(CLASSES)/com/ejbgrinder/grinder/GrinderContext.class \
	$(CLASSES)/com/ejbgrinder/grinder/PropsLoader.class \
	$(CLASSES)/com/ejbgrinder/grinder/GrinderPlugin.class \
	$(CLASSES)/com/ejbgrinder/grinder/CycleThread.class \
	$(CLASSES)/com/ejbgrinder/grinder/GrinderProcess.class \
	$(CLASSES)/com/ejbgrinder/grinder/EvolGraph.class \
	$(CLASSES)/com/ejbgrinder/grinder/GraphStatInfo.class \
	$(CLASSES)/com/ejbgrinder/grinder/GrinderThread.class \
	$(CLASSES)/com/ejbgrinder/grinder/MsgReader.class \
	$(CLASSES)/com/ejbgrinder/grinder/Redirector.class \
	$(CLASSES)/com/ejbgrinder/Grinder.class \
	$(CLASSES)/com/ejbgrinder/Console.class

GRINDER_J = \
	$(PRJSRC)/com/ejbgrinder/grinder/Chronus.java \
	$(PRJSRC)/com/ejbgrinder/grinder/StatInfo.java \
	$(PRJSRC)/com/ejbgrinder/grinder/GrinderContext.java \
	$(PRJSRC)/com/ejbgrinder/grinder/PropsLoader.java \
	$(PRJSRC)/com/ejbgrinder/grinder/GrinderPlugin.java \
	$(PRJSRC)/com/ejbgrinder/grinder/CycleThread.java \
	$(PRJSRC)/com/ejbgrinder/grinder/GrinderProcess.java \
	$(PRJSRC)/com/ejbgrinder/grinder/EvolGraph.java \
	$(PRJSRC)/com/ejbgrinder/grinder/GraphStatInfo.java \
	$(PRJSRC)/com/ejbgrinder/grinder/GrinderThread.java \
	$(PRJSRC)/com/ejbgrinder/grinder/MsgReader.java \
	$(PRJSRC)/com/ejbgrinder/grinder/Redirector.java \
	$(PRJSRC)/com/ejbgrinder/Grinder.java \
	$(PRJSRC)/com/ejbgrinder/Console.java

grinder: util $(GRINDER_C)

$(GRINDER_C): $(GRINDER_J)
	@echo building grinder...
	$(JAVAC) $(JAVACOPTIONS) $(GRINDER_J)


PLUGIN_C = \
	$(CLASSES)/com/ejbgrinder/grinder/plugin/HttpBmk.class \
	$(CLASSES)/com/ejbgrinder/grinder/plugin/SimpleBmk.class \
#	$(CLASSES)/com/ejbgrinder/grinder/plugin/SimpleEJBBmk.class


PLUGIN_J = \
	$(PRJSRC)/com/ejbgrinder/grinder/plugin/HttpBmk.java \
	$(PRJSRC)/com/ejbgrinder/grinder/plugin/SimpleBmk.java \
#	$(PRJSRC)/com/ejbgrinder/grinder/plugin/SimpleEJBBmk.java

plugin: grinder $(PLUGIN_C)

$(PLUGIN_C): $(PLUGIN_J)
	@echo building plugin...
	$(JAVAC) $(JAVACOPTIONS) $(PLUGIN_J)

library: util grinder plugin $(PRJJAR)

$(PRJJAR): $(UTIL_C) $(GRINDER_C) $(PLUGIN_C)
	cd classes && \
	$(JAR) cvf0 $(PRJJAR) com 

#javadoc: 
#	$(JAVADOC) \
#	  -author \
#	  -version \
#	  -windowtitle "The Grinder Documentation" \
#	  -d $(PRJDOC) \
#	  -sourcepath $(PRJSRC) \
#	  -private \
#	  com.ejbgrinder com.ejbgrinder.grinder com.ejbgrinder.util com.ejbgrinder.grinder.plugin

#javadoc: 
#	c:\jdk1.3\bin\javadoc \
#	  -author \
#	  -version \
#	  -windowtitle "Professional J2EE Programming with BEA WebLogic Server" \
#	  -d c:\wlsbook\doc \
#	  -sourcepath $(GRINDER_HOME)\src\c03\java;$(GRINDER_HOME)\src\c04a\java;$(GRINDER_HOME)\src\c04b\java;$(GRINDER_HOME)\src\c05\java;$(GRINDER_HOME)\src\c06\java;$(GRINDER_HOME)\src\c07\java;$(GRINDER_HOME)\src\c08\java;$(GRINDER_HOME)\src\c09\java;$(GRINDER_HOME)\src\c10\java \
#	  -private \
#	  jsw.c03 jsw.c04a jsw.c04b jsw.c05 jsw.c06 jsw.c07 jsw.c07.fax jsw.c07.reservation jsw.c08 jsw.c08.fax jsw.c08.reservation jsw.c09 jsw.c09.fax jsw.c09.reservation com.ejbgrinder com.ejbgrinder.grinder com.ejbgrinder.util com.ejbgrinder.grinder.plugin

clean: 
	-find $(CLASSES) -name '*.class' -exec rm {} \;
	-rm -rf $(PRJJAR)

#deploy: deployJSP
#	copy $(GRINDER_HOME)\ejb\\*.jar $(GRINDER_HOME)\$(P_S)\ejb

#deployJSP:
#	@echo deploying JSPs...
#	xcopy /E $(GRINDER_HOME)\jsp\\*.jsp $(GRINDER_HOME)\$(P_S)\public_html	
