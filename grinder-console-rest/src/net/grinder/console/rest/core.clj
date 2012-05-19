(ns net.grinder.console.rest.core
  (:use [compojure handler
                   [core :only [GET POST context defroutes routes]]
                   route])
  (:require
    [clj-json [core :as json]]
    [net.grinder.console.rest.recording :as recording])
  (:import
    [net.grinder.common GrinderBuild GrinderProperties])
  )

; Could do content negotiation?

(defn json-response [data & [status]]
  { :status (or status 200)
    :headers {"Content-Type" "application/json"}
    :body (json/generate-string data) })

(defn- agents-count [pc]
  (.getNumberOfLiveAgents pc))

(defn- agents-stop [pc]
  (.stopAgentAndWorkerProcesses pc)
  "success")

(defn- into-grinder-properties
  [source]
  (let [p (GrinderProperties.)]
    p
    ))

(defn- workers-start [pc properties]
  (.startWorkerProcesses pc (into-grinder-properties properties))
  "success")

(defn- workers-reset [pc]
  (.resetWorkerProcesses pc)
  "success")


(defn- agents-routes [pc]
  (routes
    (GET "/count" [] (json-response (agents-count pc)))
    (POST "/stop" [] (json-response (agents-stop pc)))
    ))

(defn- workers-routes [pc]
  (routes
; // Parse properties
    (POST "/start" [] (json-response (workers-start pc {})))
    (POST "/reset" [] (json-response (workers-reset pc)))
    ))

(defn- recording-routes [sm smv]
  (routes
    (GET "/status" [] (json-response (recording/status sm)))
    (GET "/data" [] (json-response  (recording/data sm smv)))
    (POST "/start" [] (json-response (recording/start sm)))
    (POST "/stop" [] (json-response (recording/stop sm)))
    (POST "/reset" [] (json-response (recording/reset sm)))
    ))

(defn- app-routes
  [process-control sample-model sample-model-views]
  (routes
    (GET "/version" [] (json-response (GrinderBuild/getName)))
    (context "/agents" [] (agents-routes process-control))
    (context "/workers" [] (workers-routes process-control))
    (context "/recording" [] (recording-routes sample-model sample-model-views))
    (not-found "Unknown request")
    ))


(defn create-app
  [state]
  (let [process-control (:processControl state)
        sample-model (:model state)
        sample-model-views (:sampleModelViews state)]
    (compojure.handler/api
      (app-routes process-control sample-model sample-model-views))))


; Support reloading.
(defonce state (atom nil))

(if-let [s @state]
  (def app (create-app s)))

(defn init-app
  [s]
  (recording/register-listener (:model s))
  (reset! state s)
  (def app (create-app s))
  #'app)
