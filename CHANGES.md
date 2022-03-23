
[back](https://github.com/MikeHardIce/strigui)

### 0.0.1-alpha25 (in the making)

* fixed the export/import of the current state to/from an edn file
* hiding based on background color by using capra/clear-rect
* fixed text appearing outside the widgets
* almost all previous stateful interactions are replaced by functions taking a
  widget map and returning a widget map -> changed button! to add-button, input! -> add-input.
  Removed functions like update! etc. ...
  From now on swap-widgets! should be used, which takes a function where the argument is the map of widgets.
  Basically its a function that is expected to transform the current map of widgets to a new map of widgets.
  Events can now be attached with attach-event, this function (as the above) expects a map of widgets and
  returns a map of widgets.
* removed register! unregister! replace!
* reworked internal event functions to use swap-widgets!
* removed canvas from widget-global-event
* input fields can now be marked as password fields via args :password?
* added before-drawing function to the widget protocol, which is called each time before a widget is drawn
* mouse-dragging now supported as widget-event
* new widget: list
  

### 0.0.1-alpha24 (latest)

* fixed removing of widgets

### 0.0.1-alpha23

* expose mouse position in mouse related widget-events 

### 0.0.1-alpha22

* replaced clojure2d with capra

### 0.0.1-alpha21

* update function now are variadic (skip-redrawing moved to own function -> update-skip-redraw!,
  update-group-skip-redraw!)
* added global events via strigui.widget namespace
* reverted tab fix

### 0.0.1-alpha20

* support multiline labels
* fixed an issue where a tabbed widget couldn't be tabbed again when removed and then read

### 0.0.1-alpha19 

* added skip-redraw? for widget updates
* added :skip-redrawing option to widget
* fixed issue where widgets can be removed on click by the user before it can get replaced

### 0.0.1-alpha18

* fixed find-by-group! and remove-group!

### 0.0.1-alpha17

* removed unnecessary redrawing of widgets

### 0.0.1-alpha16

* converting strigui.widget/state :widgets to a hashmap instead of a vector
* fixing drawing order when widgets are above/below other widgets and 
  how it affects their neighbours
* fixed tabbing which wasn't tabbing through all tabbable widgets
* register!/unregister!/replace! can now be used without redrawing the body of the widget

### 0.0.1-alpha15

* fixes around the drawing order of widgets and their neighbouring widgets
* when the mouse moves within a widgets, it is not constantly redrawn anymore

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
