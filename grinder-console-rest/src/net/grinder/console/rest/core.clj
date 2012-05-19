; Copyright (C) 2012 Philip Aston
; All rights reserved.
;
; This file is part of The Grinder software distribution. Refer to
; the file LICENSE which is part of The Grinder distribution for
; licensing details. The Grinder distribution is available on the
; Internet at http:;grinder.sourceforge.net/
;
; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
; "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
; LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
; FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
; COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
; INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
; (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
; SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
; HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
; STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
; ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
; OF THE POSSIBILITY OF SUCH DAMAGE.

(ns net.grinder.console.rest.core
  (:use [compojure handler
                   [core :only [GET POST context defroutes routes]]
                   route]
        ring.middleware.json-params)
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
    (doseq [[k v] source] (.setProperty p k v))
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
    (POST "/start" [properties] (json-response (workers-start pc properties)))
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


(defn wrap-request-logging [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [start  (System/nanoTime)
          resp   (handler req)
          finish (System/nanoTime)
          total  (- finish start)]
      (println
        (format "request %s %s (%.2f ms)" request-method uri (/ total 1e6)))
      resp)))

(defn create-app
  [state]
  (let [process-control (:processControl state)
        sample-model (:model state)
        sample-model-views (:sampleModelViews state)]
    (->
      (app-routes process-control sample-model sample-model-views)
      wrap-json-params
      compojure.handler/api
      wrap-request-logging)))


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
