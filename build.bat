@echo off

if "%JAVA_HOME%" == "" goto error

set ANT_HOME=.\etc\ant

set LOCALCLASSPATH=%ANT_HOME%\ant_1_1.jar;%ANT_HOME%\xerces_1_2.jar;%JAVA_HOME%\lib\tools.jar

%JAVA_HOME%\bin\java.exe -Dant.home="%ANT_HOME%" -classpath "%LOCALCLASSPATH%" org.apache.tools.ant.Main -buildfile etc/build.xml %1 %2 %3 %4 %5

goto end

:error

echo "ERROR: JAVA_HOME not found in your environment."
echo.
echo "Please, set the JAVA_HOME variable in your environment to match the"
echo "location of the Java Virtual Machine you want to use."

:end

set LOCALCLASSPATH=
set ANT_HOME=
