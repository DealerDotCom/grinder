(ns net.grinder.console.rest.recording
  (:import [net.grinder.console.model SampleModel$Listener]))

(defonce test-index (atom nil))

(defn register-listener
  [model]
    (.addModelListener model
      (reify SampleModel$Listener

          (stateChanged [this] nil)

          (newSample [this] nil)

          (newTests
            [this tests index]
            (reset! test-index index))

          (resetTests [this] (println "RESET")))))

(defn status
  [model]
  (let [s (.getState model)]
    {:description (.getDescription s)
     :capturing   (.isCapturing s)
     :stopped     (.isStopped s)}))


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

(defn data
  []
  (if-let [i @test-index]
    (let [n (.getNumberOfTests i)]
      n)
    []))
