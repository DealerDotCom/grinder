
(jde-set-project-name "Grinder tests")
(jde-set-variables 
 '(jde-global-classpath (quote (".:../build/classes:/opt/junit/junit3.5/junit.jar:/opt/jdk1.3/jre/lib/rt.jar")))

 '(jde-run-option-classpath (quote ("../build/tests-classes:../build/classes:/opt/junit/junit3.5/junit.jar")))

 '(jde-db-option-classpath (quote ("../build/tests-classes:../build/classes:/opt/junit/junit3.5/junit.jar")))

 '(jde-compile-option-directory "../build/tests-classes")
)
