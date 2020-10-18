(ns strigui.events)

(defprotocol Actions
    "collection of functions to hook into events"
    (clicked [this] "")
    (key-pressed [this char code] ""))

(defmulti button-clicked :name)

(defmethod button-clicked :default [btn])

(defmulti input-modified :name)

(defmethod input-modified :default [input])