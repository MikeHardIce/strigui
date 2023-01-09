
# Strigui's Api #(>= :alpha32 %)

The main namespace for using strigui is strigui.core
The following doc assumes strigui.core is aliased as gui

```Clojure
(ns ....
  (:require [strigui.core :as gui]))
```

## Transformation

The UI with all its state, including created windows, is represented by a map of widgets. Changes are done by transforming 
this map into a new state. Strigui will determine if the state transitioning requires redrawing of the widgets that have changed, or their
neightbours, and in which order widgets need to be redrawn.

State transformations are done via swap-widgets! which requires a function that takes a map of widgets as parameter, and returns the new map of widgets.

Example:
```Clojure
(gui/swap-widgets! (fn [widgets]
                        widgets))
```

## Windows

### Create Windows

Creates a window on the screen. Multiple windows can be created.

```Clojure
(gui/add-window widgets name x y width height title options)
```

Parameter | Explanation 
---|---
widgets | The full widget map representing the state of the UI
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
:color | <p>Map of java.awt.Color values where the keys are:</br><table><tr><th>Key</th><th>Explanation</th></tr><tr><td>:background-widgets</td><td>Background color of the widgets located on the window</td></tr><tr><td>:background</td><td>Background color of the window</td></tr><tr><td>:text</td><td>Font color of the widgets located on the window</td></tr><tr><td>:focus</td><td>Color when widgets are focused/hovered over</td></tr><tr><td>:select</td><td>Color when widgets are clicked/selected</td></tr><tr><td>:border</td><td>Border color of widgets</td></tr><tr><td>:resize</td><td>Color used when widgets are resized</td></tr></table></p>
:rendering-hints | Map of java.awt.RenderingHint keys and values, specifying the quality of the elements painted
:on-close | Determines the closing behaviour of the window. Possible values capra.core/exit, capra.core/hide
:icon-path | Path to the window icon image in jpeg, gif or png - default: nil
:resizable? | Make the window resizable, true/false - default: false
:visible? | Make the window visible, true/false - default: true

Example:
```Clojure
(gui/swap-widgets! #(gui/add-window % "main-window" 50 50 700 500 "Main Window" {:on-close gui/hide :resizable? true 
                                :color {  :background (java.awt.Color. 44 44 44)
                                          :text (java.awt.Color. 247 247 247)
                                          :focus (java.awt.Color. 117 190 188)
                                          :select (java.awt.Color. 117 190 188)
                                          :border (java.awt.Color. 27 100 98)
                                          :resize (java.awt.Color. 247 247 247)}
                                :rendering-hints {java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON
                                                  java.awt.RenderingHints/KEY_RENDERING java.awt.RenderingHints/VALUE_RENDER_SPEED}}))
```

### Changing the Color Profile

Changes the color profile of a window, which in effect changes the colors of existing widgets located within the particular window. New widgets that are created on the same window will inherit the color profile if no colors were given.

```Clojure
(gui/change-color-profile widgets window-name colors)
```
Parameter | Explanation
---|---
widgets | The full widget map representing the state of the UI
window-name | The window name whoise color profile should be changed.
colors | <p>Map of java.awt.Colors, the following keys are allowed:</br><table><tr><th>Key</th><th>Explanation</th></tr><tr><td>:background-widgets</td><td>Background color of the widgets located on the window</td></tr><tr><td>:background</td><td>Background color of the window</td></tr><tr><td>:text</td><td>Font color of the widgets located on the window</td></tr><tr><td>:focus</td><td>Color when widgets are focused/hovered over</td></tr><tr><td>:select</td><td>Color when widgets are clicked/selected</td></tr><tr><td>:border</td><td>Border color of widgets</td></tr><tr><td>:resize</td><td>Color used when widgets are resized</td></tr></table></p>

Example:
```Clojure
(gui/swap-widgets! #(gui/change-color-profile % "main-window" { :background-widgets (java.awt.Color. 44 44 44) 
                                                                :background (java.awt.Color. 250 250 250)
                                                                :text (java.awt.Color. 161 161 161)
                                                                :focus (java.awt.Color. 117 190 188)
                                                                :select (java.awt.Color. 117 190 188)
                                                                :border (java.awt.Color. 27 100 98)
                                                                :resize (java.awt.Color. 247 247 247)})
```

## Widgets

Widgets are the elements of the UI like text, buttons, lists and labels etc. 
Each widget type holds the information how it will be drawn based on the Capra library (https://github.com/MikeHardIce/Capra), which opens
up the possibility to implement custom widgets that can be used in the same way as the build-in widgets.
Widgets, Windows and their current state can be exported to an edn file, as well as imported from an edn file.


All widget share the following common *properties* map:
Parameter | Explanation
---|---
:x | x position relative of the assigned window (the top left corner of the window is (0 , 0))
:y | y position relative of the assigned window (the top left corner of the window is (0 , 0))
:z | z position, widgets with higher z value are drawn on top of widgets with lower z value
:width | width of the widget
:height | height of the widget
:color | <p>Map of java.awt.Color instances, where the keys can be:<table><tr><th>Key</th><th>Explanation</th></tr><tr><td>:background</td><td>Background color of the widget</td></tr><tr><td>:text</td><td>Font color of the widgets located on the window</td></tr><tr><td>:focus</td><td>Color when widgets are focused/hovered over</td></tr><tr><td>:select</td><td>Color when widgets are clicked/selected</td></tr><tr><td>:border</td><td>Border color of widgets</td></tr><tr><td>:resize</td><td>Color used when widgets are resized</td></tr></table></p>
:highlight | <p>Vector with possible elements: </br><table><tr><td>:alpha</td><td>uses the highlight colors for focusing, selecting and resizing of the widget transparent on top of the widget</td></tr><tr><td>:border</td><td>uses the highlight colors for focusing, selecting and resizing of the widget within the border around the widget</td> </tr></table></br> It is possible to use both via [:alpha :border]</p>
:highlight-alpha-opacity | alpha channel from 0 to 100 for the hightlight color. Only used if :highlight includes :alpha
:can-tab? | Enable widget tabbing. Values: true, false   default: false


### Buttons

### Labels

### Text fields

### Lists

## Helpful functions

### Arranging widgets

## Import/Export to Edn

## Custom Widgets