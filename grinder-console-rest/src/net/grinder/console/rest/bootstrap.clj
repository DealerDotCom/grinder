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
