package de.sciss.rogues

import com.fazecast.jSerialComm.SerialPort

import java.io.{FileInputStream, InputStream}
import scala.swing.{Component, Dimension, Graphics2D, Label, MainFrame, Swing}

/*  Corresponds to `capsense-binary.py`
 *
 *  Payload is two bytes using the lowest 7 bits
 *  Frame start is 0x80, frame stop ist 0x81
 */
object ReceiveSensorsHex:
  val device    = "/dev/ttyACM1" // "/dev/ttyUSB0"
  val numTouch  = 6 // 2
  val touchVals = new Array[Int](numTouch)
  final val delimiter = 0x81.toByte // 0x0A.toByte // 0x81.toByte

  def main(args: Array[String]): Unit =
    @volatile var string = "sensors: 0000, 0000"
    lazy val lb: Component = new Component {
      preferredSize = new Dimension(520, 30 + numTouch * 10)

      private var xs = new Array[Double](numTouch)
      private val w1 = 0.95
      private val w2 = 1.0 - w1

      override def paintComponent(g: Graphics2D): Unit = {
        super.paintComponent(g)
        g.drawString(string, 16, 20)
        var i = 0
        var y = 30
        while (i < numTouch) {
          val x = xs(i) * w1 + touchVals(i) / 20.0 * w2
          xs(i) = x
          g.fillRect(4, y, x.toInt, 8)
          i += 1; y += 10
        }
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
//    val in        = new FileInputStream(device)
    val (port, in) = {
      val ports = SerialPort.getCommPorts()
      val _port = ports.find(_.getSystemPortPath == device).getOrElse(sys.error(s"Device $device not found"))
      val opened = _port.openPort()
//      _port.setComPortTimeouts()
      require (opened, s"Could not open $device")
      (_port, _port.getInputStreamWithSuppressedTimeoutExceptions /*getInputStream*/)
    }
    try
      runWith(in)(fun)
    finally
      port.closePort()

  def runWith(in: InputStream)(fun: => Unit): Unit =
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
          if buf(j) == delimiter then
            if j == touchSz then
              var t = 0
              var k = 0
              while t < numTouch do
                val sHi0  = buf(k).toInt; k += 1
                val sLo0  = buf(k).toInt; k += 1
                val sHi   = sHi0 // if (sHi0 < 10) sHi0 else sHi0 - 1
                val sLo   = sLo0 // if (sLo0 < 10) sLo0 else sLo0 - 1
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
