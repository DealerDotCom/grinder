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

(ns net.grinder.console.model.properties
  (:import
    java.awt.Rectangle
    java.beans.Introspector
    java.io.File
    net.grinder.console.model.ConsoleProperties
    net.grinder.util.Directory
    ))

(defmulti coerce-value class)

(defmethod coerce-value Directory [d]
  (coerce-value (.getFile d)))

(defmethod coerce-value File [f]
  (.getPath f))

(defmethod coerce-value Rectangle [r]
  [(.x r) (.y r) (.width r) (.height r)])

(defmethod coerce-value :default [v] v)

(defn get-properties
  "Return a map representing a ConsoleProperties."
  [properties]
  (let [p (dissoc (bean properties) :class :distributionFileFilterPattern)]
    (into {} (for [[k,v] p] [k (coerce-value v)]))))



(def ^:private property-descriptors
  (into
    {}
    (for [p (.getPropertyDescriptors
              (Introspector/getBeanInfo ConsoleProperties))]
      [(.getName p) p])))

(defmulti box (fn [t v] [t (type v)]))

(defmethod box [File String]
  [_ v]
  (File. v))

(defmethod box [Directory String]
  [_ v]
  (Directory. (File. v)))

(defmethod box [Rectangle java.util.List]
  [_ [x y w h]]
  (Rectangle. x y w h))

(defmethod box :default
  [w v]
  v)

(defmacro illegal
  [fs & args]
  `(throw (IllegalArgumentException. (format ~fs ~@args))))

(defn- set-property
  [properties pd k v]
  (if-let [wm (.getWriteMethod pd)]
    (let [pt (.getPropertyType pd)
          bv (box pt v)
          rm (.getReadMethod pd)]
      (.invoke wm properties (into-array Object [bv]))
      [k (coerce-value
           (.invoke rm properties (into-array [])))])
    (illegal "No write method for property '%s'" k)))

(defn set-properties
  "Update a ConsoleProperties with values from the given map. Returns the
   a map containing the changed keys and their new values."
  [^ConsoleProperties properties m]
  (into {}
    (for [[k v] m]
      (if-let [pd (property-descriptors (if (keyword? k) (name k) k))]
        (try
          (set-property properties pd k v)
          (catch Exception e
            (throw (IllegalArgumentException.
                     (format "Cannot set '%s' to '%s'" k v)
                     e))))
        (illegal "No property '%s'" k)))))

(defn save-properties
  "Save the properties to disk."
  [properties]
  (.save properties)
  "success")
