(ns net.grinder.console.rest.bootstrap
  (:use
    [net.grinder.console.rest.core :only [init-app]])
  (:require
    [ring.adapter.jetty :as jetty])
  (:import
    [net.grinder.console.model ConsoleProperties])
  (:gen-class
   :name net.grinder.console.rest.Bootstrap
   :constructors { [net.grinder.console.model.ConsoleProperties
                    net.grinder.console.model.SampleModel
                    net.grinder.console.model.SampleModelViews
                    net.grinder.console.communication.ProcessControl]
                   [] }
   :init init
   :main true
   :implements [org.picocontainer.Startable]
   :state state
   :prefix bootstrap-
  ))


; Should listen to port property change and restart - how to handle exceptions?

(defn bootstrap-init [ properties model sampleModelViews processControl]
  [ [] {:context {:properties properties
                  :model model
                  :sampleModelViews sampleModelViews
                  :processControl processControl}
        :server (atom nil)} ])

(defn bootstrap-start [this]
  (let [context (:context (.state this))
        port (.getHttpPort (:properties context))
        app (init-app context)]
    (reset! (:server (.state this))
            (jetty/run-jetty app {:port port :join? false}))))

(defn bootstrap-stop [this]
  (.stop @(:server (.state this))))
