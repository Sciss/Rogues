package de.sciss.rogues

import com.fazecast.jSerialComm.SerialPort

object SerialTest:
  def main(args: Array[String]): Unit =
    val ports = SerialPort.getCommPorts()
    ports.foreach { p =>
      println(s"$p: baud rate ${p.getBaudRate}, ${p.getNumDataBits}/${p.getParity}/${p.getNumStopBits}, path ${p.getSystemPortPath}, descr1 ${p.getDescriptivePortName}, descr2 ${p.getPortDescription}, system ${p.getSystemPortName}")
    }
