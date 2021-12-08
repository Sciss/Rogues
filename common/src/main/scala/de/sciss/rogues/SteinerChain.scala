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
import org.rogach.scallop.{ScallopConf, ScallopOption => Opt}

import java.awt.RenderingHints
import java.awt.geom.{AffineTransform, Ellipse2D}
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.Timer
import scala.swing.{Color, Component, Dimension, Graphics2D, Image, MainFrame, Swing}

object SteinerChain:
  case class Config(
                   numCircles   : Int     = 3,
                   radius       : Int     = 240,
                   xOffset      : Double  = -0.25,
                   yOffset      : Double  = 0.0,
                   margin       : Int     = 60,
                   refreshPeriod: Int     = 20,
                   cyclePeriod  : Double  = 10.0,
                   fullScreen   : Boolean = false,
                   drawEnvelope : Boolean = true,
                   )

  def main(args: Array[String]): Unit =
    object p extends ScallopConf(args):
      import org.rogach.scallop.*

      printedName = "SteinerChain"
      private val default = Config()

      val numCircles: Opt[Int] = opt(default = Some(default.numCircles),
        descr = s"Number of moons, 3 or larger (default: ${default.numCircles}).",
        validate = x => x >= 3
      )
      val radius: Opt[Int] = opt(default = Some(default.radius),
        descr = s"Envelope radius, greater than zero (default: ${default.radius}).",
        validate = x => x > 0,
      )
      val xOffset: Opt[Double] = opt(default = Some(default.xOffset),
        descr = s"Rotation center horizontal offset, between -1 and +1 (default: ${default.xOffset}).",
        validate = x => x >= -1.0 && x <= +1.0,
      )
      val yOffset: Opt[Double] = opt(default = Some(default.yOffset),
        descr = s"Rotation center vertical offset, between -1 and +1 (default: ${default.yOffset}).",
        validate = x => x >= -1.0 && x <= +1.0,
      )
      val margin: Opt[Int] = opt(default = Some(default.margin),
        descr = s"Window margin in pixels (default: ${default.margin}).",
        validate = x => x >= 0,
      )
      val refreshPeriod: Opt[Int] = opt(default = Some(default.refreshPeriod),
        descr = s"Window refresh period in milliseconds (default: ${default.refreshPeriod}).",
        validate = x => x > 0,
      )
      val cyclePeriod: Opt[Double] = opt(default = Some(default.cyclePeriod),
        descr = s"Animation cycle period in seconds (default: ${default.cyclePeriod}).",
        validate = x => x > 0.0,
      )
      val fullScreen: Opt[Boolean] = toggle(default = Some(default.fullScreen),
        descrYes = "Put window into full-screen mode.",
      )
      val hideEnvelope: Opt[Boolean] = toggle(default = Some(!default.drawEnvelope),
        descrYes = "Do not draw enveloping circle.",
      )

      verify()
      val config: Config = Config(
        numCircles   = numCircles   (),
        radius       = radius       (),
        xOffset      = xOffset      (),
        yOffset      = yOffset      (),
        margin       = margin       (),
        refreshPeriod= refreshPeriod(),
        cyclePeriod  = cyclePeriod  (),
        fullScreen   = fullScreen   (),
        drawEnvelope = !hideEnvelope(),
      )
    end p

    Swing.onEDT(run(p.config))
  end main

  def run(c: Config): Unit =
    val chain   = new Chain(numCircles = c.numCircles, radius = c.radius.toDouble,
      xOffset = c.xOffset, yOffset = c.yOffset)
    val uriMoon = getClass.getResource("/moon512px.png")
    val imgMoon = ImageIO.read(uriMoon)
    val extent  = (c.radius + c.margin) * 2
    val canvas  = new Canvas(imgMoon, chain, extent = extent,
      cyclePeriod = c.cyclePeriod, drawEnvelope = c.drawEnvelope)

    new MainFrame:
      if c.fullScreen then
        peer.setUndecorated(true)
      else
        title = "Steiner Chain"

      contents  = canvas
      pack()
      centerOnScreen()
      open()

    val t = new Timer(c.refreshPeriod, _ => canvas.repaint())
    t.start()

  class Canvas(imgMoon: BufferedImage, chain: Chain, extent: Int, cyclePeriod: Double, drawEnvelope: Boolean)
    extends Component:

    private val circle      = new Ellipse2D.Double()
    private val at          = new AffineTransform()
    private val t0          = System.currentTimeMillis()
    private val chainRadii  = Array.tabulate(chain.numCircles)(chain.chainCircle(_).r)
    private val minChR      = chainRadii.min
    private val maxChR      = chainRadii.max

    opaque = true
    preferredSize = new Dimension(extent, extent)

    override protected def paintComponent(g: Graphics2D): Unit =
      val p   = peer
      val w   = p.getWidth
      val h   = p.getHeight
      val cx  = w * 0.5
      val cy  = h * 0.5

      g.setColor(new Color(0x000000))
      g.fillRect(0, 0, w, h)
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON         )
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE          )
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION , RenderingHints.VALUE_INTERPOLATION_BICUBIC)

      g.translate(cx, cy)

      if drawEnvelope then
        g.setColor(new Color(0x333333))
        val cOut = chain.outerCircle
        cOut.set(circle)
        g.draw(circle)

//      g.setColor(new Color(0xFF6666))
//      val cIn = chain.innerCircle
//      cIn.set(circle)
//      g.draw(circle)

      var i = 0
      val n = chain.numCircles

      val t1      = System.currentTimeMillis()
      val dt      = (t1 - t0) * 0.001
      val angle0  = (dt / cyclePeriod) % 1.0 * (2 * math.Pi)
      val angle   = math.sin(angle0) * math.Pi

//      g.setColor(new Color(0xFF0000))
      while i < n do
        if true /*i % 2 == 0*/ then
          val cChain = chain.chainCircle(index = i, angle = angle)
          val scale = cChain.r / 256.0
          at.setToIdentity()
          at.translate(cChain.x, cChain.y)
          at.rotate(-angle)
          at.scale(scale, scale)
          at.translate(-256.0, -256.0)
          g.drawImage(imgMoon, at, p)
          cChain.set(circle)
          val alpha = cChain.r.linLin(minChR, maxChR, 0, 200).toInt.clip(0, 255)
          g.setColor(new Color(0, 0, 0, alpha))
          g.fill(circle)
        i += 1
    end paintComponent

  case class Circle(x: Double, y: Double, r: Double):
    def set(e: Ellipse2D): Unit =
      e.setFrameFromCenter(x, y, x + r, y + r)

  class Chain(val numCircles: Int, radius: Double, xOffset: Double = 0.0, yOffset: Double = 0.0):
    require (numCircles >= 3)

    private val a     = (math.sqrt((2 * yOffset).squared + (2 * xOffset).squared + 1.0) + 1.0) * 0.5
    private val b     = math.Pi / numCircles
    private val sinB  = math.sin(b)
    private val c     = a * sinB / (1.0 - sinB)
    private val d     = a + c

    def outerCircle: Circle = Circle(0.0, 0.0, radius)

    def innerCircle: Circle =
      val cr = a + 2 * c
      inverseCircle(xOffset, yOffset, cr)

    def chainCircle(index: Int, angle: Double = 0.0): Circle =
      val phase = index * 2 * b + angle
      val cx = xOffset + d * math.cos(phase)
      val cy = yOffset + d * math.sin(phase)
      val cr = c
      inverseCircle(cx, cy, cr)

    private def inverseCircle(cx: Double, cy: Double, cr: Double): Circle =
      val ci  = 1.0 / (cx.squared + cy.squared - cr.squared)
      val cxi = (cx * ci + xOffset / a) * radius
      val cyi = (cy * ci + yOffset / a) * radius
      val ri  = cr * ci * radius
      Circle(cxi, cyi, ri)

  end Chain
