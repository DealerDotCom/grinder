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

(ns net.grinder.console.service.format-params
  "My tweaks to ring.middleware.format-params"
  (:require [ring.middleware.format-params :as rmf]
  ))


(defn- parse-clojure-string [s]
  "Decode a clojure body. The body is merged into the params, so must be a map or a vector of
key value pairs. An empty body is safely handled."
  (when (not (.isEmpty (.trim s)))
    (rmf/safe-read-string s)))


(defn wrap-restful-params
  "Wrapper that tries to do the right thing with the request :body and provide a solid basis for a RESTful API.
It will deserialize to JSON, YAML or Clojure depending on Content-Type header. See wrap-format-response for more details."
  [handler]
  (-> handler
      (rmf/wrap-json-params)
      (rmf/wrap-clojure-params :decoder parse-clojure-string)
      (rmf/wrap-yaml-params)))

