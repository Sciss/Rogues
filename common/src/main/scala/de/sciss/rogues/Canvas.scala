/*
 *  Canvas.scala
 *  (Rogues)
 *
 *  Copyright (c) 2021-2022 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.rogues

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.JComponent
import scala.swing.Graphics2D

class Canvas(fps: Int):

  private var animFunArr    = new Array[(/*AWT*/Graphics2D, Double) => Unit](8)
  private var animFunNum    = 0
  private var animStartTime = 0L
  private var _manualMode   = false

  var manualTime = 0.0

  def peer: JComponent = _peer

  private val timer = new javax.swing.Timer(1000/fps, { _ =>
    _peer.repaint()
    _peer.getToolkit.sync()
  })

  def repaint(fun: (/*AWT*/Graphics2D, Double) => Unit): Unit = {
    if (animFunArr.length == animFunNum) {
      val a = new Array[(/*AWT*/Graphics2D, Double) => Unit](animFunNum << 1)
      System.arraycopy(animFunArr, 0, a, 0, animFunNum)
      animFunArr = a
    }
    animFunArr(animFunNum) = fun
    animFunNum += 1
    if (!timer.isRunning && !_manualMode) {
      startTimer()
    }
  }

  def manualMode: Boolean = _manualMode
  def manualMode_=(value: Boolean): Unit = if (_manualMode != value) {
    _manualMode = value
    if (value) {
      timer.stop()
    } else {
      startTimer()
    }
  }

  private def startTimer(): Unit = {
    animStartTime = System.currentTimeMillis()
    timer.restart()
  }

  private object _peer extends JComponent {
    private var buf: BufferedImage = null
    private var g2w: /*AWT*/Graphics2D = null

    override def paintComponent(g: java.awt.Graphics/*2D*/): Unit = {
      if (buf == null || buf.getWidth != getWidth || buf.getHeight != getHeight) {
        if (buf != null) buf.flush()
        buf = new BufferedImage(getWidth, getHeight, BufferedImage.TYPE_INT_ARGB)
        val gi = buf.createGraphics()
        gi.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2w = gi // if (g2w == null) new AWTGraphics2D(gi) else g2w.newPeer(gi)
      }

      //      val g2  = g.asInstanceOf[java.awt.Graphics2D]
      //      val g2w = new AWTGraphics2D(g2)
      val dt  = if (_manualMode) manualTime else (System.currentTimeMillis() - animStartTime).toDouble
      val arr = animFunArr
      var i = 0
      val n = animFunNum
      animFunNum = 0
      while (i < n) {
        val f = arr(i)
        arr(i) = null
        f(g2w, dt)
        i += 1
      }

      // g.drawImage(buf, 0, 0, this)
      drawContents(buf, g.asInstanceOf[java.awt.Graphics2D])
    }
  }

  protected def drawContents(img: BufferedImage, target: java.awt.Graphics2D): Unit =
    target.drawImage(img, 0, 0, null)
end Canvas
