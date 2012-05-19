(ns net.grinder.console.rest.recording
  (:import [net.grinder.console.model SampleModel$Listener SampleModel$State$Value ModelTestIndex SampleModelViews]
           [net.grinder.statistics ExpressionView]))

(defonce test-index(atom (ModelTestIndex.)))

(defn register-listener
  [model]
    (.addModelListener model
      (reify SampleModel$Listener

          (stateChanged [this] nil)

          (newSample [this] nil)

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

(defn reset
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
