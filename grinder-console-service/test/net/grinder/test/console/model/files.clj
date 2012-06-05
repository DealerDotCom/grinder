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

(ns net.grinder.test.console.model.files
  "Unit tests for net.grinder.console.model.files."
  (:use [clojure.test]
        [net.grinder.console.model.files])
  (:require [net.grinder.console.model.files :as files])
  (:import [net.grinder.console.distribution
            AgentCacheState
            FileDistribution
            FileDistributionHandler
            FileDistributionHandler$Result]))


(defrecord MockCacheState
  [out-of-date]
  AgentCacheState
  (getOutOfDate [this] out-of-date))

(defrecord MockResult
  [progress file]
  FileDistributionHandler$Result
  (getProgressInCents [this] progress)
  (getFileName [this] file))

(def results (atom []))

(defrecord MockHandler
  []
  FileDistributionHandler
  (sendNextFile [this]
                (let [[f & r] @results]
                  (reset! results r)
                  f)))

(defrecord MockFD
  [cache-state]
  FileDistribution
  (getAgentCacheState [this] cache-state)
  (getHandler [this] (MockHandler.)))


(deftest test-status
  (reset! @#'files/distribution-result :foo)
  (let [fd (MockFD. (MockCacheState. true))
        s (status fd)]
    (is (= {:stale true :last-distribution :foo} s))
  ))

(def history (atom []))

(add-watch @#'files/distribution-result :test
           (fn [k r o n] (swap! history conj n)))

(deftest test-start-distribution
  (reset! results
           [(MockResult. 50 "a")
            (MockResult. 100 "b")])
  (reset! @#'files/next-id 22)
  (reset! history [])

  (let [initial (start-distribution (MockFD. nil))]
    (await-for 1000 @#'files/distribution-agent)
    (is (= {:id 23 :state :started :files []} initial))
    (is (= [initial
            {:id 23 :state :sending :files ["a"] :per-cent-complete 50}
            {:id 23 :state :sending :files ["a" "b"] :per-cent-complete 100}
            {:id 23 :state :finished :files ["a" "b"] :per-cent-complete 100}
            ]
           @history))))


(deftest test-start-distribution-bad-handler
  (reset! @#'files/next-id 0)
  (reset! history [])

  (let [e (RuntimeException.)
        fd (reify FileDistribution (getHandler [this] (throw e)))
        initial (start-distribution fd)]
    (await-for 1000 @#'files/distribution-agent)
    (is (= {:id 1 :state :started :files []} initial))
    (is (= [initial
            {:id 1 :state :error :exception e :files []}
            ]
           @history))))

