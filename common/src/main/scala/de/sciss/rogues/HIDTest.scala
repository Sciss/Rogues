package de.sciss.rogues

import org.hid4java.{HidManager, HidServices, HidServicesListener, HidServicesSpecification}
import org.hid4java.event.HidServicesEvent
import org.hid4java.jna.HidApi

// more or less verbatim translation from hid4java by Gary Rowe (MIT License)
object HIDTest:
  def main(args: Array[String]): Unit = run()

  private val ANSI_RESET  = "\u001B[0m"
  private val ANSI_BLACK  = "\u001B[30m"
  private val ANSI_RED    = "\u001B[31m"
  private val ANSI_GREEN  = "\u001B[32m"
  private val ANSI_YELLOW = "\u001B[33m"
  private val ANSI_BLUE   = "\u001B[34m"
  private val ANSI_PURPLE = "\u001B[35m"
  private val ANSI_CYAN   = "\u001B[36m"
  private val ANSI_WHITE  = "\u001B[37m"

  def run(): Unit =
    HidApi.useLibUsbVariant = true

    // Configure to use custom specification
    val hidServicesSpecification = new HidServicesSpecification
    // Use the v0.7.0 manual start feature to get immediate attach events
//    hidServicesSpecification.setAutoStart(false)
//    hidServicesSpecification.setAutoDataRead(true)
    hidServicesSpecification.setDataReadInterval(500)

    // Get HID services using custom specification
    val hidServices = HidManager.getHidServices(hidServicesSpecification)
    hidServices.addHidServicesListener(Listener)
    
    // Manually start the services to get attachment event
    println(ANSI_GREEN + "Manually starting HID services." + ANSI_RESET)
    hidServices.start()
    
    println(ANSI_GREEN + "Enumerating attached devices..." + ANSI_RESET)
    
    // Provide a list of attached devices
    
    import scala.jdk.CollectionConverters._
    val hidDevices = hidServices.getAttachedHidDevices.asScala
    for hidDevice <- hidDevices do
      println(hidDevice)

    if hidDevices.nonEmpty then
      val hidDevice = hidDevices.find(_.getProduct.trim == "Generic   USB  Joystick").get
      println(s"Opening for four seconds... isClosed? ${hidDevice.isClosed}")
      hidDevice.open()
      Thread.sleep(4000)
      println(s"Now: isClosed? ${hidDevice.isClosed}")

      Thread.sleep(4000)
      hidDevice.close()

    Thread.sleep(1000)

    println(ANSI_YELLOW + "Triggering shutdown..." + ANSI_RESET)
    hidServices.shutdown()

  object Listener extends HidServicesListener:
    override def hidDeviceAttached(event: HidServicesEvent): Unit =
      println(ANSI_BLUE + "Device attached: " + event + ANSI_RESET)

    override def hidDeviceDetached(event: HidServicesEvent): Unit =
      println(ANSI_YELLOW + "Device detached: " + event + ANSI_RESET)
  
    override def hidFailure(event: HidServicesEvent): Unit =
      println(ANSI_RED + "HID failure: " + event + ANSI_RESET)
  
    override def hidDataReceived(event: HidServicesEvent): Unit =
      printf(ANSI_PURPLE + "Data received:%n")
      val dataReceived: Array[Byte] = event.getDataReceived
      printf("< [%02x]:", dataReceived.length)
      for (b <- dataReceived) {
        printf(" %02x", b)
      }
      println(ANSI_RESET)

  end Listener
  