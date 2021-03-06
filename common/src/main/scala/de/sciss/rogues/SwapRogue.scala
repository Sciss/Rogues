/*
 *  SwapRogue.scala
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

import java.awt.event.{KeyAdapter, KeyEvent}
import math.{max, min, abs}
import javax.imageio.ImageIO
import javax.swing.JComponent
import scala.util.Random

object SwapRogue:
  case class Center(cx: Int, cy: Int, r: Int, strength: Double)

  case class Config(
                     radius       : Int     = 240,
                     numBlades    : Int     = 6,
                     margin       : Int     = 0,
                     fps          : Int     = 50,
                     fullScreen   : Boolean = false,
                     imageIndex   : Int     = 51,
                     centerIndex  : Int     = 0,
                     smooth       : Boolean = false,
                     verbose      : Boolean = false,
                     debug        : Boolean = false,
                     debugSensors : Boolean = false,
                     isLaptop     : Boolean = false,
                     ldrThresh    : Int     = 100,
                     ldrNoise     : Int     =  10,
                     capThresh    : Int     = 200,
                     capNoise     : Int     =   5,
                   ) extends Visual.Config

  val centers: Map[Int, Seq[Center]] = Map(
    47 -> Seq(
      Center(cx = 1436, cy =  695, r =  98, strength =  97.9),
      Center(cx =  133, cy = 1947, r =  43, strength =  42.7),
      Center(cx =   39, cy = 1869, r =  47, strength =  46.7),
      Center(cx =  419, cy = 1328, r =  27, strength =  26.6),
      Center(cx =  308, cy = 1241, r =  85, strength =  84.7),
      Center(cx =  307, cy =  415, r =  43, strength =  43.3),
      Center(cx = 1105, cy =  682, r =  34, strength =  33.5),
      Center(cx =  880, cy = 2825, r =  30, strength =  29.7),
      Center(cx = 1275, cy =  481, r =  29, strength =  28.8),
      Center(cx =  102, cy = 2234, r =  21, strength =  21.0),
    ),
    49 -> Seq(
      Center(cx = 1716, cy = 2063, r = 114, strength = 113.6),
      Center(cx = 1845, cy = 1797, r =  62, strength =  61.8),
      Center(cx = 1610, cy =  754, r =  69, strength =  68.5),
      Center(cx = 1714, cy = 2235, r =  61, strength =  60.6),
      Center(cx = 1712, cy =  759, r =  61, strength =  60.7),
      Center(cx =  323, cy = 2284, r =  46, strength =  46.3),
    ),
    51 -> Seq(
      Center(cx = 1040, cy =  461, r =  51, strength =  50.7),
      Center(cx =  884, cy = 1177, r =  58, strength =  57.7),
      Center(cx =  852, cy =  923, r =  49, strength =  48.6),
      Center(cx =  902, cy = 2759, r =  60, strength =  59.8),
      Center(cx =  676, cy = 2150, r =  34, strength =  33.6),
      Center(cx = 1115, cy = 1905, r =  48, strength =  47.9),
    ),
    52 -> Seq(
      Center(cx = 1568, cy = 2135, r = 165, strength = 165.3),
      Center(cx = 1663, cy = 2086, r =  98, strength =  98.3),
      Center(cx = 1308, cy =  950, r =  79, strength =  78.6),
      Center(cx = 1693, cy = 2231, r =  95, strength =  95.4),
      Center(cx = 1633, cy = 2629, r =  55, strength =  54.7),
      Center(cx = 1730, cy =  683, r =  80, strength =  79.9),
      Center(cx = 1558, cy = 1992, r =  60, strength =  59.9),
      Center(cx = 1567, cy = 1854, r =  98, strength =  97.7),
      Center(cx = 1666, cy = 1962, r =  51, strength =  50.5),
      Center(cx = 1682, cy =  898, r =  28, strength =  27.9),
      Center(cx = 1855, cy = 1257, r =  66, strength =  65.5),
      Center(cx = 1343, cy = 1064, r =  78, strength =  78.2),
      Center(cx = 1129, cy =  241, r =  59, strength =  58.7),
      Center(cx = 1878, cy =  992, r =  49, strength =  48.9),
    ),
    53 -> Seq(
      Center(cx = 1447, cy = 2469, r = 176, strength = 176.3),
      Center(cx =  993, cy = 2035, r =  90, strength =  89.7),
      Center(cx = 1219, cy = 2538, r = 171, strength = 170.8),
      Center(cx =  623, cy = 1852, r =  58, strength =  57.9),
      Center(cx = 1076, cy = 1141, r =  26, strength =  25.8),
      Center(cx = 1327, cy = 2694, r =  66, strength =  65.8),
    ),
    54 -> Seq(
      Center(cx =  563, cy =  625, r =  70, strength =  69.9),
      Center(cx =  772, cy =  532, r = 119, strength = 118.7),
      Center(cx =  767, cy =  392, r =  55, strength =  54.8),
      Center(cx =  657, cy =  506, r =  47, strength =  47.4),
      Center(cx =  740, cy = 2068, r =  15, strength =  14.7),
      Center(cx = 1261, cy = 1123, r =  51, strength =  50.5),
      Center(cx =  884, cy =  812, r =  79, strength =  78.7),
    ),
    55 -> Seq(
      Center(cx =  935, cy = 1556, r =  59, strength =  59.4),
      Center(cx =  140, cy = 2912, r =  50, strength =  49.6),
      Center(cx =  188, cy = 2126, r =  50, strength =  49.9),
      Center(cx = 1767, cy =  969, r =  51, strength =  50.8),
      Center(cx =  580, cy =  918, r =  39, strength =  39.0),
      Center(cx =  976, cy =  290, r =  49, strength =  48.9),
      Center(cx = 1017, cy = 1350, r =  46, strength =  46.3),
      Center(cx = 1482, cy = 2426, r =  41, strength =  40.8),
    ),
    56 -> Seq(
      Center(cx = 1183, cy =  916, r =  50, strength =  49.7),
      Center(cx = 1009, cy = 1528, r =  44, strength =  44.1),
      Center(cx =  307, cy = 1339, r =  80, strength =  80.0),
      Center(cx = 1482, cy = 2061, r =  57, strength =  56.8),
      Center(cx =  879, cy = 1012, r =  49, strength =  48.7),
      Center(cx =  633, cy = 1825, r =  21, strength =  21.0),
    ),
    57 -> Seq(
      Center(cx = 1112, cy = 2059, r = 102, strength = 101.9),
      Center(cx =  730, cy = 2421, r = 108, strength = 107.9),
      Center(cx =  847, cy = 2556, r =  79, strength =  78.9),
      Center(cx =  778, cy = 1651, r =  24, strength =  23.6),
      Center(cx =  727, cy = 2060, r =  68, strength =  68.1),
      Center(cx = 1008, cy = 1145, r =  22, strength =  21.8),
    ),
    58 -> Seq(
      Center(cx = 1277, cy =  635, r =  97, strength =  96.8),
      Center(cx = 1187, cy =  315, r =  91, strength =  91.0),
      Center(cx = 1325, cy =  526, r =  78, strength =  77.8),
      Center(cx = 1367, cy =  329, r =  92, strength =  92.4),
      Center(cx = 1322, cy =  799, r =  97, strength =  96.8),
      Center(cx = 1465, cy =  367, r =  53, strength =  52.8),
      Center(cx = 1320, cy =  215, r =  59, strength =  59.2),
      Center(cx =  360, cy =  400, r =  22, strength =  21.7),
    ),
    59 -> Seq(
      Center(cx =  526, cy =  601, r =  96, strength =  96.4),
      Center(cx = 1322, cy = 1152, r =  94, strength =  93.8),
      Center(cx = 1288, cy = 1776, r =  78, strength =  77.6),
      Center(cx = 1329, cy = 1999, r = 108, strength = 107.8),
      Center(cx =  658, cy = 2170, r =  68, strength =  67.8),
      Center(cx =  446, cy =   47, r =  45, strength =  44.9),
      Center(cx =  995, cy = 2128, r =  56, strength =  55.8),
      Center(cx =  546, cy = 1057, r =  29, strength =  28.9),
      Center(cx = 1307, cy = 1015, r =  46, strength =  45.9),
      Center(cx =  559, cy = 1848, r =  50, strength =  49.9),
      Center(cx = 1268, cy =  689, r =  46, strength =  46.3),
      Center(cx = 1154, cy = 1675, r =  32, strength =  31.9),
      Center(cx =  668, cy = 2473, r =  29, strength =  28.9),
      Center(cx =  463, cy = 2142, r =  24, strength =  23.6),
      Center(cx =  985, cy =  812, r =  23, strength =  23.0),
      Center(cx = 1233, cy = 1951, r =  36, strength =  35.7),
      Center(cx =  350, cy =  418, r =  41, strength =  40.8),
      Center(cx =  756, cy = 2252, r =  31, strength =  30.7),
      Center(cx = 1040, cy = 1371, r =  19, strength =  18.5),
      Center(cx =  775, cy = 2047, r =  19, strength =  19.4),
    ),
    60 -> Seq(
      Center(cx = 1131, cy =  560, r =  34, strength =  34.4),
      Center(cx =  810, cy = 2076, r =  36, strength =  35.8),
      Center(cx =  933, cy = 2172, r =  27, strength =  26.6),
      Center(cx =  640, cy = 1849, r =  27, strength =  26.8),
      Center(cx =  936, cy = 1927, r =  27, strength =  26.6),
      Center(cx =  860, cy = 1763, r =  31, strength =  31.0),
    ),
    63 -> Seq(
      Center(cx =  253, cy = 1211, r = 158, strength = 157.6),
      Center(cx = 1544, cy =  160, r = 131, strength = 130.9),
      Center(cx =  344, cy = 2359, r =  53, strength =  52.7),
      Center(cx =  439, cy =  696, r =  31, strength =  31.4),
      Center(cx = 1067, cy =   60, r =  27, strength =  26.8),
      Center(cx = 1362, cy = 1438, r =  32, strength =  32.2),
      Center(cx = 1090, cy = 1430, r =  25, strength =  25.2),
    )
  )

  def main(args: Array[String]): Unit = {
    object p extends ScallopConf(args):
      import org.rogach.scallop.*

      printedName = "ScanRota"
      private val default = Config()

      val imageIndex: Opt[Int] = opt(default = Some(default.imageIndex),
        descr = s"Index of rotation center element (default: ${default.imageIndex}).",
//        validate = x => x >= 0 && x < centers.size,
      )
      val centerIndex: Opt[Int] = opt(default = Some(default.centerIndex),
        descr = s"Index of rotation center element (default: ${default.centerIndex}).",
        validate = x => x >= 0, // && x < centers.size,
      )
      val radius: Opt[Int] = opt(default = Some(default.radius),
        descr = s"Envelope radius, greater than zero (default: ${default.radius}).",
        validate = x => x > 0,
      )
      val margin: Opt[Int] = opt(default = Some(default.margin),
        descr = s"Window margin in pixels (default: ${default.margin}).",
        validate = x => x >= 0,
      )
      val numBlades: Opt[Int] = opt(default = Some(default.numBlades),
        descr = s"Number of blades, 3 or larger (default: ${default.numBlades}).",
        validate = x => x >= 3
      )
      val fps: Opt[Int] = opt(default = Some(default.fps),
        descr = s"Window refresh frames-per-second (default: ${default.fps}).",
        validate = x => x > 0,
      )
      val ldrThresh: Opt[Int] = opt(default = Some(default.ldrThresh),
        descr = s"LDR change threshold (default: ${default.ldrThresh}).",
        validate = x => x > 0,
      )
      val capThresh: Opt[Int] = opt(default = Some(default.capThresh),
        descr = s"Capacitive change threshold (default: ${default.capThresh}).",
        validate = x => x > 0,
      )
      val ldrNoise: Opt[Int] = opt(default = Some(default.ldrNoise),
        descr = s"LDR noise floor divider (default: ${default.ldrNoise}).",
        validate = x => x > 0,
      )
      val capNoise: Opt[Int] = opt(default = Some(default.capNoise),
        descr = s"LDR noise floor divider (default: ${default.capNoise}).",
        validate = x => x > 0,
      )
      val fullScreen: Opt[Boolean] = toggle(default = Some(default.fullScreen),
        descrYes = "Put window into full-screen mode.",
      )
      val smooth: Opt[Boolean] = toggle(default = Some(default.smooth),
        descrYes = "Use smooth interpolation.",
      )
      val debug: Opt[Boolean] = toggle(default = Some(default.debug),
        descrYes = "Debug mode.",
      )
      val verbose: Opt[Boolean] = toggle(default = Some(default.verbose),
        descrYes = "Verbosity on.",
      )
      val debugSensors: Opt[Boolean] = toggle(default = Some(default.debugSensors),
        descrYes = "Debug sensor reception.",
      )
      val isLaptop: Opt[Boolean] = toggle(default = Some(default.isLaptop),
        descrYes = "Run on laptop without GPIO and sensors.",
      )

      verify()
      val config: Config = Config(
        imageIndex    = imageIndex    (),
        centerIndex   = centerIndex   (),
        radius        = radius        (),
        margin        = margin        (),
        numBlades     = numBlades     (),
        fps           = fps           (),
        ldrThresh     = ldrThresh     (),
        capThresh     = capThresh     (),
        ldrNoise      = ldrNoise      (),
        capNoise      = capNoise      (),
        fullScreen    = fullScreen    (),
        smooth        = smooth        (),
        debug         = debug         (),
        verbose       = verbose       (),
        debugSensors  = debugSensors  (),
        isLaptop      = isLaptop      (),
      )
    end p

    implicit val c: Config = p.config

    if !c.isLaptop then {
      // disable screen blanking
      import sys.process._
      Seq("xset", "s", "off").!
      Seq("xset", "-dpms").!
    }

    Swing.onEDT(run())
  }

  /** Must be called on the EDT. */
  def run()(implicit c: Config): Unit = {
    val extent  = (c.radius + c.margin) * 2
    val visual  = new Visual(extent = extent)

    new MainFrame {
      if c.fullScreen then {
        peer.setUndecorated(true)
        visual.component.setCursor(java.awt.Toolkit.getDefaultToolkit.createCustomCursor(
          new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "hidden"
        ))
        visual.component.addKeyListener(
          new KeyAdapter {
            override def keyPressed(e: KeyEvent): Unit =
              if (e.getKeyCode == KeyEvent.VK_ESCAPE) closeOperation()
          }
        )
      } else {
        title = "SwapRogue"
      }

      contents  = Component.wrap(visual.component)
      pack()
      centerOnScreen()
      open()
      visual.component.requestFocus()
    }

    val rnd = new Random()

    if c.isLaptop then {
      val t = new Timer()

      def randomMove(): Unit =
        val dly = rnd.between(6000, 16000)
        t.schedule({ () =>
          val idx = rnd.between(0, 6)
          Swing.onEDT {
            visual.setCenterIndex(idx)
          }
          randomMove()
        }, dly)

      randomMove()

    } else {
      implicit val sCfg: ReceiveSensors.Config = ReceiveSensors.Config(
        debug = c.debugSensors,
      )
      var ci          = c.centerIndex
      var tUpdate     = System.currentTimeMillis()
      var tDebug      = tUpdate

      val histSize    = 200 // 20 Hz rate -- 10 seconds history
      val ldrHistory  = Array.ofDim[Int](6, histSize)
      val ldrSum      = new Array[Int](6)
      var hasHistory  = false
      var histOff     = 0
      val arrOff      = 6   // 0 for LDR, 6 for cap
      val thresh      = c.capThresh // c.ldrThresh
      val noiseDiv    = c.capNoise  // c.ldrNoise
      var holdUpdate  = 2000

      val st = new Thread {
        override def run(): Unit = ReceiveSensors.run { arr =>
          val t1  = System.currentTimeMillis()

          var i = 0
          while (i < 6) {
            val h = ldrHistory(i)
            val v = arr(i + arrOff)
            if hasHistory then {
              val vOld = h(histOff)
              h(histOff) = v
              ldrSum(i) += v - vOld
            } else {
              java.util.Arrays.fill(h, v)
              ldrSum(i) = v * histSize
            }
            i += 1
          }
          histOff = (histOff + 1) % histSize
          if !hasHistory then hasHistory = true

          if t1 - tUpdate > holdUpdate then {
            var i = 0
            var mi = -1
            var mt = thresh
            while i < 6 do {
              val v = arr(i + arrOff)
              val mean = ldrSum(i)/histSize
              val noiseFloor = mean / noiseDiv
              val t = abs(v - mean)
              if (t > noiseFloor && t > mt) {
                mt = t
                mi = i
              }
              i += 1
            }

            if c.debug && t1 - tDebug > 1000 then {
              println(s"arr ${arr.slice(arrOff, arrOff + 6).mkString(", ")}")
              println(s"ldr ${ldrSum.iterator.map(_ / histSize).mkString(", ")}; mi $mi, mt $mt")
              tDebug = t1
            }

            if mi != -1 && mi != ci then {
              if c.verbose then {
                println(s"visual.setCenterIndex($mi)")
              }
              ci      = mi
              tUpdate = t1
              holdUpdate = rnd.between(1500, 4500)
              Swing.onEDT {
                visual.setCenterIndex(mi)
              }
            }
          }
        }
      }
      st.setDaemon(true)
      st.start()
    }
  }

//    val t = new Timer()
//    t.scheduleAtFixedRate({ () =>
//      visual.repaint()
//      visual.toolkit.sync()
//    }, c.refreshPeriod.toLong, c.refreshPeriod.toLong)

