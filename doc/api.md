
# Strigui's Api #(>= :alpha32 %)

The main namespace for using strigui is strigui.core
The following doc assumes strigui.core is aliased as gui

```Clojure
(ns ....
  (:require [strigui.core :as gui]))
```

## Transformation

The UI with all its state, including created windows, is represented by a map of widgets. Changes in strigui are done by transforming 
this map into a new state. Strigui will determine if the state transitioning requires redrawing of the widgets that have changed or their
neightbours and in which order widgets need to be redrawn.

State transformations are done via swap-widgets! which requires a function that takes a map of widgets as parameter, and returns the new map of widgets.

Example:
```Clojure
(gui/swap-widgets! (fn [widgets]
                        widgets))
```

## Windows

### Create Windows

Parameter | Explanation 
---|---
widgets | The entire widget map representing the state of the UI
name | The name that is used as a key for the window widget
x | x-axis position of the top left corner of the window on screen
y | y-axis postition of the top left window corner on screen
width | window width
height | window height
title | The window title
options map | A map containing settings for the window behavior or appearance

Options map:
Parameter | Explanation
---|---
:color |
:rendering-hints |
:on-close | Determines the closing behaviour of the window. Possible values capra.core/exit, capra.core/hide
:icon-path | Path to the window icon image in jpeg, gif or png - default: nil
:resizable? | Make the window resizable, true/false - default: false
:visible? | Make the window visible, true/false - default: true

Example:
```Clojure
(gui/swap-widgets! #(gui/add-window % "main-window" 50 50 700 500 "Main Window" {:on-close gui/hide :resizable? true}))
```

### Changing the Color Profile


## Widgets

### Buttons

### Labels

### Text fields

### Lists

## Helpful functions

### Arranging widgets

## Import/Export to Edn

## Custom Widgets