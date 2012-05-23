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
  (:require
    [ring.adapter.jetty :as jetty]
    [net.grinder.console.rest.core :as core])
  (:import
    net.grinder.console.model.ConsoleProperties
    java.beans.PropertyChangeListener)
  (:gen-class
   :name net.grinder.console.rest.Bootstrap
   :constructors { [net.grinder.console.model.ConsoleProperties
                    net.grinder.console.model.SampleModel
                    net.grinder.console.model.SampleModelViews
                    net.grinder.console.communication.ProcessControl
                    net.grinder.console.common.ErrorQueue]
                   [] }
   :init init
   :implements [org.picocontainer.Startable]
   :state state
   :prefix bootstrap-
  ))


; Should listen to port property change and restart.
; Support different listen host.

(defn- stop-jetty
  [server error-handler]
  (when server
    (try
      (.stop server)
      (catch Exception e
       (.handleException error-handler e)
       server)))
  )

(defn- start-jetty
  [server port error-handler app]
  (or (stop-jetty server error-handler)
      (try
        (jetty/run-jetty app {:port port :join? false})
        (catch Exception e
          (.handleException
            error-handler
            e
            "Failed to start HTTP server")
          server)))
  )


(defn- restart
  [state]
  (let [context (:context state)
        server (:server state)
        port (.getHttpPort (:properties context))
        app (core/init-app context)
        error-handler (:errorQueue context)
        ]
    (reset! server (start-jetty @server port error-handler app))))


(defn bootstrap-init
  [properties model sampleModelViews processControl errorQueue]
  (let [state
        {:context {:properties properties
                  :model model
                  :sampleModelViews sampleModelViews
                  :processControl processControl
                  :errorQueue errorQueue}
        :server (atom nil)}]

    (.addPropertyChangeListener
      properties
      (reify PropertyChangeListener
        (propertyChange
          [this event]
          (let [n (.getPropertyName event)]
            (println n)
            (if (#{ConsoleProperties/HTTP_HOST_PROPERTY
                   ConsoleProperties/HTTP_PORT_PROPERTY
                   } n)
              (restart state))))))

    [ [] state ]))

(defn bootstrap-start [this]
  (let [state (.state this)]
    (restart state)))

(defn bootstrap-stop [this]
  (let [state (.state this)
        context (:context state)
        server (:server state)
        error-handler (:errorQueue context)]
    (reset! server (stop-jetty @server error-handler))))
