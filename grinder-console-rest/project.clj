(defproject net.sf.grinder/grinder-console-rest "3.10-SNAPSHOT"
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

  ; This currently compiles too much into our jar file, including many
  ; of our direct dependencies. Need to work out how to make Bootstrap
  ; require a smaller set of linked classes.
  :aot [ net.grinder.console.rest.bootstrap ]

  :min-lein-version "2.0.0")
