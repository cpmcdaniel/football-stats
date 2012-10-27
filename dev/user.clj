(ns user
  (:require [clj-webdriver.taxi :as taxi]
            [clj-http.client :as http])
  (:use clojure.tools.namespace.repl
        [clj-http.util :only [url-encode]]
        clojure.test))

