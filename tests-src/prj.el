(load-file "/work/src/grinder3/src/prj.el")

(jde-set-project-name "The Grinder tests")
(jde-set-variables 
 '(jde-global-classpath
   (append '("../build/tests-classes"
	     "../build/classes")
	   jde-global-classpath))

 '(jde-run-option-classpath jde-global-classpath)

 '(jde-db-source-directories (quote ("../src")))

 '(jde-compile-option-directory "../build/tests-classes")
)

(setq indent-tabs-mode nil)

; Redo this as JDE appears to override things set up in c-mode-hook.
(my-c-mode-hook-stuff)
