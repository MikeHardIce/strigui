# strigui

[![Clojars Project](https://img.shields.io/clojars/v/strigui.svg)](https://clojars.org/strigui)

A small straightforward GUI library that can be extended with new Widgets. At the moment, it only contains widgets for a button, input and a label plus the window component itself.
The goal is to provide an easy to use tool to create a simple UI in Clojure. It should provide a few simple widgets to start with,
but you should also be able to create your own widgets that are more or less "managed" by strigui. 
It uses Clojure2d (https://github.com/Clojure2D/clojure2d) underneath. So anything that can be drawn could potentially be a widget (like the game board in https://github.com/MikeHardIce/Dame).

Note: This is in an early alpha stage. It is also my road to Clojure. Any suggestion or help on coding is absolutely welcome!

In project.clj:

```
:dependencies [[strigui "0.0.1-alpha11"]]
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
(gui/label! "welcome" "Welcome to Strigui" {:x 190 :y 100
                                             :color [:green]
                                             :font-size 20 :font-style [:bold]})
(gui/button! "click" "Click me" {:x 400 :y 200 :color [:white :black]})
(gui/input! "input" "" {:x 100 :y 150 :color [:white :red] :min-width 420})
```
The parameters are the name of the widget, the value and a map for the position and optional settings like color, selected?, focused?, can-tab?, can-move? etc. ...
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
    (draw [this canvas] "draw the widget, returns the widget on success"))
```
See https://github.com/MikeHardIce/strigui-sample/blob/main/src/strigui_sample/widget_stacks.clj#L42 for reference

A custom widget could be invoked via

```
...
(:require ...
            [strigui-sample.widget-stacks :as st])
...
(gui/create! (st/->Stack "stacks" '(5 1 8 2 0 3 0 5 7) {:x 100 :y 400}))
```

As mentioned in the begining, https://github.com/MikeHardIce/Dame is another example.

The game board and the 2 buttons are strigui widgets.


## Releasing standalone apps:

When releasing an app which uses strigui, delete the /org/bytedeco directory inside your jar file.
This will make your jar about 960 mb smaller. I believe that is being used in clojure2d when working with images. There must be an alternative way skipping this dependency by default.