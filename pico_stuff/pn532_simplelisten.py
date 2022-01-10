# SPDX-FileCopyrightText: 2021 ladyada for Adafruit Industries
# SPDX-License-Identifier: MIT

"""
This example shows connecting to the PN532 with I2C (requires clock
stretching support), SPI, or UART. SPI is best, it uses the most pins but
is the most reliable and universally supported. In this example, we also connect
IRQ and poll that pin for a card. We don't try to read the card until we know
there is one present. After initialization, try waving various 13.56MHz RFID
cards over it!
"""

import time
import board
import busio
import digitalio
from digitalio import DigitalInOut

led = digitalio.DigitalInOut(board.LED)
led.direction = digitalio.Direction.OUTPUT
 
led.value = True
time.sleep(0.5)
led.value = False
time.sleep(0.5)
time.sleep(1.0)

#
# NOTE: pick the import that matches the interface being used
#
#from adafruit_pn532.i2c import PN532_I2C
#
from adafruit_pn532.spi import PN532_SPI
# from adafruit_pn532.uart import PN532_UART

# I2C connection:
#i2c = busio.I2C(board.SCL, board.SDA)

# Non-hardware
# pn532 = PN532_I2C(i2c, debug=False)

# With I2C, we recommend connecting RSTPD_N (reset) to a digital pin for manual
# harware reset
#reset_pin = DigitalInOut(board.D6)
# On Raspberry Pi, you must also connect a pin to P32 "H_Request" for hardware
# wakeup! this means we don't need to do the I2C clock-stretch thing
#req_pin = DigitalInOut(board.D12)
# Using the IRQ pin allows us to determine when a card is present by checking
# to see if the pin is pulled low.
#irq_pin = DigitalInOut(board.D10)
#pn532 = PN532_I2C(i2c, debug=False, reset=reset_pin, req=req_pin, irq=irq_pin)

# SPI connection:
spiCK = board.GP18
spiRX = board.GP16 # aka MISO
spiTX = board.GP19 # aka MOSI
spiCS = board.GP17

spi     = busio.SPI(clock = spiCK, MOSI = spiTX, MISO = spiRX)
cs_pin  = DigitalInOut(spiCS)
pn532   = PN532_SPI(spi, cs_pin, debug=False)

firm = pn532.firmware_version
ic, ver, rev, support = firm

# Configure PN532 to communicate with MiFare cards
pn532.SAM_configuration()

print("Start listening")
# Start listening for a card
pn532.listen_for_passive_target()

while True:
    # time.sleep(1.0)
    print("Waiting for RFID/NFC card...")
    uid = None
    while uid is None:
        led.value = True
        time.sleep(0.1)
        led.value = False
        # time.sleep(0.4)
        # Check if a card is available to read
        uid = pn532.read_passive_target(timeout=0.5)
        print(".", end="")

    print("")
    print("Found card with UID:", [hex(i) for i in uid])
