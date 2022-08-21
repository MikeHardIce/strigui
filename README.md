# strigui

[![Clojars Project](https://img.shields.io/clojars/v/strigui.svg)](https://clojars.org/strigui)

A small "kind-of-functional" GUI library that can be extended with new Widgets. At the moment, it contains widgets for buttons, input boxes, lists/tables and a label widget for just displaying text.
The goal is to provide an easy to use library to create small desktop apps with Clojure in a somehow functional style. 
Hand swap-widgets! a function to transform all widgets, swap-widgets will apply the function and determine what has been changed to see what needs to be redrawn.


It uses [Capra](https://github.com/MikeHardIce/Capra) underneath (it was using [Clojure2d](https://github.com/Clojure2D/clojure2d) before). So anything that can be drawn could potentially be a widget (like the game board in [Dame](https://github.com/MikeHardIce/Dame)).

[See Changes](CHANGES.md)

In project.clj:

```
:dependencies [[strigui "0.0.1-alpha31"]]
```
[Example](https://github.com/MikeHardIce/strigui-sample)

You need the core namespace.

```Clojure
(ns example.core
  (:require [strigui.core :as gui])
  (:import [java.awt Color]))

```
Create the main window via

```Clojure
(gui/window! 200 300 600 600 "My Window" (java.awt.Color. 255 200 133))
```
or

```Clojure
(gui/window! 200 300 600 600 "My Window" (java.awt.Color. 255 200 133) {java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON})
```
to pass a map consisting of RenderingHint Keys and values that is applied when drawing widgets.

Basic widgets like buttons, input boxes and labels can be created via

```Clojure
(gui/swap-widgets! (fn [wdgs]
                       (-> wdgs
                           (gui/add-label "welcome" "Welcome to Strigui" {:x 190 :y 100
                                                                          :color [(Color. 255 31 0)]
                                                                          :font-size 20 :font-style [:bold]})
                           (gui/add-button "click" "Click me" {:x 400 :y 200 :color [Color/white Color/black]})
                           (gui/add-input "input" "" {:x 100 :y 150 :width 420 :color [Color/white Color/red] :min-width 420}))))
```
The parameters are the name of the widget, the value and a map for the position and optional settings like color, selected?, focused?, can-tab?, can-move? etc. ...
Each function takes the entire map of currently displayed widgets and returns the entire map of widgets.

Events can be attached by using the chosen widget name.

```Clojure
(gui/swap-widgets! (fn [wdgs]
                       (gui/attach-event wdgs "click" :mouse-clicked (fn [_ _] 
                                                                       (gui/close-window!)))))
```
Event functions should usually always return the entire widget map (which is normally the first parameter),
but since the window will close and end the program, it can be skipped.

Custom widgets can be defined by creating a record that implements the protocol of strigui.widget.Widget

```Clojure
(defprotocol Widget
  "collection of functions around redrawing widgets, managing the border etc. ..."
  (coord [this canvas] "gets the coordinates of the widget")
  (defaults [this] "attach default values once the widget gets created")
  (before-drawing [this] "modify the widget each time before it gets drawn")
  (draw [this canvas] "draw the widget, returns the widget on success"))
```
See [example](https://github.com/MikeHardIce/strigui-sample/blob/main/src/strigui_sample/widget_stacks.clj#L42) for reference

A custom widget could be invoked via

```Clojure
...
(:require ...
            [strigui-sample.widget-stacks :as st])
...
(gui/swap-widgets! (fn [wdgs]
                       (gui/add wdgs (st/->Stack "stacks" '(5 1 8 2 0 3 0 5 7) {:x 100 :y 400}))))
```

As mentioned in the begining, [Dame](https://github.com/MikeHardIce/Dame) is another example.

The game board and the 2 buttons are strigui widgets.

## Edn file

Widgets can now be loaded from a edn file too.

Example:
gui-test.edn
```Clojure
{:window [200 300 1500 600 "My Window" (java.awt.Color. 44 44 44)]
 :strigui.label/Label [["welcome" "Welcome to Strigui
                                   and other stuff ..." {:x 190 :y 100 :z 20
                                                      :color {:text (java.awt.Color. 47 120 118)}
                                                      :font-size 20 :font-style [:bold]
                                                      :can-move? true :group "bla"}]]
 :strigui.button/Button [["click" "Click me" {:x 400 :y 250 :z 10 
                                             :highlight [:alpha] :can-tab? true :group "bla"}]]
 :strigui.list/List [["test-list" [{:value "First Item"} {:value "Second Item"} {:value "Third Item"}
                                   {:value "4"} {:value "5"} {:value "6"} {:value "7"} {:value "8"} {:value "9"}] {:x 350 :y 300 :width 150
                                                                                                        :height 200 :highlight [:alpha]}]
                     ["test-table" [{:value [1 "One"]} {:value [2 "Two"]} {:value [3 "Three"]}
                                    {:value [4 "Four"]} {:value [5 "Five"]} {:value [6 "Six"]}
                                    {:value [7 "Seven"]} {:value [8 "Eight"]} {:value [9 "Nine"]}
                                    {:value [10 "Ten"]} {:value [11 "Eleven"]} {:value [12 "Twelve"]}
                                    {:value [13 "Thirteen"]} {:value [14 "Fourteen"]} {:value [15 "Fifteen"]}
                                    {:value [17 "Seventeen"]} {:value [18 "Eighteen"]} {:value [19 "Nineteen"]}
                                    {:value [20 "Twenty"]} {:value [21 "Twentyone"]} {:value [22 "Twentytwo"]}
                                    {:value [23 "Twentythree"]} {:value [24 "Twentyfour"]} {:value [25 "Twentyfive"]}] 
                      {:x 600 :y 100 :width 800 :height 400 :highlight [:alpha]
                       :highlight-alpha-opacity 10
                       :header [{:value "Number" :action :sort} {:value "Name" :action :sort} {:value "Select All" :action :select-all}]}]]
 :strigui.input/Input [["input" "abc" {:x 100 :y 150 :z -20 :width 420 
                                      :highlight [:alpha] :selected? false :can-tab? true 
                                    :can-resize? true :can-move? true :group ["inputs" "bla"]}]
                       ["input1" "" {:x 100 :y 200 
                                    :highlight [:alpha] :width 420 :can-tab? true :group "inputs" :password? true}]]}
```

And load it in your clj file via
```Clojure
(ns example.core
  (:require [strigui.core :as gui]))

....
(gui/from-file! "gui-test.edn")
...
```

Modifications could be done as usual via
```Clojure
(gui/swap-widgets! (fn [wdgs]
                       (-> wdgs
                           (gui/attach-event "click" :mouse-clicked (fn [_ _]
                                                                      (gui/close-window!)))
                           (assoc-in ["click" :props :x] 100)
                           (assoc-in ["click" :props :y] 400)
                           (gui/add-button "btnBla" "Don't Click Me" {:x 100 :y 300 :color {:background (java.awt.Color. 47 120 118) 
                                                                                            :text (java.awt.Color. 247 247 247)
                                                                                            :focus (java.awt.Color. 77 150 148)
                                                                                            :select (java.awt.Color. 77 150 148)
                                                                                            :border (java.awt.Color. 27 100 98)}
                                                                     :highlight [:alpha] :can-tab? true})
                           (update-in ["test-list" :items] conj {:value "10"} {:value "11"} {:value "12"} {:value "13"} {:value "14"} {:value "15"}))))
```

Which would result in:

![](resources/strigui-alpha32.png)

If a widget name already exists, the widget will get overriden by the new widget with the same name.
