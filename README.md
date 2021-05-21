# strigui

[![Clojars Project](https://img.shields.io/clojars/v/strigui.svg)](https://clojars.org/strigui)

A small straightforward GUI library that can be extended with new Widgets. At the moment, it only contains Buttons, TextFields, Labels and the window component itself.
The goal is to provide an easy to use tool to create a simple UI.

In project.clj:

```
:dependencies [[strigui "0.0.1-alpha5"]]
```
Example at https://github.com/MikeHardIce/strigui-sample

You need the core namespace.

```
(ns example.core
  (:require [strigui.core :as gui]))

```
Create the main window via

```
(gui/window! 600 600)
```

Basic widgets like buttons, input boxes and labels can be created via

```
(gui/label "welcome" "Welcome to Strigui" {:x 190 :y 100
                                             :color [:green]
                                             :font-size 20 :font-style [:bold]})
(gui/button "click" "Click me" {:x 400 :y 200 :color [:white :black]})
(gui/input "input" "" {:x 100 :y 150 :color [:white :red] :min-width 420})
```
The parameters are the name of the widget, the value and a map for the position and optional settings like color etc. ...
The name is used when widgets are modified.

Events can be attached by using the chosen widget name.

```
(gui/update! "click" :events {:mouse-clicked (fn [wdg]
                                                (gui/info "Button A pressed"))})
```

A widget can be removed, updated with

```
(gui/update! "welcome" :value "A new title")
(gui/remove! "input")
```

It is also possible to retrieve a widget by name via
```
(gui/find-by-name "click")
```

Custom widgets can be defined by creating a record that implements the protocol of strigui.widget.Widget

```
(defprotocol Widget 
    "collection of functions around redrawing widgets, managing the border etc. ..."
    (coord [this canvas] "gets the coordinates of the widget")
    (value [this] "the current text of the widget")
    (args [this] "the current args of the widget")
    (widget-name [this] "name of the widget")
    (draw [this canvas] "draw the widget, returns the widget on success")
    (redraw [this canvas] "redraw the widget"))
```
See https://github.com/MikeHardIce/strigui-sample/blob/main/src/strigui_sample/widget_stacks.clj#L42 for reference

A custom widget could be invoked via

```
...
(:require ...
            [strigui-sample.widget-stacks :as st])
...
(gui/create (st/->Stack "stacks" '(5 1 8 2 0 3 0 5 7) {:x 100 :y 400}))
```
