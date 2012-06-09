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
  "Wrap net.grinder.console.communication.ProcessControl."
  (:import [net.grinder.console.communication
            ProcessControl
            ProcessControl$Listener
            ProcessControl$ProcessReports]
           [net.grinder.console.model
            ConsoleProperties]
           net.grinder.common.GrinderProperties
           [net.grinder.common.processidentity
            ProcessAddress
            ProcessIdentity
            ProcessReport]
           ))

(defonce ^:private last-reports (atom nil))
(defonce ^:private initialised (atom false))

(defn initialise
  "Should be called once before status will work."
  [pc]
  (.addProcessStatusListener pc
    (reify ProcessControl$Listener
      (update
        [this reports]
        (reset! last-reports reports))))
  (reset! initialised pc))

(defn agents-stop
  "Stop the agents, and their workers."
  [^ProcessControl pc]
  (.stopAgentAndWorkerProcesses pc)
  :success)


(defn- report
  [^ProcessReport r]
  (let [i (-> r .getProcessAddress .getIdentity)]
    {
     :id (.getUniqueID i)
     :name (.getName i)
     :number (.getNumber i)
     :state (str (.getState r))
     }))

(defn- agent-and-workers
  [^ProcessControl$ProcessReports r]
  (let [agent (report (.getAgentProcessReport r))]
    (into agent {:workers (for [w (.getWorkerProcessReports r)] (report w)) })))

(defn status
  "Return a vector containing the known status of all connected agents and
   worker processes.
   pc is an instance of net.grinder.console.communication.ProcessControl.
   (initialise) must have been called previously with the same ProcessControl,
   otherwise this function will throw an IllegalStateException."
  [^ProcessControl pc]
  (when (not= pc @initialised)
    (throw (IllegalStateException. "Not initialised.")))
  (for [r @last-reports]
    (agent-and-workers r)))

(defn- into-grinder-properties
  [^GrinderProperties p source]
  (doseq [[k v] source] (.setProperty p (name k) (str v)))
    p)

(defn workers-start
  "Send a start signal to the agent to start worker processes.

   This will only take effect if the agent is waiting for the start signal.
   The agent will ignore start signals received while the workers are running.
   We should revisit this in the future to allow process ramp up and ramp
   down to be scripted.

   The supplied-properties contain additional properties to pass on to the
   agent. These take precedence over any specified by the console properties
   \"propertiesFile\" attribute."
  [^ProcessControl pc
   ^ConsoleProperties cp
   supplied-properties]
  (let [f (.getPropertiesFile cp)
        p (if f (GrinderProperties. f) (GrinderProperties.))]
    (.startWorkerProcesses pc (into-grinder-properties p supplied-properties)))
  :success)

(defn workers-stop
  "Send a stop signal to connected worker processes."
  [^ProcessControl pc]
  (.resetWorkerProcesses pc)
  :success)
