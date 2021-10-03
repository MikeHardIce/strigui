
[back](README.md)

### 0.0.1-alpha14

* some fixes to the drawing order, this probably needs a rework/different strategy
* resizing, selecting and focusing state drawing functions can be overriden when defining custom widgets

### 0.0.1-alpha13

* the entire gui can now be stored into an edn file
* widgets can now be resized with the mouse (args :can-resize?)
* widget can have one or more groups assigned
* groups can be updated/removed, which in turn will update all widgets assigned to that group
* widgets are now initialized with a width and height, depending on their displayed content
* the widget protocol provides now a "default" function, called when the widget gets created, this helps setting specific properties
* removed value and widget-name functions from widget protocol

### 0.0.1-alpha12

* load gui from an edn file (or load only a subset of widgets via an edn file)
* possibility of custom hide function
