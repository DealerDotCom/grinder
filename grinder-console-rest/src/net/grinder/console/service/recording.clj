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

(ns net.grinder.console.service.recording
  (:import [net.grinder.console.model
            SampleModel$Listener
            SampleModel$State$Value
            ModelTestIndex SampleModelViews]
           [net.grinder.statistics ExpressionView]))

(defonce test-index(atom (ModelTestIndex.)))

(defn initialise
  [model]
    (.addModelListener model
      (reify SampleModel$Listener

          (stateChanged
            [this]
            nil)

          (newSample
            [this]
            nil)

          (newTests
            [this tests index]
            (reset! test-index index))

          (resetTests
            [this]
            (reset! test-index (ModelTestIndex.))))))

(defn status
  [model]
  (let [s (.getState model)
        v (.getValue s)
        m {:state (str v)
           :description (.getDescription s)}]
    (if (#{ SampleModel$State$Value/IgnoringInitialSamples
            SampleModel$State$Value/Recording } v)
      (assoc m :sample-count (.getSampleCount s))
      m)))

(defn start
  [model]
  (.start model)
  (status model))

(defn stop
  [model]
  (.stop model)
  (status model))

(defn zero
  [model]
  (.zeroStatistics model)
  (status model))

(defn reset
  "After a reset, the model loses all knowledge of Tests; this can be
   useful when swapping between scripts. It makes sense to reset with
   the worker processes stopped."
  [model]
  (.reset model)
  (status model))

(defn- process-statistics
  [views statistics]
  (for [^ExpressionView v views]
    (let [e (.getExpression v)]
      (if (.isDouble e)
        (.getDoubleValue e statistics)
        (.getLongValue e statistics)))))

(defn data
  [sample-model ^SampleModelViews statistics-view]
  (let [^ModelTestIndex model @test-index
        views (.getExpressionViews
                (.getCumulativeStatisticsView statistics-view))]
    {:status (status sample-model)
     :columns (for [^ExpressionView v views] (.getDisplayName v))
     :tests
     (for [i (range (.getNumberOfTests model))]
       (let [test (.getTest model i)]
         {
          :test (.getNumber test)
          :description (.getDescription test)
          :statistics
          (process-statistics views
                              (.getCumulativeStatistics model i)) }))
     :totals (process-statistics views
                                 (.getTotalCumulativeStatistics sample-model))
     }
    ))
