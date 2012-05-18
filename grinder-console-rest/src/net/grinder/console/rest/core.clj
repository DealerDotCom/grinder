(ns net.grinder.console.rest.core
  (:use [compojure handler
                   [core :only [GET POST context defroutes]]
                   route])
  (:require
    [ring.adapter.jetty :as jetty]
    [clj-json [core :as json]])
  (:import
    [net.grinder.common GrinderBuild])
  )

; Sigh - global state.
(defonce state (atom nil))

; Could do content negotiation?

(defn json-response [data & [status]]
  { :status (or status 200)
    :headers {"Content-Type" "application/json"}
    :body (json/generate-string data) })

(defn version []
  (json-response (GrinderBuild/getName)))

(defn number-of-agents []
  (json-response (.getNumberOfLiveAgents (:processControl @state))))

(defn agents-stop []
  (.stopAgentAndWorkerProcesses (:processControl @state))
  (json-response "success"))

(defn workers-start []
  (.startWorkerProcesses (:processControl @state))
  (json-response "success"))

(defn workers-reset []
  (.resetWorkerProcesses (:processControl @state))
  (json-response "success"))


(defn sm-start [m]
  (.start m)
  (json-response "success"))

(defn sm-stop [m]
  (.stop m)
  (json-response "success"))

(defn sm-reset [m]
  (.reset m)
  (json-response "success"))

(defn sm-status [m]
  (let [s (.getState m)]
    (json-response {:description (.getDescription s)
                    :capturing   (.isCapturing s)
                    :stopped     (.isStopped s)})))

(defn sample-model-handler []
  (let [sm (:model @state)]
    (defroutes _
      (GET "/start" [] (sm-start sm))
      (GET "/stop" [] (sm-stop sm))
      (GET "/reset" [] (sm-reset sm))
      (GET "/status" [] (sm-status sm)))))


(defroutes app*
  (GET "/version" [] (version))
  (GET "/agents/numberOfAgents" [] (number-of-agents))
  (GET "/agents/stop" [] (agents-stop))
  ; // Parse properties (POST "/workers/start" request (workers-start))
  (GET "/workers/reset" [] (workers-reset))
  (context "/recording" [] (sample-model-handler))
  (not-found "Unknown request")
  )

(def app (compojure.handler/api app*))


(defn start-jetty [ c ]
  (let [port (.getHttpPort (:properties c))]
    (reset! state c)
    (jetty/run-jetty #'app {:port port :join? false})))

(defn stop-jetty [ server]
  (.stop server))
