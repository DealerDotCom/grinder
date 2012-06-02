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

(ns net.grinder.console.model.files)


(def ^:private next-id (atom 0))
(def ^:private distribution-result (atom nil))
(def ^:private handler (agent nil))

(defn status
  [fd]
  (let [cache-state (.getAgentCacheState fd)
        stale (.getOutOfDate cache-state)]
    { :stale stale
      :last-distribution @distribution-result}))


(defn- add-file
  [{:keys [files] :as last-result} result]
  (assoc last-result
    :state :sending
    :per-cent-complete (.getProgressInCents result)
    :files (conj files (.getFileName result))))

(defn- finished
  [last-result]
  (assoc last-result
    :state :finished))

(defn- error
  [last-result e]
  (assoc last-result
    :state :error
    :exception e))

(defn- process
  [handler]
  (if-let
    [result (.sendNextFile handler)]
    (do
      (swap! distribution-result add-file result)
      (recur handler))
    (swap! distribution-result finished)))

(defn start-distribution
  [fd]
  (let [n (swap! next-id inc)]
    (letfn [(start-process
              [_]
              (reset! distribution-result {:id n, :state :started, :files []})
              (try
                 (process (.getHandler fd))
                 (catch Exception e
                   (swap! distribution-result error e)))
               n)]
           (send handler start-process)
           [:distribution-started n])))



