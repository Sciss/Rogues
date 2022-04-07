/*
 *  Iris.scala
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
import org.rogach.scallop.{ScallopConf, ScallopOption as Opt}

import java.awt.{Color, RenderingHints}
import java.awt.geom.{AffineTransform, Ellipse2D, Path2D}
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.Timer
import scala.math.Pi
import scala.swing.event.{Key, KeyPressed, KeyTyped}
import scala.swing.{Color, Component, Dimension, Graphics2D, Image, MainFrame, Point, Swing}

object Iris:
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

      printedName = "Iris"
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
    val extent  = (c.radius + c.margin) * 2
    val canvas  = new Canvas(extent = extent,
      cyclePeriod = c.cyclePeriod, drawEnvelope = c.drawEnvelope)

    new MainFrame:
      if c.fullScreen then
        peer.setUndecorated(true)
        canvas.cursor = java.awt.Toolkit.getDefaultToolkit.createCustomCursor(
          new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "hidden"
        )
        canvas.keys.reactions += {
          case KeyPressed(_, Key.Escape, _, _) => closeOperation()
        }
      else
        title = "Iris"

      contents  = canvas
      pack()
      centerOnScreen()
      open()
      canvas.requestFocus()

    val t = new Timer(c.refreshPeriod, _ => canvas.repaint())
    t.start()

  class Canvas(extent: Int, cyclePeriod: Double, drawEnvelope: Boolean)
    extends Component:

    private val circle      = new Ellipse2D.Double()
    private val at          = new AffineTransform()
    private val t0          = System.currentTimeMillis()

    opaque = true
    preferredSize = new Dimension(extent, extent)

    private val NumBlades   = 6
    private val BladeAngle  = 2 * Pi / NumBlades

    private val baseShape = {
      val r = new Path2D.Float()
      val scale = extent / 20f
      val y0 = -2f * scale
      val wh = 4f * scale
      val w  = 2f * wh
      val h1 = 4f * scale
      val y1 = y0 + h1
//      val h2 = 7f * scale
      val h2 = wh / math.tan(BladeAngle/2)
//      println(s"h2 = ${h2 / scale}")
      val y2 = y1 + h2
      r.moveTo(-wh, y0)
      r.lineTo(-wh, y1)
      r.lineTo(0f, y2)
      r.lineTo(wh , y1)
      r.lineTo(wh, y0)
      r.closePath()
      r
    }

    private var direction   = 0
    private var tM          = t0
    private var closed      = true
    private var rotAng      = 0.0

    private val RotSpeed    = 2.0e-5
    private val MinRot      = 0.0
    private val MaxRot      = 45 * Pi / 180
    private val WaitChange  = 4.0 * 1000

    override protected def paintComponent(g: Graphics2D): Unit =
      val p   = peer
      val w   = p.getWidth
      val h   = p.getHeight
      val cx  = w * 0.5
      val cy  = h * 0.5

      g.setColor(Color.red) // new Color(0x000000))
      g.fillRect(0, 0, w, h)
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON         )
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE          )
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION , RenderingHints.VALUE_INTERPOLATION_BICUBIC)

      val atOrig = g.getTransform
      for (i <- 0 until NumBlades) {
        g.rotate(i * BladeAngle, cx, cy)
        g.translate(cx, cy * 0.105) // XXX TODO exact value
        // val rotAng = 45 * Pi / 180
        val atRot = AffineTransform.getRotateInstance(rotAng)
        val shp = atRot.createTransformedShape(baseShape)
        g.setColor(Color.black)
        g.fill(shp)
        g.setColor(Color.white)
        g.draw(shp)
        g.setTransform(atOrig)
      }

      val t1 = System.currentTimeMillis()
      if direction == 0 then
        if t1 - tM > WaitChange then
          tM = t1
          direction = if closed then -1 else +1
        end if
      else
        rotAng += direction * (t1 - tM) * RotSpeed
        if rotAng <= MinRot || rotAng >= MaxRot then
          rotAng    = math.max(MinRot, math.min(MaxRot, rotAng))
          direction = 0
          closed    = !closed
        end if
      end if

    end paintComponent
