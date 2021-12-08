/*
 *  SteinerChain.scala
 *  (Rogues)
 *
 *  Copyright (c) 2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.rogues

import de.sciss.numbers.Implicits.*

import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import javax.swing.Timer
import scala.swing.{Color, Component, Dimension, Graphics2D, MainFrame, Swing}

object SteinerChain:
  def main(args: Array[String]): Unit = Swing.onEDT(run())

  private val chain = new Chain(numCircles = 3 /*7*/, radius = 240.0, xOffset = -0.25)
  
  def run(): Unit =
    new MainFrame:
      title     = "Steiner Chain"
      contents  = Canvas
      size      = new Dimension(600, 600)
      centerOnScreen()
      open()

    val t = new Timer(20, _ => Canvas.repaint())
    t.start()

  object Canvas extends Component:
    private val circle      = new Ellipse2D.Double()
    private val t0          = System.currentTimeMillis()
    private val period      = 10.0 // seconds per cycle

    opaque = true

    override protected def paintComponent(g: Graphics2D): Unit =
      val p   = peer
      val w   = p.getWidth
      val h   = p.getHeight
      val cx  = w * 0.5
      val cy  = h * 0.5

      g.setColor(new Color(0x000000))
      g.fillRect(0, 0, w, h)
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON )
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE  )

      g.translate(cx, cy)

      g.setColor(new Color(0x333333))
      chain.setOuterCircle(circle)
      g.draw(circle)

      g.setColor(new Color(0xFF6666))
      chain.setInnerCircle(circle)
      g.draw(circle)

      var i = 0
      val n = chain.numCircles

      val t1      = System.currentTimeMillis()
      val dt      = (t1 - t0) * 0.001
      val angle   = (dt / period) % 1.0 * (2 * math.Pi)

      g.setColor(new Color(0x999999))
      while i < n do
        chain.setChainCircle(circle, index = i, angle = angle)
        g.draw(circle)
        i += 1
    end paintComponent

  class Chain(val numCircles: Int, radius: Double, xOffset: Double = 0.0, yOffset: Double = 0.0):
    require (numCircles >= 3)

    private val a     = (math.sqrt((2 * yOffset).squared + (2 * xOffset).squared + 1.0) + 1.0) * 0.5
    private val b     = math.Pi / numCircles
    private val sinB  = math.sin(b)
    private val c     = a * sinB / (1.0 - sinB)
    private val d     = a + c

    def setOuterCircle(circle: Ellipse2D): Unit =
      circle.setFrameFromCenter(0.0, 0.0, radius, radius)

    def setInnerCircle(circle: Ellipse2D): Unit =
      val cr = a + 2 * c
      inverseCircle(xOffset, yOffset, cr, circle)

    def setChainCircle(circle: Ellipse2D, index: Int, angle: Double = 0.0): Unit =
      val phase = index * 2 * b + angle
      val cx = xOffset + d * math.cos(phase)
      val cy = yOffset + d * math.sin(phase)
      val cr = c
      inverseCircle(cx, cy, cr, circle)

    private def inverseCircle(cx: Double, cy: Double, cr: Double, circle: Ellipse2D): Unit =
      val ci  = 1.0 / (cx.squared + cy.squared - cr.squared)
      val cxi = (cx * ci + xOffset / a) * radius
      val cyi = (cy * ci + yOffset / a) * radius
      val ri  = cr * ci * radius
      circle.setFrameFromCenter(cxi, cyi, cxi + ri, cyi + ri)

  end Chain
