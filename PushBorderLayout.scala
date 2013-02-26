

package com.heeere.scalaui.layout

import java.awt.{Component,Container,Dimension,Insets,LayoutManager2,Rectangle}
import java.util.ArrayList
import javax.swing.JPanel

class PushBorderLayout extends LayoutManager2 {
  import PushBorderLayout._
  private val elements = scala.collection.mutable.ArrayBuffer.empty[Element]

  private def closed = !elements.isEmpty && elements(elements.length - 1).position == CENTER

  override def addLayoutComponent(comp: Component, pconstraints: Object) = comp.getTreeLock.synchronized {
    val constraints = if (pconstraints == null) CENTER else pconstraints
    if (closed) {
      throw new IllegalStateException("Cannot add more components to a " + this.getClass.getName + " after having added something in " + CENTER + " (or without parameter)")
    }
    if (!(constraints.isInstanceOf[Position])) {
      throw new IllegalArgumentException("Only Position constraints are allowed, found " + constraints.getClass().getCanonicalName())
    }
    if (elements.map(_.component).contains(comp)) {
      throw new IllegalArgumentException("Cannot add a given component twice, check your code.")
    }
    elements += new Element(comp, constraints.asInstanceOf[Position])
  }

  override def removeLayoutComponent(comp: Component) = comp.getTreeLock().synchronized {
    elements --= elements.filter(_.component == comp).take(1)
  }

  override def layoutContainer(target: Container) = target.getTreeLock().synchronized {
    val insets = target.getInsets
    val available = new Rectangle(
      insets.left,
      insets.top,
      target.getWidth() - insets.right - insets.left,
      target.getHeight() - insets.bottom - insets.top)
    val ltr = target.getComponentOrientation().isLeftToRight
    for (e <- elements) {
      val c = e.component
      var pref: Dimension = null
      e.position.asLeftToRight(ltr) match {
        case LINE_START => // left
          c.setSize(c.getWidth, available.height)
          pref = c.getPreferredSize
          c.setBounds(available.x, available.y, pref.width, available.height)
          available.x += pref.width
          available.width -= pref.width
        case LINE_END => // right
          c.setSize(c.getWidth, available.height)
          pref = c.getPreferredSize
          c.setBounds(available.x + available.width - pref.width, available.y, pref.width, available.height)
          available.width -= pref.width
        case PAGE_START => // top
          c.setSize(available.width, c.getHeight)
          pref = c.getPreferredSize
          c.setBounds(available.x, available.y, available.width, pref.height)
          available.y += pref.height
          available.height -= pref.height
        case PAGE_END => // down
          c.setSize(available.width, c.getHeight)
          pref = c.getPreferredSize
          c.setBounds(available.x, available.y + available.height - pref.height, available.width, pref.height)
          available.height -= pref.height
        case CENTER =>
          c.setBounds(available.x, available.y, available.width, available.height)
      }
    }
  }

  override def minimumLayoutSize(target: Container): Dimension = new Dimension(0, 0)

  override def preferredLayoutSize(target: Container): Dimension = target.getTreeLock.synchronized {
    val usedSize = new Dimension()
    val minimalInsideSize = new Dimension()

    var sanityCheckStopped = false
    val ltr = target.getComponentOrientation().isLeftToRight
    for (e <- elements) {
      if (sanityCheckStopped) {
        throw new IllegalStateException("Internal illegal state, wrong components order.")
      }
      val c = e.component
      val cDim = c.getPreferredSize()
      e.position.asLeftToRight(ltr) match {
        case CENTER =>
          sanityCheckStopped = true
          usedSize.width += cDim.width
          usedSize.height += cDim.height
        case LINE_START | LINE_END =>
          usedSize.width += cDim.width
          minimalInsideSize.width = math.max(0, minimalInsideSize.width - cDim.width)
          minimalInsideSize.height = math.max(minimalInsideSize.height, cDim.height)
        case PAGE_START | PAGE_END =>
          usedSize.height += cDim.height
          minimalInsideSize.width = math.max(minimalInsideSize.width, cDim.width)
          minimalInsideSize.height = math.max(0, minimalInsideSize.height - cDim.height)
      }
    }

    usedSize.width += minimalInsideSize.width
    usedSize.height += minimalInsideSize.height

    val insets = target.getInsets
    usedSize.width += insets.left + insets.right
    usedSize.height += insets.top + insets.bottom
    usedSize
  }

  override def maximumLayoutSize(target: Container) = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)

  override def getLayoutAlignmentX(target: Container) = 0.5f

  override def getLayoutAlignmentY(target: Container) = 0.5f

  override def invalidateLayout(target: Container) = {}

  override def addLayoutComponent(name: String, comp: Component): Unit = throw new UnsupportedOperationException()

}

object PushBorderLayout {

  abstract sealed class Position {
    def asLeftToRight(leftToRight: Boolean) = {
      if (leftToRight) this
      else this match {
        case LINE_START => PAGE_START
        case LINE_END => PAGE_END
        case PAGE_START => LINE_START
        case PAGE_END => LINE_END
        case CENTER => CENTER
      }
    }
  }

  case object LINE_END extends Position
  case object LINE_START extends Position
  case object PAGE_START extends Position
  case object PAGE_END extends Position
  case object CENTER extends Position

  def pad(size: Int) = {
    val p = new JPanel()
    p.setPreferredSize(new Dimension(size, size))
    p
  }

  def add(c: Container, content: (Position, Component)*) = {
    if (!c.getLayout.isInstanceOf[PushBorderLayout]) {
      throw new IllegalArgumentException("In 'add', passed container should have a PushBorderLayout set to it")
    }
    for ((p,comp) <- content) c.add(comp, p)
  }

  class Element(val component: Component, val position: Position)
}

object TestPushBorderLayout {
  import javax.swing.{JFrame,JLabel}
  import javax.swing.border.{CompoundBorder,EmptyBorder,LineBorder}
  import java.awt.Color

  // component generator
  var i = 0
  def newComponent = {
    val res = new JLabel("Comp " + i)
    i += 1
    res.setBorder(new CompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(4, 4, 4, 4)))
    res
  }

  def main(args: Array[String]) {
    val f = new JFrame("PushBorderLayout !")
    val c = f.getContentPane

    import PushBorderLayout._
    c.setLayout(new PushBorderLayout())
    c.add(newComponent, PushBorderLayout.LINE_START)
    c.add(newComponent, PushBorderLayout.LINE_START)
    // scala specific
    add(c,
      LINE_END -> newComponent,
      PAGE_START -> newComponent,
      LINE_START -> newComponent,
      PAGE_END -> newComponent,
      PAGE_END -> pad(10),
      PAGE_END -> newComponent,
      LINE_END -> newComponent,
      LINE_END -> pad(10),
      CENTER -> newComponent)
    f.pack
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    f.setVisible(true)
  }
}
