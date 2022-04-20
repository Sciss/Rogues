package de.sciss.rogues

import com.fazecast.jSerialComm.SerialPort
import de.sciss.audiofile.{AudioFile, AudioFileSpec, AudioFileType, SampleFormat}
import de.sciss.file.File
import de.sciss.osc
import org.rogach.scallop.ScallopConf

import java.io.{BufferedReader, FileInputStream, InputStream, InputStreamReader}
import java.net.InetSocketAddress
import scala.swing.{Component, Dimension, Graphics2D, Label, MainFrame, Swing}

/*  Corresponds to `swap_swap_reagenz.py`.
 *
 *  Reads ASCII text formatted lines of sensor 16-bit sensor values separated by space characters
 */
object ReceiveSensors:
  val defaultDevice = "/dev/ttyACM1" // "/dev/ttyUSB0"
  val baudRate      = 115200

  case class Config(
                     debug      : Boolean         = false,
                     verbose    : Boolean         = false,
                     gui        : Boolean         = false,
                     device     : String          = defaultDevice,
                     numSensors : Int             = 12,
                     record     : Option[File]    = None,
                     osc        : Boolean         = false,
                     oscIP      : String          = "127.0.0.1",
                     oscPort    : Int             = 57130,
                   )

  def main(args: Array[String]): Unit =
    object p extends ScallopConf(args):
      import org.rogach.scallop.{ScallopOption => Opt, *}

      printedName = "ReceiveLDRText"
      private val default = Config()

      val debug: Opt[Boolean] = toggle(default = Some(default.debug),
        descrYes = "Debug operation.",
      )
      val verbose: Opt[Boolean] = toggle(default = Some(default.verbose),
        descrYes = "Verbose operation.",
      )
      val gui: Opt[Boolean] = toggle(default = Some(default.gui),
        descrYes = "Show sensor data GUI.",
      )
      val device: Opt[String] = opt(default = Some(default.device),
        descr = s"Serial device name (default: ${default.device}).",
      )
      val numSensors: Opt[Int] = opt(name = "num-sensors", default = Some(default.numSensors),
        descr = s"Number of sensor values per frame (default: ${default.numSensors}).",
      )
      val record: Opt[File] = opt(default = default.record,
        descr = s"Record data to IRCAM sound file.",
      )
      val osc: Opt[Boolean] = toggle(default = Some(default.osc),
        descrYes = "Send sensor data to OSC.",
      )
      val oscIP: Opt[String] = opt(name = "osc-ip", default = Some(default.oscIP),
        descr = s"Target OSC IP address (default: ${default.oscIP}).",
      )
      val oscPort: Opt[Int] = opt(name = "osc-port", default = Some(default.oscPort),
        descr = s"Target OSC port (default: ${default.oscPort}).",
      )

      verify()
      val config: Config = Config(
        debug       = debug     (),
        verbose     = verbose   (),
        gui         = gui       (),
        device      = device    (),
        numSensors  = numSensors(),
        record      = record.toOption,
        osc         = osc       (),
        oscIP       = oscIP     (),
        oscPort     = oscPort   (),
      )
    end p

    implicit val c: Config = p.config

    val sensorVals = new Array[Int](c.numSensors)

    val recFileOpt = c.record.map { f =>
      require (!f.exists(), s"Recordings file $f already exists. Not overwriting.")

      val spec  = AudioFileSpec(
        fileType      = AudioFileType.IRCAM,  // doesn't need header update
        sampleFormat  = SampleFormat.Int16,   // that's the resolution of the ADC
        numChannels   = c.numSensors,
        sampleRate    = 20.0,                 // approximately; not used
      )
      val af = AudioFile.openWrite(f, spec)
      af
    }
    val recBufLen = 1024
    val recBuf    = Array.ofDim[Double](c.numSensors, recBufLen)

    val oscT = if !c.osc then None else
      val oscCfg  = osc.UDP.Config()
      oscCfg.localIsLoopback = c.oscIP == "127.0.0.1"
      val t = osc.UDP.Transmitter(oscCfg)
      t.connect()
      Some(t)

    val oscTgt =
      if !c.osc then InetSocketAddress.createUnresolved (c.oscIP, c.oscPort)
      else       new InetSocketAddress                  (c.oscIP, c.oscPort)

    lazy val lb: Component = new Component {
      preferredSize = new Dimension(520, 30 + c.numSensors * 10)

      private val xs = new Array[Double](c.numSensors)
      private val w1 = 0.95
      private val w2 = 1.0 - w1

      override def paintComponent(g: Graphics2D): Unit = {
        super.paintComponent(g)
        var i = 0
        var y = 30
        while (i < c.numSensors) {
          val x = xs(i) * w1 + sensorVals(i) / 64.0 * w2
          xs(i) = x
          g.fillRect(4, y, x.toInt, 8)
          i += 1; y += 10
        }
      }
    }
    if c.gui then Swing.onEDT {
      val f = new MainFrame
      f.contents = lb
      f.pack().centerOnScreen()
      f.open()
    }

    var recBufOff = 0
    run(sensorVals = sensorVals) {
      recFileOpt.foreach { af =>
        var si = 0
        while si < c.numSensors do
          recBuf(si)(recBufOff) = sensorVals(si).toDouble / 0x7FFF
          si += 1

        recBufOff += 1
        if recBufOff == recBufLen then
          // println("WRITE")
          af.write(recBuf)
          recBufOff = 0
      }

      oscT.foreach { t =>
        t.send(osc.Message("/ldr", sensorVals: _*), oscTgt)
      }

      if c.verbose then
        val s = sensorVals.mkString("sensors: ", ", ", "")
        println(s)

      if c.gui then
        lb.repaint()
        lb.toolkit.sync()
    }

  def run(sensorVals: Array[Int])(fun: => Unit)(implicit c: Config): Unit =
    val (port, in) = {
      val ports = SerialPort.getCommPorts()
      val _port = ports.find(_.getSystemPortPath == c.device).getOrElse(sys.error(s"Device ${c.device} not found"))
      val opened = _port.openPort()
      val baudOk = _port.setBaudRate(baudRate)
      //      _port.setComPortTimeouts()
      if (!baudOk) println(s"BAUD RATE $baudRate IS NOT VALID")
      require (opened, s"Could not open ${c.device}")
      (_port, _port.getInputStreamWithSuppressedTimeoutExceptions /*getInputStream*/)
    }
    try
      runWith(in, sensorVals = sensorVals)(fun)
    finally
      port.closePort()

  def runWith(in: InputStream, sensorVals: Array[Int])(fun: => Unit)(implicit c: Config): Unit =
    val numSensors  = sensorVals.length
    val bufSz       = numSensors * 6
    val buf         = new Array[Byte](bufSz)
    while true do
      var lineDone  = false
      var overflow  = false
      var bufOff    = 0

      while !lineDone do
        val c = in.read()
        if c < 0 then {
          // asynchronous; not available
          Thread.sleep(1)
//          Thread.`yield`()
          // println("Stream closed")
          // return
        }
        else if c == 10 then
          lineDone = true
        else if !overflow then
          buf(bufOff) = c.toByte
          bufOff += 1
          if bufOff == bufSz then
            overflow  = true
            lineDone  = true

      val sLine = new String(buf, 0, bufOff, "UTF-8")
      if c.debug then println(s"lineDone at $bufOff. overflow? $overflow. line is '$sLine'")

      if !overflow then
        val sArr = sLine.split(' ')
//        println(s"GOT ${sArr.length} VALUES")
        if sArr.length >= numSensors then
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
                i += 1
              else
                ok = false

            if ok then sensorVals(t) = sensorVal
            t += 1

          fun
        end if
      end if
