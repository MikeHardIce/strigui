(defproject strigui "0.0.1-alpha28"
  :description "A small GUI library."
  :url "https://github.com/MikeHardIce/strigui"
  :license {:name "MIT License"
            :url "none"
            :year 2022
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.github.mikehardice/capra "0.0.2"]]
  :repl-options {:init-ns strigui.playground})