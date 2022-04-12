import time
import board
import busio
import digitalio
import adafruit_ads1x15.ads1115 as ADS
import touchio
import neopixel
# import usb_cdc
from adafruit_ads1x15.analog_in import AnalogIn

# this file has pin layout like the PCB v1

ledInt = digitalio.DigitalInOut(board.LED)
ledInt.direction = digitalio.Direction.OUTPUT

butWait = digitalio.DigitalInOut(board.GP15)
butWait.switch_to_input(pull=digitalio.Pull.UP) # they close to GND

# press button to avoid program
if (not butWait.value):
    while True:
        ledInt.value = True
        time.sleep(0.1)
        ledInt.value = False
        time.sleep(0.4)

# Create the I2C bus
pinSCA = board.GP26 # GP20 # GP16
pinSCL = board.GP27 # GP21 # GP17
i2c = busio.I2C(scl = pinSCL, sda = pinSCA)

while not i2c.try_lock():
    pass

try:
    while True:
        print(
            "I2C addresses found:",
            [hex(device_address) for device_address in i2c.scan()],
        )
        time.sleep(2)

finally:  # unlock the i2c bus when ctrl-c'ing out of the loop
    i2c.unlock()