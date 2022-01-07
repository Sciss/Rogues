package de.sciss.rogues

import purejavahidapi.{HidDevice, HidDeviceInfo, InputReportListener, PureJavaHidApi}

import java.util

// attempt with 'purejavahidapi'
object HIDTest2:
  def main(args: Array[String]): Unit =
    import scala.jdk.CollectionConverters._
    val hidDevices = PureJavaHidApi.enumerateDevices.asScala
    for info <- hidDevices do
      printf("VID = 0x%04X PID = 0x%04X Manufacturer = %s Product = %s Path = %s\n",
        info.getVendorId, info.getProductId, info.getManufacturerString, info.getProductString, info.getPath
      )

    // N.B.: bug, getManufacturerString returns null, getProductString begins with manufacturer
    val devInfo = hidDevices.find(_.getProductString.contains("Generic   USB  Joystick")).get
    val dev = PureJavaHidApi.openDevice(devInfo)
    dev.setInputReportListener(new InputReportListener():
      override def onInputReport(hidDevice: HidDevice, id: Byte, data: Array[Byte], len: Int): Unit =
        printf("onInputReport: id %d len %d data ", id, len)
        for i <- 0 until len do
          printf("%02X ", data(i))

        println()
    )

    Thread.sleep(8000)
    dev.close()