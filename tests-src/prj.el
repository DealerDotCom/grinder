(load-file "/work/src/grinder/src/prj.el")

(jde-set-project-name "The Grinder tests")
(jde-set-variables 
 '(jde-global-classpath
   (append '("../build/tests-classes"
	     "../build/classes")
	   jde-global-classpath))

 '(jde-run-option-classpath jde-global-classpath)

 '(jde-compile-option-directory "../build/tests-classes")
)
