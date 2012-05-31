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
                   [core :only [GET POST PUT context defroutes routes]]
                   route]
        [ring.middleware.format-params :only [wrap-restful-params]]
        [ring.middleware.format-response :only [wrap-restful-response]])
  (:require
    [net.grinder.console.model.processes :as processes]
    [net.grinder.console.model.properties :as properties]
    [net.grinder.console.model.recording :as recording])
  (:import
    net.grinder.common.GrinderBuild
  ))


(defn- to-body
  "The model functions return raw clojure structures (strings, maps,
   vectors, ...,  which Compojure would handle in various ways.
   Intercept and pass them to the format-response middleware as :body."
  [data & [status]]
  { :status (or status 200)
    :body data })


(defn- agents-routes
  [pc]
  (routes
    (GET "/status" [] (to-body (processes/status pc)))
    (POST "/stop" [] (to-body (processes/agents-stop pc)))
    ))

(defn- workers-routes
  [pc]
  (routes
    (POST "/start" {properties :params}
          (to-body (processes/workers-start pc properties)))
    (POST "/reset" [] (to-body (processes/workers-reset pc)))
    ))

(defn- recording-routes
  [sm smv]
  (routes
    (GET "/status" [] (to-body (recording/status sm)))
    (GET "/data" [] (to-body (recording/data sm smv)))
    (POST "/start" [] (to-body (recording/start sm)))
    (POST "/stop" [] (to-body (recording/stop sm)))
    (POST "/zero" [] (to-body (recording/zero sm)))
    (POST "/reset" [] (to-body (recording/reset sm)))
    ))

(defn- properties-routes
  [p]
  (routes
    (GET "/" [] (to-body (properties/get-properties p)))
    (POST "/" {properties :params}
          (to-body (properties/set-properties p properties)))
    ))

(defn- app-routes
  [process-control
   sample-model
   sample-model-views
   properties]
  (routes
    (GET "/version" [] (to-body (GrinderBuild/getName)))
    (context "/agents" [] (agents-routes process-control))
    (context "/properties" [] (properties-routes properties))
    (context "/workers" [] (workers-routes process-control))
    (context "/recording" [] (recording-routes sample-model sample-model-views))
    (not-found "Resource not found")
    ))


(defn create-app
  [{:keys [process-control
           sample-model
           sample-model-views
           properties]}]
  (->
    (app-routes process-control
                sample-model
                sample-model-views
                properties)
    compojure.handler/api
    wrap-restful-params
    wrap-restful-response))
