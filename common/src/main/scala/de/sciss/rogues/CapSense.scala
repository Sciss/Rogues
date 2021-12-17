package de.sciss.rogues

import com.pi4j.Pi4J
import com.pi4j.io.gpio.GpioProvider
import com.pi4j.io.gpio.digital.{DigitalInput, DigitalOutput, PullResistance}

import java.util.concurrent.TimeUnit

object CapSense:
  def main(args: Array[String]): Unit = run()

  val PIN_OUT = 23
  val PIN_IN  = 24

  def run(): Unit =
    val instance  = Pi4J.newAutoContext()
    val cOut      = DigitalOutput.newConfigBuilder(instance)
      .id("out")
      .address(PIN_OUT)
      .provider("pigpio-digital-output")
    val cIn       = DigitalInput.newConfigBuilder(instance)
      .id("out")
      .address(PIN_IN)
      .pull(PullResistance.OFF)
      .provider("pigpio-digital-input")

    import com.pi4j.io.gpio.digital.DigitalState

    val pinOut  = instance.create(cOut)
    val pinIn   = instance.create(cIn )

    var T_REF   = 0L

    pinIn.addListener { e =>
      val now = System.currentTimeMillis()
      println(s"${now - T_REF}: ${e.state()}")
    }

    println("Started. Waiting for 2 sec")
    Thread.sleep(2000)
    println("OUT pulse")
    T_REF = System.currentTimeMillis()
    pinOut.pulse(100, TimeUnit.MILLISECONDS)
    Thread.sleep(2000)

    println("Shutting down")
    instance.shutdown()
    sys.exit()
    