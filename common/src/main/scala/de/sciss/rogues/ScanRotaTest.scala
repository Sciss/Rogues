/*
 *  ScanRotaTest.scala
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

import de.sciss.file.*

import java.awt.{Color, RenderingHints}
import java.awt.geom.{AffineTransform, Path2D}
import java.awt.image.BufferedImage
import java.util.Timer
import scala.swing.{Component, Dimension, Graphics2D, MainFrame, Point, Swing}
import scala.swing.event.{Key, KeyPressed}
import de.sciss.numbers.Implicits.*
import org.rogach.scallop.{ScallopConf, ScallopOption as Opt}

import javax.imageio.ImageIO

object ScanRotaTest:
  case class Center(cx: Int, cy: Int, r: Int, strength: Double)

  case class Config(
                     radius       : Int     = 240,
                     xOffset      : Double  = -0.25,
                     yOffset      : Double  = 0.0,
                     margin       : Int     = 60,
                     refreshPeriod: Int     = 20,
                     fullScreen   : Boolean = false,
                   )

  val centers: Seq[Center] = Seq(
    Center(cx = 1183, cy =  916, r = 50, strength = 49.7),
    Center(cx = 1009, cy = 1528, r = 44, strength = 44.1),
    Center(cx =  307, cy = 1339, r = 80, strength = 80.0),
    Center(cx = 1482, cy = 2061, r = 57, strength = 56.8),
    Center(cx =  879, cy = 1012, r = 49, strength = 48.7),
    Center(cx =  633, cy = 1825, r = 21, strength = 21.0),
  )

  def main(args: Array[String]): Unit =
    object p extends ScallopConf(args):
      import org.rogach.scallop.*

      printedName = "ScanRota"
      private val default = Config()

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
      val fullScreen: Opt[Boolean] = toggle(default = Some(default.fullScreen),
        descrYes = "Put window into full-screen mode.",
      )

      verify()
      val config: Config = Config(
        radius       = radius       (),
        xOffset      = xOffset      (),
        yOffset      = yOffset      (),
        margin       = margin       (),
        refreshPeriod= refreshPeriod(),
        fullScreen   = fullScreen   (),
      )
    end p

    implicit val c: Config = p.config
    Swing.onEDT(run())
  end main

  /** Must be called on the EDT. */
  def run()(implicit c: Config): Unit =
    val extent  = (c.radius + c.margin) * 2
    val canvas  = new Canvas(extent = extent)

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
        title = "ScanRota"

      contents  = canvas
      pack()
      centerOnScreen()
      open()
      canvas.requestFocus()

    val t = new Timer()
    t.scheduleAtFixedRate({ () =>
      canvas.repaint()
      canvas.toolkit.sync()
    }, c.refreshPeriod.toLong, c.refreshPeriod.toLong)

  class Canvas(extent: Int)
    extends Component:

    private val t0 = System.currentTimeMillis()

    opaque = true
    preferredSize = new Dimension(extent, extent)

    private val img     = ImageIO.read(file(
      "images/IMG_0056ovr_eq_scale.jpg")
    )
    private val imgW    = img.getWidth
    private val imgH    = img.getHeight
    private val radius  = extent / 2.0

    private val center = centers.find(c =>
      c.cx >= radius && (imgW - c.cx >= radius) &&
      c.cy >= radius && (imgH - c.cy >= radius)
    ) .getOrElse(sys.error("No suitable center"))

    private var direction   = 0
    private var tM          = t0
    private var closed      = true
    private var position    = 0.0

    private val RotSpeed    = 1.0e-5 // 2.0e-5
    private val WaitChange  = 3.0 * 1000

    override protected def paintComponent(g: Graphics2D): Unit =
      val p   = peer
      val w   = p.getWidth
      val h   = p.getHeight
      val cx  = w * 0.5
      val cy  = h * 0.5

      g.setColor(Color.black)
      g.fillRect(0, 0, w, h)
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON         )
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE          )
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION , RenderingHints.VALUE_INTERPOLATION_BICUBIC)

      val atOrig = g.getTransform
//      val posH = position * 0.5
      val ang = (position * math.Pi).cos * math.Pi
      g.rotate(ang, cx, cy)
      g.translate(cx - center.cx, cy - center.cy)
      g.drawImage(img, 0, 0, peer)
      g.setTransform(atOrig)

      g.setColor(Color.green)
      g.drawOval(0, 0, w, h)

      val t1 = System.currentTimeMillis()
      if direction == 0 then
        if t1 - tM > WaitChange then
          tM = t1
          direction = if closed then -1 else +1
        end if
      else
        position += direction * (t1 - tM) * RotSpeed
        if position <= 0.0 || position >= 1.0 then
          position  = position.clip(0.0, 1.0)
          tM        = t1
          direction = 0
          closed    = !closed
        end if
      end if

    end paintComponent
