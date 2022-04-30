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
import de.sciss.rogues.SwapRogue.{Center, Config, centers}

import java.awt.image.BufferedImage
import java.awt.{Color, RenderingHints}
import java.awt.geom.{AffineTransform, Path2D}
import javax.imageio.ImageIO
import javax.swing.JComponent
import scala.math.{Pi, cos, min, tan, round}
import scala.swing.{Dimension, Graphics2D}

object Visual {
  trait Config {
     def fps          : Int
     def fullScreen   : Boolean
     def imageIndex   : Int
     def centerIndex  : Int
     def smooth       : Boolean
     def debug        : Boolean
     def verbose      : Boolean
     def numBlades    : Int
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
class Visual(extent: Int)(implicit config: Visual.Config) {

  private val t0 = System.currentTimeMillis()
  private val canvas = new Canvas(config.fps)

  def component: JComponent = canvas.peer

  canvas.peer.setOpaque(true)
  canvas.peer.setPreferredSize(new Dimension(extent, extent))

  private val imageIndex  = config.imageIndex
  private val centerIndex = config.centerIndex

  private val imgPath       = s"images/scan$imageIndex.jpg"
  private val imgFibrePath  = s"images/fibre$imageIndex.jpg"
  // println(imgPath)
  private val img       = Visual.loadImage(imgPath)
  private val imgFibre  = Visual.loadImage(imgFibrePath)

//  println(s"imgFibre.w ${imgFibre.getWidth}, imgFibre.h ${imgFibre.getHeight}")

  private val imgW      = img.getWidth
  private val imgH      = img.getHeight
  private val fibreH    = imgFibre.getHeight
  private val radius    = extent / 2 // .0

  //    private val center = centers.find(c =>
  //      c.cx >= radius && (imgW - c.cx >= radius) &&
  //      c.cy >= radius && (imgH - c.cy >= radius)
  //    ) .getOrElse(sys.error("No suitable center"))

  private var center: Center = _

  private var focusTgtX   : Double = _
  private var focusTgtY   : Double = _

  private def mkCenter(idx: Int): Center = {
    val c0 = centers(imageIndex)(idx)
//    println(c0)
    val radiusI = radius // .ceil.toInt
    val c1 = c0.copy(cx = c0.cx.clip(radiusI, imgW - radiusI), cy = c0.cy.clip(radiusI, imgH - radiusI))
//    println(c1)
    c1
  }

  def setCenterIndex(idx: Int): Unit = {
    val _center = mkCenter(idx)
    center      = _center
    focusTgtX   = _center.cx
    focusTgtY   = _center.cy
  }


  private var dirIris     = 0
  private var dirScan     = 0
  private var dirFade     = 1
  private var tM          = t0
  private var nextDirScan = true
  private var closedIris  = true
  private var posScan     = 0.0
  private var posFade     = 0.0
  private var posIris     = 0.0

  private val RotScanSpeed  = 1.0e-6 // 2.0e-5
  private val IrisSpeed     = 2.0e-6 // 2.0e-5
  private val FadeSpeed     = 3.0e-6 // 2.0e-5
  private val WaitChange    = 3.0 * 1000

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
//  private val compositeBurn: java.awt.Composite = new com.jhlabs.composite.MultiplyComposite(1f)
  private val compositeBurn: java.awt.Composite = new com.jhlabs.composite.ColorBurnComposite(1f)

  private var state = 0   // 0 - iris, 1 - iris-to-scan, 2 - scan

  private val NumBlades   = config.numBlades // 6
  private val BladeAngle  = 2 * Pi / NumBlades
  private val BladeAngleH = BladeAngle / 2

  private val triBaseH = {
    -radius / (1.0 - (1.0 / tan(BladeAngleH))) // distance to the rotating axis, equals half base of triangle
  }

  private val irisBaseShape = {
    val p     = new Path2D.Float()
    p.moveTo(-triBaseH, -triBaseH)
    p.lineTo(0.0, radius)
    p.lineTo(+triBaseH, -triBaseH)
    p.closePath()
    p
  }

  protected def paint(g: Graphics2D, animTime: Double): Unit = {
    //      val p   = peer
    val w   = extent // p.getWidth
    val h   = extent // p.getHeight
    val cx  = radius // w * 0.5
    val cy  = radius // h * 0.5

    val animDt    = min(100.0, animTime - lastAnimTime)
    lastAnimTime  = animTime
    val wT        = animDt * speed // 0.01
    val wS        = 1.0 - wT

    val atOrig = g.getTransform

//    g.setColor(Color.blue)
//    g.fillRect(0, 0, w, h)

    if smooth then {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON         )
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE          )
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION , RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    }

    if state == 2 then {
      val focusX1   = (focusX * wS + focusTgtX * wT).clip(focusMinX, focusMaxX)
      val focusY1   = (focusY * wS + focusTgtY * wT).clip(focusMinY, focusMaxY)

      //    val dx  = focusX1 - focusX
      //    val dy  = focusY1 - focusY
      focusX  = focusX1
      focusY  = focusY1
    }

    if state > 0 then {
      val tx = focusX - focusMinX
      val ty = focusY - focusMinY

      //      val posH = position * 0.5
      val ang = cos(posScan * Pi) * Pi
      g.rotate(ang, cx, cy)
      g.translate(-tx, -ty) // cx - center.cx, cy - center.cy)
      g.drawImage(img, 0, 0, null /*peer*/)
      g.setTransform(atOrig)

      val cmpOrig = g.getComposite
      g.setComposite(compositeBurn)
      val fibreY = (animTime * 0.01).toInt % (fibreH - extent)
      g.drawImage(imgFibre, 0, 0, extent, extent, 0, fibreY, extent, fibreY + extent, null)
      g.setComposite(cmpOrig)
    }

    if state < 2 then {
      val colr = if state == 0 then Color.red else {
        val alpha = round((1.0 - posFade) * 255).toInt
        // println(s"alpha $alpha")
        new Color(0xFF0000 | (alpha << 24), true)
      }
      g.setColor(colr) // new Color(0x000000))
      g.fillRect(0, 0, w, h)

      if state == 0 then {
        val posH = posIris * 0.5
        var i = 0
        while i < NumBlades do {
          g.rotate((i - posH) * BladeAngle, cx, cy)
          g.translate(cx, 0.0) // cy * 0.105) // XXX TODO exact value
          val posTx = posIris.linLin(0.0, 1.0, 0.0, extent * 0.5773502691896257)
          val atRot = AffineTransform.getTranslateInstance(posTx, 0.0)
          val shp   = atRot.createTransformedShape(irisBaseShape)
          g.setColor(Color.black)
          g.fill(shp)
          g.setColor(Color.white)
          g.draw(shp)
          g.setTransform(atOrig)
          i += 1
        }
      }
    }

    if debug then {
      g.setColor(Color.green)
      g.drawOval(0, 0, w, h)
    }

    val t1 = System.currentTimeMillis()

    if state == 0 then {
      if dirIris == 0 then {
        if t1 - tM > WaitChange then {
          tM = t1
          dirIris = if closedIris then -1 else +1
        }
      } else {
        posIris += dirIris * (t1 - tM) * IrisSpeed
        // println(s"posIris $posIris")
        if posIris < 0.0 || posIris > 1.0 then {
          posIris = posIris.clip(0.0, 1.0)
          tM = t1
          dirIris = 0
          closedIris = !closedIris
          if closedIris then {
            state   = 1
            posFade = 0.0
            dirFade = 1
            if config.verbose then println(s"state = $state")
          }
        }
      }

    } else if state == 1 then {
      posFade += dirFade * (t1 - tM) * FadeSpeed
      // println(s"posFade $posFade")
      if posFade < 0.0 || posFade > 1.0 then {
        if dirFade == 1 then {
          state   = 2
          dirScan = 0
          posScan = 0.0
          if config.verbose then println(s"state = $state")
        } else {
          state   = 0
          dirIris = -1
          posIris = 1.0
          if config.verbose then println(s"state = $state")
        }
        tM = t1
      }

    } else if state == 2 then {
      if dirScan == 0 then {
        if t1 - tM > WaitChange then {
          tM = t1
          dirScan = if nextDirScan then -1 else +1
        }
      } else {
        posScan += dirScan * (t1 - tM) * RotScanSpeed
        if posScan < 0.0 || posScan > 1.0 then {
          posScan     = posScan.clip(0.0, 1.0)
          tM          = t1
          dirScan     = 0
          nextDirScan = !nextDirScan

//          state   = 1
//          posFade = 1.0
//          dirFade = -1
//          if config.verbose then println(s"state = $state")
        }
      }
    }
  }

  setCenterIndex(config.centerIndex)
  repaint()
}