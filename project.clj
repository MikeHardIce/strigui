(defproject strigui "0.0.1-alpha32"
  :description "A small GUI library."
  :url "https://github.com/MikeHardIce/strigui"
  :license {:name "MIT License"
            :url "none"
            :year 2022
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 #_[com.github.mikehardice/capra "0.0.3"]]
  :resource-paths ["resources/capra-0.0.4-test5.jar"]
  :repl-options {:init-ns strigui.playground})