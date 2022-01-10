import time
import board
import busio
import digitalio
import adafruit_ads1x15.ads1115 as ADS
from adafruit_ads1x15.analog_in import AnalogIn

led = digitalio.DigitalInOut(board.LED)
led.direction = digitalio.Direction.OUTPUT
 
led.value = True
time.sleep(0.5)
led.value = False
time.sleep(0.5)

# Create the I2C bus
pinSCA = board.GP16
pinSCL = board.GP17
i2c = busio.I2C(scl = pinSCL, sda = pinSCA)

# Create the ADC object using the I2C bus
ads = ADS.ADS1115(i2c)

# Create single-ended input on channel 0
chan = AnalogIn(ads, ADS.P0)

# Create differential input between channel 0 and 1
#chan = AnalogIn(ads, ADS.P0, ADS.P1)

print("{:>5}\t{:>5}".format('raw', 'v'))

while True:
    print("{:>5}\t{:>5.3f}".format(chan.value, chan.voltage))
    time.sleep(0.5)
    led.value = True
    time.sleep(0.1)
    led.value = False
    time.sleep(0.4)
