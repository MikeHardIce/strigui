(defproject strigui "0.0.1-alpha23"
  :description "A small GUI library."
  :url "https://github.com/MikeHardIce/strigui"
  :license {:name "MIT License"
            :url "none"
            :year 2021
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.github.mikehardice/capra "0.0.1-alpha3"]]
  :repl-options {:init-ns strigui.playground})