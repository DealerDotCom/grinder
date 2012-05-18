(ns net.grinder.console.rest.play
  (:import
    [net.grinder.common GrinderBuild]
    [org.slf4j LoggerFactory]))


(let [resources (net.grinder.console.common.ResourcesImplementation.
                  "net.grinder.console.common.resources.Console")
      logger (LoggerFactory/getLogger "test")]
  (def cf(net.grinder.console.ConsoleFoundation. resources logger)))

(defn start []
  (.start (Thread. #(.run cf))))

(defn stop []
  (.shutdown cf))
