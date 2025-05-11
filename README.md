# strigui

[![Clojars Project](https://img.shields.io/clojars/v/strigui.svg)](https://clojars.org/strigui)

A small experimental GUI library that treats the entire UI as a map of widgets. Changes occur by transforming this map into a new state.
Strigui will figure out which widgets need to be redrawn and in what order.

Available widgets:
- window
- button
- input
- label
- image
- checkbox/radiobutton
- list

Each of those widget types have their own "add" function in strigui.core. In addition there are global events that trigger for a specific window, and events that trigger for specific widgets. See the [Api Documentation] (https://github.com/MikeHardIce/strigui/blob/master/doc/api.md) for further information.


## Quick Start

Create a new app
```
lein new app my-app
```

Add the strigui dependency to your project.clj:

```
:dependencies [[strigui "0.0.1-alpha32"]
              [com.github.mikehardice/capra "0.0.10"]]
```

In my-app/core.clj , add the following to create our first window
```Clojure
(ns my-app.core
  (:require [strigui.core :as gui])
  (:gen-class))

(defn -main
  []
  (gui/swap-widgets! #(gui/add-window % "first-window" 50 50 600 450 "My First Window" {})))
```

Add a label via:
```Clojure
(gui/swap-widgets! #(gui/add-label % "first-window" "lbl-hello" "Hello!" {:x 100 :y 100 :font-size 24})))
```

Of course we can also use threading:

```Clojure
(defn -main
  []
  (gui/swap-widgets! #(-> %
                          (gui/add-window "first-window" 50 50 600 450 "My First Window" {})
                          (gui/add-label "first-window" "lbl-hello" "Hello!" {:x 100 :y 100 :font-size 24}))))

```

Additionally, we can add a button that enables us to drag the label we have just created:

```Clojure
(ns my-app.core
  (:require [strigui.core :as gui])
  (:import [java.awt Color])
  (:gen-class))

(defn -main
  []
  (gui/swap-widgets! #(-> %
                          (gui/add-window "first-window" 50 50 600 450 "My First Window" {})
                          (gui/add-label "first-window" "lbl-hello" "Hello!" {:x 100 :y 100 :font-size 24})
                          (gui/add-button "first-window" "btn-dragging" "Disabled" {:x 400 :y 200})
                          (gui/attach-event "btn-dragging" :mouse-clicked (fn [wdgs _]
                                                                            (-> wdgs
                                                                                (update-in ["lbl-hello" :props]
                                                                                           (fn [props]
                                                                                             (merge props
                                                                                                         (if (:can-move? props)
                                                                                                           {:can-move? false :color {:text Color/black}}
                                                                                                           {:can-move? true :color {:text Color/orange}}))))
                                                                                (update-in ["btn-dragging" :value] (fn [text] 
                                                                                                                    (case text
                                                                                                                      "Disabled" "Enabled"
                                                                                                                      "Disabled")))))))))
```

No matter where we dragged our label, we can save our layout into a edn file via:
```Clojure
(gui/to-file "my-app.edn")
```

And load it from it via:
```Clojure
(gui/from-file! "my-app.edn")
```

For the above, the my-app.edn file will contain something like 
```Clojure
{:window [["first-window" {:y 50, :icon-path nil, :visible? true, :color {:background (java.awt.Color. 250 250 250), :background-widgets (java.awt.Color. 250 250 250)}, :on-close #window exit, :width 600, :title "My First Window", :x 50, :selected? nil, :resizable? false, :height 450}]]
, :strigui.label/Label [["lbl-hello" "Hello!" {:y 301, :resizing? nil, :color {:text (java.awt.Color. 0 0 0)}, :font-size 24, :highlight [], :width 150, :highlight-alpha-opacity 30, :can-hide? true, :window "first-window", :can-move? false, :z 0, :highlight-border-size 1.5, :border-size 1, :x 220, :focused? nil, :selected? nil, :height 42}]]
, :strigui.button/Button [["btn-dragging" "Disabled" {:y 200, :resizing? nil, :color {:background (java.awt.Color. 250 250 250), :text (java.awt.Color. 10 10 10), :focus (java.awt.Color. 117 190 188), :select (java.awt.Color. 117 190 188), :border (java.awt.Color. 27 100 98), :resize (java.awt.Color. 147 220 118)}, :highlight [:border :alpha], :width 150, :highlight-alpha-opacity 30, :can-hide? true, :window "first-window", :z 0, :highlight-border-size 1.5, :border-size 1, :x 400, :focused? nil, :selected? true, :height 42}]]
}
```

Have a look at the "examples" folder.

### Warning

This is an alpha version and may never be completed. It mostly serves as an experiment to see how it would be if the gui is just a map and rendered automaticallhy
when the map is being transformed from one state into the other.
So only use Strigui for fun stuff like [Dame](https://github.com/MikeHardIce/Dame)

When drawing widgets, strigui uses [Capra](https://github.com/MikeHardIce/Capra). This means anything that can be drawn could potentially be a widget.

Creating a custom widget type can easily be done by implementing the widget protocol:
```Clojure
(defprotocol Widget
  "collection of functions around redrawing widgets, managing the border etc. ..."
  (coord [this window] "gets the coordinates of the widget")
  (defaults [this] "attach default values once the widget gets created")
  (before-drawing [this] "modify the widget each time before it gets drawn")
  (draw [this window] "draw the widget, returns the widget on success")
  (after-drawing [this] "modify the widget each time after it got drawn"))
```
And using the core function "add" to create the actual custom widget.
For example the game board in the game [Dame](https://github.com/MikeHardIce/Dame) is added via

```Clojure
(let [board {:game game-start
               :players (list [:player1 :human] [:player2 :human])}]
    (gui/swap-widgets! #(-> %
                          ....
                          (gui/add "main-window" (board/create-board "Dame" board))))
```

[See Changes](CHANGES.md)

