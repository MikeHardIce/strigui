(ns strigui.events)

(defmulti button-clicked :name)

(defmethod button-clicked :default [btn])