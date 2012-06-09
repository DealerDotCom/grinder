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

(ns net.grinder.console.service.html
  "Compojure routes that generate plain HTML pages."
  (:use [compojure handler
                   [core :only [GET POST context defroutes routes]]
                   route])
  (:require
    [net.grinder.console.model.processes :as processes]
    [net.grinder.console.model.recording :as recording])
  (:import
    net.grinder.common.GrinderBuild
  ))

(defn- html-response
  [data & [status]]
  { :status (or status 200)
    :headers {"Content-Type" "text/html"}
    :body (str data) })

(defn- agents-routes [pc]
  (routes
    (GET "/status" [] (html-response (processes/status pc)))
    (POST "/stop" [] (html-response (processes/agents-stop pc)))
    ))

(defn- workers-routes [pc]
  (routes
    (POST "/start" [properties]
          (html-response (processes/workers-start pc properties)))
    ))

(defn- recording-routes [sm smv]
  (routes
    (GET "/status" [] (html-response (recording/status sm)))
    (GET "/data" [] (html-response (recording/data sm smv)))
    (POST "/start" [] (html-response (recording/start sm)))
    (POST "/stop" [] (html-response (recording/stop sm)))
    (POST "/zero" [] (html-response (recording/zero sm)))
    (POST "/reset" [] (html-response (recording/reset sm)))
    ))

(defn- app-routes
  [process-control sample-model sample-model-views]
  (routes
    (GET "/version" [] (html-response (GrinderBuild/getName)))
    (context "/agents" [] (agents-routes process-control))
    (context "/workers" [] (workers-routes process-control))
    (context "/recording" [] (recording-routes sample-model sample-model-views))
    ;(not-found "Unknown request")
    ))

(defn create-app
  [{:keys [process-control sample-model sample-model-views]}]
  (->
    (app-routes process-control sample-model sample-model-views)
    compojure.handler/site))
