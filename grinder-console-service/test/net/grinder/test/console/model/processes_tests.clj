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

(ns net.grinder.test.console.model.processes-tests
  "Unit tests for net.grinder.console.model.processes."
  (:use [clojure.test]
        [net.grinder.test])
  (:require [net.grinder.console.model.processes :as processes])
  (:import [net.grinder.console.communication
            ProcessControl
            ProcessControl$ProcessReports]
           [net.grinder.console.model
            ConsoleProperties]
           [net.grinder.messages.console
            AgentAddress
            AgentAndCacheReport
            WorkerAddress]
           [net.grinder.common.processidentity
            AgentIdentity
            ProcessReport$State
            WorkerIdentity
            WorkerProcessReport]
           [java.io
            FileWriter]))

(declare called)

(deftest test-agents-stop
  (with-redefs [called (atom false)]
    (let [pc (reify ProcessControl
               (stopAgentAndWorkerProcesses [this] (reset! called true)))]
      (is (= :success (processes/agents-stop pc)))
      (is @called)
    )))

(defn- workers-start
  [console-properties user-properties expected]
  (with-redefs [called (atom false)]
    (with-temporary-files [f1 f2]
      (with-open [w (FileWriter. f2)]
        (doseq [[k v] console-properties]
          (.write w (format "%s: %s\n" k v))))

      (let [pc (reify ProcessControl
                 (startWorkerProcesses [this p] (reset! called p)))
            cp (ConsoleProperties. nil f1)]
        (.setPropertiesFile cp f2)
        (is (= :success (processes/workers-start pc cp user-properties)))
        (is (= expected @called))
        ))))

(deftest test-workers-start-empty-properties
  (workers-start {} {} {}))

(deftest test-workers-start-with-properties
  (workers-start {"grinder.runs" 1
                  "grinder.threads" 2}
                 {"f1" "v1"
                  :f2 :v2
                  "grinder.runs" 99}
                 {"grinder.runs" "99"
                  "grinder.threads" "2"
                  "f1" "v1"
                  "f2" ":v2"}))

(deftest test-workers-stop
  (with-redefs [called (atom nil)]
    (let [pc (reify ProcessControl
               (resetWorkerProcesses [this] (reset! called true)))]
      (is (= :success (processes/workers-stop pc)))
      (is @called)
    )))

(deftest test-status-with-no-reports
  (def listener (atom nil))
  (let [pc (reify ProcessControl
             (addProcessStatusListener [this l] (reset! listener l)))]
    (processes/initialise pc)
    (let [l @listener]
      (.update l (into-array ProcessControl$ProcessReports []))
      (is (= [] (processes/status pc))))))


(defrecord MockAgentIdentity
  [id name number]
  AgentIdentity
  (getUniqueID [this] id)
  (getName [this] name)
  (getNumber [this] number))

(defrecord MockWorkerIdentity
  [id name number]
  WorkerIdentity
  (getUniqueID [this] id)
  (getName [this] name)
  (getNumber [this] number))

(defn- make-state
  [s]
  (ProcessReport$State/valueOf (name s)))

(defrecord MockAgentReport
  [agent-identity state]
  AgentAndCacheReport
  (getAgentIdentity [this] agent-identity)
  (getProcessAddress [this] (AgentAddress. agent-identity))
  (getState [this] (make-state state))
  )

(defrecord MockWorkerReport
  [worker-identity state running-threads maximum-threads]
  WorkerProcessReport
  (getWorkerIdentity [this] worker-identity)
  (getProcessAddress [this] (WorkerAddress. worker-identity))
  (getState [this] (make-state state))
  (getNumberOfRunningThreads [this] running-threads)
  (getMaximumNumberOfThreads [this] maximum-threads))

(defrecord MockReports
  [agent-report worker-reports]
  ProcessControl$ProcessReports
  (getAgentProcessReport [this] agent-report)
  (getWorkerProcessReports
    [this]
    (into-array WorkerProcessReport worker-reports)))

(deftest test-status-with-reports
  (def listener (atom nil))
  (let [pc (reify ProcessControl
             (addProcessStatusListener [this l] (reset! listener l)))]
    (processes/initialise pc)
    (let [l @listener
          r1 (MockReports.
               (MockAgentReport.
                 (MockAgentIdentity. "1" "foo" 10)
                 :RUNNING)
               [(MockWorkerReport.
                  (MockWorkerIdentity. "13" "bah" 9)
                  :STARTED
                  2
                  22)])]
      (.update l (into-array ProcessControl$ProcessReports [r1]))
      (is (= [{:id "1" :name "foo" :number 10 :state "RUNNING" :workers
               [{:id "13" :name "bah" :number 9 :state "STARTED"
                 :running-threads 2 :maximum-threads 22}]}]
             (processes/status pc))))))


(deftest test-status-uninitialised
  (let [pc (reify ProcessControl)]
    (is (thrown? IllegalStateException (processes/status pc)))))

(deftest test-status-initialised-different-pc
  (let [pc (reify ProcessControl
             (addProcessStatusListener [this l]))
        pc2 (reify ProcessControl)]
    (processes/initialise pc)
    (is (thrown? IllegalStateException (processes/status pc2)))))


