(jde-set-project-name "The Grinder")

(jde-set-variables 

 '(jde-global-classpath
; For paths we don't want JDE to calculate, its much quicker to
; provide a single string in the classpath list; otherwise JDE churns
; disk for each of them. Relative paths should be individual elements.
   (list (mapconcat
     'identity
     '("/opt/xalan/xalan-j_2_3_1/bin/xalan.jar"
       "/opt/xalan/xalan-j_2_3_1/bin/xml-apis.jar"
       "/opt/jtidy/jtidy-04aug2000r7-dev/build/Tidy.jar"
       "/work/src/grinder/src"
       "/opt/wls5.1/lib/weblogicaux.jar"
       "/opt/jakarta-oro/jakarta-oro-2.0.6/jakarta-oro-2.0.6.jar"
       "/opt/junit/junit3.7/junit.jar"
       "/opt/jdk1.3.1_02/jre/lib/rt.jar"
       "/opt/jdk1.3.1_02/jre/lib/ext/jcert.jar"
       "/opt/jdk1.3.1_02/jre/lib/ext/jnet.jar"
       "/opt/jdk1.3.1_02/jre/lib/ext/jsse.jar")
     ":")))

 '(jde-run-option-classpath (cons "../build/classes" jde-global-classpath));

 '(jde-db-option-classpath (cons "../build/classes" jde-global-classpath));

 '(jde-db-source-directories '("/work/src/grinder/src"))

 '(jde-compile-option-directory "../build/classes")
)
