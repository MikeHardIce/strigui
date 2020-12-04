(ns strigui.events)



(defmulti button-clicked :name)

(defmethod button-clicked :default [btn])

(defmulti input-modified :name)

(defmethod input-modified :default [input])