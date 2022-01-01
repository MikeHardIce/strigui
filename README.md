# strigui

[![Clojars Project](https://img.shields.io/clojars/v/strigui.svg)](https://clojars.org/strigui)

A small straightforward GUI library that can be extended with new Widgets. At the moment, it only contains widgets for a button, input and a label plus the window component itself.
The goal is to provide an easy to use library to create small desktop apps with Clojure. It should provide a few simple widgets to start with,
but you can create your own widgets too. 
It uses [Capra](https://github.com/MikeHardIce/Capra) underneath (it was using [Clojure2d](https://github.com/Clojure2D/clojure2d) before). So anything that can be drawn could potentially be a widget (like the game board in [Dame](https://github.com/MikeHardIce/Dame)).

[See Changes](CHANGES.md)

In project.clj:

```
:dependencies [[strigui "0.0.1-alpha23"]]
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
(gui/window! 600 600)
```

Basic widgets like buttons, input boxes and labels can be created via

```Clojure
(gui/label! "welcome" "Welcome to Strigui" {:x 190 :y 100
                                             :color [Color/green]
                                             :font-size 20 :font-style [:bold]})
(gui/button! "click" "Click me" {:x 400 :y 200 :color [Color/white Color/black]})
(gui/input! "input" "" {:x 100 :y 150 :color [Color/white Color/red] :min-width 420})
```
The parameters are the name of the widget, the value and a map for the position and optional settings like color, selected?, focused?, can-tab?, can-move? etc. ...
The name is used when widgets are modified.

Events can be attached by using the chosen widget name.

```Clojure
(gui/update! "click" :events {:mouse-clicked (fn [wdg]
                                                (gui/info "Button A pressed"))})
```

A widget can be removed, updated with

```Clojure
(gui/update! "welcome" :value "A new title")
(gui/remove! "input")
```

It is also possible to retrieve a widget by name via
```Clojure
(gui/find-by-name "click")
```

Custom widgets can be defined by creating a record that implements the protocol of strigui.widget.Widget

```Clojure
(defprotocol Widget 
    "collection of functions around redrawing widgets, managing the border etc. ..."
  (coord [this canvas] "gets the coordinates of the widget")
  (defaults [this] "attach default values")
  (draw [this canvas] "draw the widget, returns the widget on success"))

```
See [example](https://github.com/MikeHardIce/strigui-sample/blob/main/src/strigui_sample/widget_stacks.clj#L42) for reference

A custom widget could be invoked via

```Clojure
...
(:require ...
            [strigui-sample.widget-stacks :as st])
...
(gui/create! (st/->Stack "stacks" '(5 1 8 2 0 3 0 5 7) {:x 100 :y 400}))
```

As mentioned in the begining, [Dame](https://github.com/MikeHardIce/Dame) is another example.

The game board and the 2 buttons are strigui widgets.

## Edn file

Widgets can now be loaded from a edn file too.

Example:
gui-test.edn
```Clojure
{:window [600 600 "From a edn file"]
 :strigui.label/Label [["welcome" "Welcome to Strigui" {:x 190 :y 100
                                                      :color [Color/green]
                                                      :font-size 20 :font-style [:bold]
                                                      :can-move? true}]]
 :strigui.button/Button [["click" "Click me" {:x 400 :y 250 :z 10 :color [Color/white Color/black] :can-tab? true}]]
 :strigui.input/Input [["input" "" {:x 100 :y 150 :color [Color/white Color/red] :min-width 420 :selected? true :can-tab? true}]
                       ["input1" "" {:x 100 :y 200 :color [Color/white Color/red] :min-width 420 :can-tab? true}]]}
```

And load it in your clj file via
```Clojure
(ns example.core
  (:require [strigui.core :as gui]))

....
(gui/from-file "gui-test.edn")
...
```

If a widget name already exists, the widget gets unregistered and replaced by the new widget.