package de.sciss.rogues

import com.fazecast.jSerialComm.SerialPort

import java.io.{BufferedReader, FileInputStream, InputStream, InputStreamReader}
import scala.swing.{Component, Dimension, Graphics2D, Label, MainFrame, Swing}

/*  Corresponds to `swap_swap_barn.py`.
 *
 *  Reads ASCII text formatted lines of sensor 16-bit sensor values separated by space characters
 */
object ReceiveLDRText:
  val device      = "/dev/ttyACM0" // "/dev/ttyUSB0"
  val numSensors  = 6 // 2
  val sensorVals  = new Array[Int](numSensors)

  def main(args: Array[String]): Unit =
    lazy val lb: Component = new Component {
      preferredSize = new Dimension(520, 30 + numSensors * 10)

      private val xs = new Array[Double](numSensors)
      private val w1 = 0.95
      private val w2 = 1.0 - w1

      override def paintComponent(g: Graphics2D): Unit = {
        super.paintComponent(g)
        var i = 0
        var y = 30
        while (i < numSensors) {
          val x = xs(i) * w1 + sensorVals(i) / 64.0 * w2
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
      val s = sensorVals.mkString("sensors: ", ", ", "")
      lb.repaint()
      lb.toolkit.sync()
    }

  def run(fun: => Unit): Unit =
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
    val reader = new BufferedReader(new InputStreamReader(in))
    while true do
//      while in.available() == 0 do
//        Thread.sleep(1)

      val sArr = reader.readLine().trim.split(' ')
      if sArr.length == numSensors then
        var t = 0
        while t < numSensors do
          val s = sArr(t)
          var i = 0
          var sensorVal = 0
          var ok = true
          while ok && i < s.length do
            val c = s.charAt(i).toInt - 48
            if c >= 0 && c <= 9 then
              sensorVal = sensorVal * 10 + c
            else
              ok = false

          if ok then sensorVals(t) = sensorVal
          t += 1

        fun
