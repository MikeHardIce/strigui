(defproject strigui "0.0.1-alpha32"
  :description "An experimental GUI library."
  :url "https://github.com/MikeHardIce/strigui"
  :license {:name "MIT"
            :url "https://choosealicense.com/licenses/mit"
            :year 2023
            :key "mit"
            :comment "MIT License"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.github.mikehardice/capra "0.0.10"]
                 [org.clojure/core.async "1.6.681"]]
  ;;:resource-paths ["resources/capra-0.0.9-test.jar"]
  :repl-options {:init-ns strigui.playground}
  ;;:jvm-opts ["-Dsun.java2d.opengl=True"] 
  )