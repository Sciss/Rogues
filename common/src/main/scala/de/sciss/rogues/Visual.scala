/*
 *  Visual.scala
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

import de.sciss.file.file
import de.sciss.numbers.Implicits.*
import de.sciss.rogues.SwapRogue.{Config, centers}

import java.awt.image.BufferedImage
import java.awt.{Color, RenderingHints}
import javax.imageio.ImageIO
import javax.swing.JComponent
import scala.math.min
import scala.swing.{Dimension, Graphics2D}

object Visual {
  trait Config {
     def fps          : Int
     def fullScreen   : Boolean
     def imageIndex   : Int
     def centerIndex  : Int
     def smooth       : Boolean
     def debug        : Boolean
  }

  def loadImage(path: String): BufferedImage = {
    val peer0 = ImageIO.read(file(path))
    val peer = if (peer0.getType == BufferedImage.TYPE_INT_ARGB) peer0 else {
      val b = new BufferedImage(peer0.getWidth, peer0.getHeight, BufferedImage.TYPE_INT_ARGB)
      val g = b.createGraphics()
      g.drawImage(peer0, 0, 0, null)
      g.dispose()
      b
    }
    peer
  }
}
class Visual(extent: Int)(implicit config: Visual.Config): //, imageIndex: Int, centerIndex: Int, smooth: Boolean, debug: Boolean):

  private val t0 = System.currentTimeMillis()
  private val canvas = new Canvas(config.fps)

  def component: JComponent = canvas.peer

  canvas.peer.setOpaque(true)
  canvas.peer.setPreferredSize(new Dimension(extent, extent))

  private val imageIndex  = config.imageIndex
  private val centerIndex = config.centerIndex

  private val imgPath       = s"images/scan$imageIndex.jpg"
  private val imgFibrePath  = s"images/fibre4.jpg"
  // println(imgPath)
  private val img       = Visual.loadImage(imgPath)
  private val imgFibre  = Visual.loadImage(imgFibrePath)

  println(s"imgFibre.w ${imgFibre.getWidth}, imgFibre.h ${imgFibre.getHeight}")

  private val imgW      = img.getWidth
  private val imgH      = img.getHeight
  private val radius    = extent / 2 // .0

  //    private val center = centers.find(c =>
  //      c.cx >= radius && (imgW - c.cx >= radius) &&
  //      c.cy >= radius && (imgH - c.cy >= radius)
  //    ) .getOrElse(sys.error("No suitable center"))

  private val center = {
    val c0 = centers(imageIndex)(centerIndex)
    println(c0)
    val radiusI = radius // .ceil.toInt
    val c1 = c0.copy(cx = c0.cx.clip(radiusI, imgW - radiusI), cy = c0.cy.clip(radiusI, imgH - radiusI))
    println(c1)
    c1
  }

  private var focusTgtX   : Double        = center.cx
  private var focusTgtY   : Double        = center.cy

  private var direction   = 0
  private var tM          = t0
  private var closed      = true
  private var position    = 0.0

  private val RotSpeed    = 1.0e-6 // 2.0e-5
  private val WaitChange  = 3.0 * 1000

  private val focusMinX = radius // canvasWH
  private val focusMinY = radius // canvasHH
  private var focusMaxX   : Int           = imgW - radius
  private var focusMaxY   : Int           = imgH - radius

  private var focusX      : Double = focusMinX
  private var focusY      : Double = focusMinY
  private var lastAnimTime  = 0.0
  private var speed         = 0.002

  def repaint(): Unit =
    canvas.repaint(repaint) // dom.window.requestAnimationFrame(repaint)

  // animTime is in milliseconds
  def repaint(ctx: Graphics2D /*Ctx*/, animTime: Double): Unit = {
    paint(ctx, animTime)
    //      sendTrunkXY()
    //      sendSensors()
    repaint()
  }

  private val smooth = config.smooth
  private val debug  = config.debug

  private val compositeNormal = java.awt.AlphaComposite.SrcOver
//  private val compositeBurn   = new ColorBurnComposite(1f)
  private val compositeBurn: java.awt.Composite = new com.jhlabs.composite.MultiplyComposite(1f)

  protected def paint(g: Graphics2D, animTime: Double): Unit =
    //      val p   = peer
    val w   = extent // p.getWidth
    val h   = extent // p.getHeight
    val cx  = radius // w * 0.5
    val cy  = radius // h * 0.5

    val animDt    = min(100.0, animTime - lastAnimTime)
    lastAnimTime  = animTime
    val wT        = animDt * speed // 0.01
    val wS        = 1.0 - wT
    val focusX1   = (focusX * wS + focusTgtX * wT).clip(focusMinX, focusMaxX)
    val focusY1   = (focusY * wS + focusTgtY * wT).clip(focusMinY, focusMaxY)

    val dx  = focusX1 - focusX
    val dy  = focusY1 - focusY
    focusX  = focusX1
    focusY  = focusY1

    val tx = focusX - focusMinX
    val ty = focusY - focusMinY

    g.setColor(Color.blue)
    g.fillRect(0, 0, w, h)

    if (smooth) {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON         )
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE          )
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION , RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    }

    val atOrig = g.getTransform
    //      val posH = position * 0.5
    val ang = (position * math.Pi).cos * math.Pi
    g.rotate(ang, cx, cy)
    g.translate(-tx, -ty) // cx - center.cx, cy - center.cy)
    g.drawImage(img, 0, 0, null /*peer*/)
    g.setTransform(atOrig)

    if debug then
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

    val cmpOrig = g.getComposite
    g.setComposite(compositeBurn)
//    g.drawImage(imgFibre, 0, 0, null)
    val fibreY = (animTime * 0.01).toInt % (2538 - 480)
    g.drawImage(imgFibre, 0, 0, 480, 480, 0, fibreY, 480, fibreY + 480, null)
    g.setComposite(cmpOrig)

  end paint

  repaint()

end Visual