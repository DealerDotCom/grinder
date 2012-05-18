(defproject net.sf.grinder/grinder-console-rest "0.1.0-SNAPSHOT"
  :description "REST API to The Grinder console."
  :url "http://grinder.sourceforge.net"
  :license {:name "The Grinder License (modified BSD)"
            :url "http://grinder.sourceforge.net/license.html"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ring/ring-core "1.1.0"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [compojure "1.0.4"]
                 [clj-json "0.5.0"]
                 [net.sf.grinder/grinder-core "3.10-SNAPSHOT"]]
  :profiles {:dev {:dependencies
                 [[ring/ring-devel "1.1.0"]]}}

  ; How to produce an uberjar without grinder-core?

  :aot [ net.grinder.console.rest.bootstrap ]

  :min-lein-version "2.0.0")

