
[back](https://github.com/MikeHardIce/strigui)

## future (for non alpha release)
  * focus on a complete set of widgets, like DatePicker, DropDown, Radio Buttons, etc. ...
  * window property :guides? which draws visual guides to help align widgets when dragging them across the window
  * get rid of box.clj and rather provide a namespace with helper functions to easily construct
    types of widgets
  * multi-select widgets via shift+left-mouse-click to drag them as a group
  * find a way to use the OS color theme by default if no color maps are given
  * align function to align an widget relative to another widget with :left :center :right. 
  * better defaults, just add a widget with x and y coordinates and everything else is default
  * improve overall performance (large lists, moving images, ...)

## 0.0.1-alpha32 (in progress)

### General
  * unify event signatures
  * arrange function to automatically adjust position of selected widgets. Widgets can be aligned :left :center :right
  * add-multiple function to add a bulk of widgets of a given type by providing just their names and values
  * fixed tabbing when no widget was selected previously
  * enable updating properties of multiple widgets at the same time
  * reworked how widgets are selected for redrawing  
  * reworked how neighbours are determined
  * removed def-action macro, use the property :can-hide? instead
  * internally reworked swap-widgets so that it can be used internally with update-in 
  * State map now uses an agent instead of an atom
  * support for multiple windows
  * better documentation (in progress, [wiki](https://github.com/MikeHardIce/strigui/wiki))
  * export/import with reader tag #window for hide and exit
  * windows don't get recreated anymore when calling add-window with the same widget name
  * Unified all event parameters to 2 parameters -> first parameter is the map of widgets, 2nd a map of all arguments like the widget itself, perhaps coordinates etc. ... therefore no more guessing when using strigui
  - TODO: Have a process in the background that redraws every window once/twice per second and don't redraw anymore on "swap-widgets". Use that solely to
    update the widget map via an agent

### Highlight properties
  * highlight alpha opacity can be adjusted via key :highlight-alpha-opacity accepting integers from 0 to 255
  * highlight border size can be adjusted via key :highlight-border-size (accepting a non negative numbers)
  * removed :has-border? property key, hightlight are now active as soon as a :highlight key with a non empty vector is provided
  * give option for widget highlights to be a combination of :border :none :alpha (:alpha with add a transparent layer 
    in the highlight color on top)

### Widgets
  * **windows** are now widgets
  * checkboxes
  * radio buttons
  * image widget
  * align-text property with :left :center :right
  * each widget needs to be assigned to a window key in order for it to be drawn
  * changed widgets **:args** key to **:props** key (properties of a widget)
  * copy and paste via ctrl+c and ctrl+v for **input fields**
  * :can-multiline? for multi line support of **input fields** and **buttons**
  * multi column support for **list** widgets
  * add optional header for **list** widgets
  * headers for **list** widgets can have **header actions** :sort :sort-asc :sort-desc :select-all 
    the header actions get reversed with consecutive clicks on the header :sort and :sort-asc -> :sort-desc, :select-all will get unselected

### Colors
  * rework color property (pass a map with keys :background :text :focus :select :resize)
  * made it possible to set a color theme

## 0.0.1-alpha31

* fixed severe bug when hiding, forgot to hide items which were removed

## 0.0.1-alpha30

* reworked when to hide widgets, now that double buffering is enabled and no flickering occurs,
  I can go less specific. Therefore widgets simply hide when they have changed (immutability is great!)
  this fixes a bug where widgets are updated and need to redraw completly 

## 0.0.1-alpha29

* added after-drawing function to widget protocol
* enabled double buffering via Capra, which flips at the end of the swap-widgets!, removing basically
  all flickering (who ever invented double buffering and who ever made it so easy usable in awt should get a noble prize)

## 0.0.1-alpha28

* bugfix: rendering hints not applying
* remove rendering hints on hide for performance

## 0.0.1-alpha27

* support rendering hints
* fixed a bug where widgets on top of widgets wouldn't lose their focus
* reduced flickering: widgets now only hide when they change their size or position before being drawn again
* fixed widget borders slightly to leave no trace when widgets are drawn over

## 0.0.1-alpha26 (latest)
* fixed remove-widget-group

## 0.0.1-alpha25

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
* input fields can now be marked as password fields via props :password?
* added before-drawing function to the widget protocol, which is called each time before a widget is drawn
* mouse-dragging now supported as widget-event
* new widget: list
  

## 0.0.1-alpha24

* fixed removing of widgets

## 0.0.1-alpha23

* expose mouse position in mouse related widget-events 

## 0.0.1-alpha22

* replaced clojure2d with capra

## 0.0.1-alpha21

* update function now are variadic (skip-redrawing moved to own function -> update-skip-redraw!,
  update-group-skip-redraw!)
* added global events via strigui.widget namespace
* reverted tab fix

## 0.0.1-alpha20

* support multiline labels
* fixed an issue where a tabbed widget couldn't be tabbed again when removed and then read

## 0.0.1-alpha19 

* added skip-redraw? for widget updates
* added :skip-redrawing option to widget
* fixed issue where widgets can be removed on click by the user before it can get replaced

## 0.0.1-alpha18

* fixed find-by-group! and remove-group!

## 0.0.1-alpha17

* removed unnecessary redrawing of widgets

## 0.0.1-alpha16

* converting strigui.widget/state :widgets to a hashmap instead of a vector
* fixing drawing order when widgets are above/below other widgets and 
  how it affects their neighbours
* fixed tabbing which wasn't tabbing through all tabbable widgets
* register!/unregister!/replace! can now be used without redrawing the body of the widget

## 0.0.1-alpha15

* fixes around the drawing order of widgets and their neighbouring widgets
* when the mouse moves within a widgets, it is not constantly redrawn anymore

## 0.0.1-alpha14

* some fixes to the drawing order, this probably needs a rework/different strategy
* resizing, selecting and focusing state drawing functions can be overriden when defining custom widgets

## 0.0.1-alpha13

* the entire gui can now be stored into an edn file
* widgets can now be resized with the mouse (props :can-resize?)
* widget can have one or more groups assigned
* groups can be updated/removed, which in turn will update all widgets assigned to that group
* widgets are now initialized with a width and height, depending on their displayed content
* the widget protocol provides now a "default" function, called when the widget gets created, this helps setting specific properties
* removed value and widget-name functions from widget protocol

## 0.0.1-alpha12

* load gui from an edn file (or load only a subset of widgets via an edn file)
* possibility of custom hide function
