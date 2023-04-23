(defproject strigui "0.0.1-alpha32"
  :description "An experimental GUI library."
  :url "https://github.com/MikeHardIce/strigui"
  :license {:name "MIT License"
            :url "none"
            :year 2023
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.github.mikehardice/capra "0.0.5"]]
  ;;:resource-paths ["resources/capra-0.0.5.jar"]
  :repl-options {:init-ns strigui.playground})