# strigui

[![Clojars Project](https://img.shields.io/clojars/v/strigui.svg)](https://clojars.org/strigui)

A small GUI library that treats the entire UI as a map of widgets. Changes occur by transforming this map into a new state.
Strigui will figure out which widgets need to be redrawn and in what order.

Available widgets:
- window
- button
- input
- label
- image
- checkbox
- list

Each of those widget types has its own "add" function in strigui.core. In addition there are global events that trigger for a specific window, and events that trigger for specific widgets. See the [Api Documentation] (https://github.com/MikeHardIce/strigui/blob/master/doc/api.md) for further information.

Add the strigui dependency to your project.clj:

```
:dependencies [[strigui "0.0.1-alpha32"]
              [com.github.mikehardice/capra "0.0.10"]]
```

### Warning

This is a alpha version and there is still a long way to go until it can be used for anything serious.
So only use it for fun stuff like [Dame](https://github.com/MikeHardIce/Dame)

For drawing widgets, strigui uses [Capra](https://github.com/MikeHardIce/Capra). This means anything that can be drawn could potentially be a widget.

Creating a custom widget type can be easily done by implementing the widget protocol:
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

