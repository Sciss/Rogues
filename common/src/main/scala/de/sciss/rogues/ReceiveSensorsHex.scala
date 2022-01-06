package de.sciss.rogues

import java.io.FileInputStream
import scala.swing.{Component, Dimension, Graphics2D, Label, MainFrame, Swing}

/*  Corresponds to `capsense-binary.py`
 *
 *  Payload is two bytes using the lowest 7 bits
 *  Frame start is 0x80, frame stop ist 0x81
 */
object ReceiveSensorsHex:
  val device    = "/dev/ttyUSB0"
  val numTouch  = 2
  val touchVals = new Array[Int](numTouch)

  def main(args: Array[String]): Unit =
    @volatile var string = "sensors: 0000, 0000"
    lazy val lb: Component = new Component {
      preferredSize = new Dimension(520, 50)

      private var x1 = 0.0
      private var x2 = 0.0
      private val w1 = 0.95
      private val w2 = 1.0 - w1

      override def paintComponent(g: Graphics2D): Unit = {
        super.paintComponent(g)
        g.drawString(string, 16, 20)
        x1 = x1 * w1 + touchVals(0) / 20.0 * w2
        x2 = x2 * w1 + touchVals(1) / 20.0 * w2
        g.fillRect(4, 30, x1.toInt, 8)
        g.fillRect(4, 40, x2.toInt, 8)
      }
    }
    Swing.onEDT {
      val f = new MainFrame
      f.contents = lb
      f.pack().centerOnScreen()
      f.open()
    }
    run {
      val s = touchVals.mkString("sensors: ", ", ", "")
      // println(s)
      string = s
      lb.repaint()
      lb.toolkit.sync()
//      Swing.onEDT {
//        lb.text = s
//      }
    }

  def run(fun: => Unit): Unit =
    val in        = new FileInputStream(device)
    val touchSz   = numTouch * 2
    val bufSz     = (touchSz + 2) * 2
    println(s"bufSz = $bufSz")
    val buf       = new Array[Byte](bufSz)
    var n         = 0
    var start     = false
    var stop      = false
    while true do
      val m = in.read(buf, n, bufSz - n)
      if m == -1 then {
        println("Stream terminated")
        return
      }

      n += m
      // println(s"n = $n")

      if !start then
        var i = 0
        while !start && i < n do
          val j = i
          i += 1
          if buf(j) == 0x80.toByte then
            System.arraycopy(buf, i, buf, 0, n - i)
            start = true
            // off  -= i
            n    -= i

      if start then
        var i = 0
        while !stop && i < n do
          val j = i
          i += 1
          if buf(j) == 10 /*0x81*/.toByte then
            if j == touchSz then
              var t = 0
              var k = 0
              while t < numTouch do
                val sHi0  = buf(k).toInt; k += 1
                val sLo0  = buf(k).toInt; k += 1
                val sHi   = if (sHi0 < 10) sHi0 else sHi0 - 1
                val sLo   = if (sLo0 < 10) sLo0 else sLo0 - 1
                val sense = (sHi << 7) | sLo
                touchVals(t) = sense
                t += 1

              fun

            else
              println("drop out")

            System.arraycopy(buf, i, buf, 0, n - i)
            stop  = true
            // off  -= i
            n    -= i

      if stop then
        start = false
        stop  = false
        // println(s"n $n")
