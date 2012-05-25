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

(ns net.grinder.console.model.processes
  (:import [net.grinder.console.communication
            ProcessControl$Listener
            ProcessControl$ProcessReports
            ]
           net.grinder.common.GrinderProperties
           [net.grinder.common.processidentity
            ProcessIdentity
            ProcessAddress]
           ))

(defonce last-reports(atom nil))

(defn initialise
  [pc]
  (.addProcessStatusListener pc
    (reify ProcessControl$Listener
      (update
        [this reports]
        (reset! last-reports reports)))))

(defn agents-stop
  [pc]
  (.stopAgentAndWorkerProcesses pc)
  "success")


(defn- report
  [r]
  (let [^ProcessIdentity i (.getIdentity (.getProcessAddress r))]
    {
     :id (.getUniqueID i)
     :name (.getName i)
     :number (.getNumber i)
     :state (str (.getState r))
     }
   ))

(defn- agent-and-workers
  [^ProcessControl$ProcessReports r]
  (let [agent (report (.getAgentProcessReport r))]
    (into agent {:workers (for [w (.getWorkerProcessReports r)] (report w)) })))

(defn status
  [pc]
  (for [r @last-reports]
    (agent-and-workers r)))

(defn- into-grinder-properties
  [source]
  (let [p (GrinderProperties.)]
    (doseq [[k v] source] (.setProperty p k v))
    p
    ))

(defn workers-start [pc properties]
  (.startWorkerProcesses pc (into-grinder-properties properties))
  "success")

(defn workers-reset [pc]
  (.resetWorkerProcesses pc)
  "success")
