; WHAT'S GOING ON, RELATIVE PATHS SEEM BROKEN FOR ALL BUT THE FIRST?
(jde-set-project-name "Grinder tests")
(jde-set-variables 
 '(jde-global-classpath (quote ("../build/classes:/work/src/grinder3/build/tests-classes:/opt/junit/junit3.5/junit.jar:/opt/jdk1.3/jre/lib/rt.jar")))

 '(jde-run-option-classpath (quote ("../build/classes:/work/src/grinder3/build/tests-classes:/opt/junit/junit3.5/junit.jar")))

 '(jde-db-option-classpath (quote ("../build/classes:/work/src/grinder3/build/tests-classes:/opt/junit/junit3.5/junit.jar")))
 '(jde-db-source-directories (quote ("." "../src")))

 '(jde-compile-option-directory "../build/tests-classes")
)
