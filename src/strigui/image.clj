(ns strigui.image
  (:require
   [capra.core :as c]
   [strigui.widget :as wdg]))

(defrecord Image [name path props]
  wdg/Widget
  (coord [this context] (let [{:keys [x y width height]} (:props this)]
                          [x y width height]))
  (defaults [this] (assoc-in this [:props :highlight] [:border :alpha]))
  (before-drawing [this] (if (not= (:path this) (-> this :props :loaded-path))
                           (let [loaded-path (:path this)
                                 im (c/path->Image loaded-path)
                                 widget (assoc-in this [:props :loaded-path] loaded-path)
                                 widget (assoc-in widget [:props :loaded-image] im)]
                             widget)
                           this))
  (draw [this context]
        (time (let [[x y width height] (wdg/coord this context)]
                (c/draw-> context
                          (c/image (-> this :props :loaded-image) x y width height (-> this :props :color :background)))))
    this)
  (after-drawing [this]
    this))