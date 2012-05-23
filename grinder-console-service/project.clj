(defproject net.sf.grinder/grinder-console-service "3.10-SNAPSHOT"
  :parent [net.sf.grinder/grinder-parent "3.10-SNAPSHOT"]
  :description "REST API to The Grinder console."
  :url "http://grinder.sourceforge.net"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [ring/ring-core "1.1.0"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [ring-json-params "0.1.3"]
                 [compojure "1.0.4"]
                 [clj-json "0.5.0"]
                 [net.sf.grinder/grinder-core "3.10-SNAPSHOT" :scope "provided"]]
  :profiles {:dev {:dependencies
                 [[ring/ring-devel "1.1.0"]]}}

  :aot [ net.grinder.console.service.bootstrap ]

  :min-lein-version "2.0.0")
