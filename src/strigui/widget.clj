(ns strigui.widget)

(defprotocol Widget 
    "collection of functions around redrawing widgets, managing the border etc. ..."
    (coord [this] "gets the coordinates of the widget")
    (value [this] "the current text of the widget")
    (args [this] "the current args of the widget")
    (widget-name [this] "name of the widget")
    (draw [this canvas] "draw the widget, returns the widget on success")
    (redraw [this canvas] "redraw the widget")
    (hide [this canvas] "removes the widget from the canvas"))

