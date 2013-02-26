
# PushBorderLayout

A BorderLayout that accepts any number of components within (without requiring to nest JPanels).
Scala version inspired by the [Java version](http://github.com/twitwi/PushBorderLayout).

See also a [quick post on it](http://home.heeere.com/tech-push-border-layout.html).

Example use:

    ...
    def main(args: Array[String]) {
      val f = new JFrame("PushBorderLayout !")
      val c = f.getContentPane
  
      import PushBorderLayout._
      c.setLayout(new PushBorderLayout())
      // one by one
      c.add(newComponent, PushBorderLayout.LINE_START)
      c.add(newComponent, PushBorderLayout.LINE_START)
      // many at once
      add(c,
        LINE_END   -> newComponent,
        PAGE_START -> newComponent,
        LINE_START -> newComponent,
        PAGE_END   -> newComponent,
        PAGE_END   -> pad(10),
        PAGE_END   -> newComponent,
        LINE_END   -> newComponent,
        LINE_END   -> pad(10),
        CENTER     -> newComponent)
      f.pack
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
      f.setVisible(true)
    }
