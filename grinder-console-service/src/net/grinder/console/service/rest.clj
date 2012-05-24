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

(ns net.grinder.console.service.rest
  "Compojure application that provides the console REST API."
  (:use [compojure handler
                   [core :only [GET POST context defroutes routes]]
                   route]
        ring.middleware.json-params)
  (:require
    [clj-json [core :as json]]
    [net.grinder.console.service.processes :as processes]
    [net.grinder.console.service.recording :as recording])
  (:import
    org.codehaus.jackson.JsonParseException
    net.grinder.common.GrinderBuild
  ))

(defn- json-response
  [data & [status]]
  { :status (or status 200)
    :headers {"Content-Type" "application/json"}
    :body (json/generate-string data) })

(defn- agents-routes [pc]
  (routes
    (GET "/status" [] (json-response (processes/status pc)))
    (POST "/stop" [] (json-response (processes/agents-stop pc)))
    ))

(defn- workers-routes [pc]
  (routes
    (POST "/start" [properties]
          (json-response (processes/workers-start pc properties)))
    (POST "/reset" [] (json-response (processes/workers-reset pc)))
    ))

(defn- recording-routes [sm smv]
  (routes
    (GET "/status" [] (json-response (recording/status sm)))
    (GET "/data" [] (json-response (recording/data sm smv)))
    (POST "/start" [] (json-response (recording/start sm)))
    (POST "/stop" [] (json-response (recording/stop sm)))
    (POST "/zero" [] (json-response (recording/zero sm)))
    (POST "/reset" [] (json-response (recording/reset sm)))
    ))

(defn- app-routes
  [process-control sample-model sample-model-views]
  (routes
    (GET "/version" [] (json-response (GrinderBuild/getName)))
    (context "/agents" [] (agents-routes process-control))
    (context "/workers" [] (workers-routes process-control))
    (context "/recording" [] (recording-routes sample-model sample-model-views))
    ;(not-found "Unknown request")
    ))

(defn- wrap-json-response [handler]
  (fn [req]
    (try
      (or (handler req)
          (json-response {"error" "resource not found"} 404))
      (catch JsonParseException e
        (json-response
          {"error" (format "malformed json: %s" (.getMessage e))} 400))
      #_(catch Exception e
        (json-response
          {"error" (.getMessage e)} 400))
      )))

(defn create-app
  [{:keys [process-control sample-model sample-model-views]}]
  (->
    (app-routes process-control sample-model sample-model-views)
    wrap-json-params
    wrap-json-response
    compojure.handler/api))
