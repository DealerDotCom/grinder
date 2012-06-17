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

(ns net.grinder.test.console.service.rest-tests
  "Unit tests for net.grinder.console.service.rest."
  (:use [clojure.test])
  (:require [net.grinder.console.service.rest :as rest])
  )

(defmacro check-route
  ([req]
    `(let [ks# [:process-control
                :sample-model
                :sample-model-views
                :properties
                :file-distribution]]

       (let [app# (rest/create-app (zipmap ks# ks#))]
         (app# (assoc ~req :scheme :http)))))

  ([req [function parameters & rest]]
    `(with-redefs [~function #(is (= ~parameters %&))]
       (check-route ~req ~@rest))))


(defn- ok-status
  [res]
  (is (= 200 (:status res)))
  res)

(defn- is-json
  [res]
  (is (re-find #"^application/json" (get-in res [:headers "Content-Type"])))
  res)



(deftest version
  (re-find #"The Grinder"
           (->
             (check-route {:request-method :get
                           :uri "/version"})
             ok-status
             :body)))

(deftest basic-routes
  (are [method uri fn params]
       (-> (check-route {:request-method method
                         :uri uri} [fn params])
         ok-status
         is-json)
       :get "/agents/status" net.grinder.console.model.processes/status [:process-control]
       :post "/agents/stop" net.grinder.console.model.processes/agents-stop [:process-control]
       :post "/agents/stop-workers" net.grinder.console.model.processes/workers-stop [:process-control]
       :post "/files/distribute" net.grinder.console.model.files/start-distribution [:file-distribution]
       :get "/files/status" net.grinder.console.model.files/status [:file-distribution]
       :get "/properties" net.grinder.console.model.properties/get-properties [:properties]
       :post "/properties/save" net.grinder.console.model.properties/save [:properties]
       :get "/recording/status" net.grinder.console.model.recording/status [:sample-model]
       :get "/recording/data" net.grinder.console.model.recording/data [:sample-model :sample-model-views]
       :post "/recording/start" net.grinder.console.model.recording/start [:sample-model]
       :post "/recording/stop" net.grinder.console.model.recording/stop [:sample-model]
       :post "/recording/zero" net.grinder.console.model.recording/zero [:sample-model]
       :post "/recording/reset" net.grinder.console.model.recording/reset [:sample-model]
       ))

(deftest start-workers
  (are [input params]
       (-> (check-route {:request-method :post
                         :uri "/agents/start-workers"
                         :params input}
                        [net.grinder.console.model.processes/workers-start
                         [:process-control :properties params]])
         ok-status
         is-json)
    {} {}
    {:foo :bah} {:foo :bah}
    {"foo" :bah} {:foo :bah}))

(deftest put-properties
  (are [input params]
       (-> (check-route {:request-method :put
                         :uri "/properties"
                         :params input}
                        [net.grinder.console.model.properties/set-properties
                         [:properties params]])
         ok-status
         is-json)
    {} {}
    {:foo :bah} {:foo :bah}
    {"foo" :bah} {:foo :bah}))

(deftest unknown-routes
  (are [method uri]
       (is (= 404
              (-> (check-route {:request-method method
                                :uri uri})
                :status)))
       :get "agents/status"
       :post "/agents/status"
       :put "/agents/stop"
       :get "/"
       :get "/recording/zero"))
