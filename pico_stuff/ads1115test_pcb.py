import time
import board
import busio
import digitalio
import adafruit_ads1x15.ads1115 as ADS
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

ledExt1 = digitalio.DigitalInOut(board.GP0)
ledExt1.direction = digitalio.Direction.OUTPUT
ledExt2 = digitalio.DigitalInOut(board.GP1)
ledExt2.direction = digitalio.Direction.OUTPUT

# Create the I2C bus
pinSCA = board.GP26 # GP20 # GP16
pinSCL = board.GP27 # GP21 # GP17
i2c = busio.I2C(scl = pinSCL, sda = pinSCA)

# Create the ADC object using the I2C bus
ads1 = ADS.ADS1115(i2c, address=0x48)
ads2 = ADS.ADS1115(i2c, address=0x49)

# Create single-ended input on channel 0
chan1 = AnalogIn(ads1, ADS.P0)
chan2 = AnalogIn(ads1, ADS.P1)
chan3 = AnalogIn(ads1, ADS.P2)

chan4 = AnalogIn(ads2, ADS.P0)
chan5 = AnalogIn(ads2, ADS.P1)
chan6 = AnalogIn(ads2, ADS.P2)

# Create differential input between channel 0 and 1
#chan = AnalogIn(ads, ADS.P0, ADS.P1)

ledInt.value = True
time.sleep(0.5)
ledInt.value = False
time.sleep(0.5)

print("{:>5}\t{:>5}".format('raw', 'v'))

while True:
    print("{:>5}\t{:>5.3f}   {:>5}\t{:>5.3f}   {:>5}\t{:>5.3f} | {:>5}\t{:>5.3f}   {:>5}\t{:>5.3f}   {:>5}\t{:>5.3f}".format(
        chan1.value, chan1.voltage, chan2.value, chan2.voltage, chan3.value, chan3.voltage,
        chan4.value, chan4.voltage, chan5.value, chan5.voltage, chan6.value, chan6.voltage
    ))
    time.sleep(0.5)
    ledExt1.value = True
    time.sleep(0.1)
    ledExt1.value = False
    time.sleep(0.4)
