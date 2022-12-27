## Transformation

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

```Clojure
(gui/swap-widgets! #(gui/add-window % "main-window" 50 50 700 500 "Main Window" {:on-close gui/hide :resizable? true}))
```