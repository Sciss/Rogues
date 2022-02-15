import time
import board
import busio
import digitalio
import adafruit_ads1x15.ads1115 as ADS
import touchio
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
ldr_ch1 = AnalogIn(ads1, ADS.P0)
ldr_ch2 = AnalogIn(ads1, ADS.P1)
ldr_ch3 = AnalogIn(ads1, ADS.P2)

ldr_ch4 = AnalogIn(ads2, ADS.P0)
ldr_ch5 = AnalogIn(ads2, ADS.P1)
ldr_ch6 = AnalogIn(ads2, ADS.P2)

# Create differential input between channel 0 and 1
#chan = AnalogIn(ads, ADS.P0, ADS.P1)

ledInt.value = True
time.sleep(0.5)
ledInt.value = False
time.sleep(0.5)

cap_ch1 = touchio.TouchIn(board.GP2)
# cap_ch2 = touchio.TouchIn(board.GP3)
# cap_ch3 = touchio.TouchIn(board.GP4)

# cap_ch4 = touchio.TouchIn(board.GP6)
# cap_ch5 = touchio.TouchIn(board.GP7)
# cap_ch6 = touchio.TouchIn(board.GP8)

ledInt.value = True
time.sleep(0.5)
ledInt.value = False
time.sleep(0.5)

print("{:>5}\t{:>5}".format('raw', 'v'))

while True:
    print("{:>5}\t{:>5.3f}   {:>5}\t{:>5.3f}   {:>5}\t{:>5.3f} | {:>5}\t{:>5.3f}   {:>5}\t{:>5.3f}   {:>5}\t{:>5.3f}".format(
        ldr_ch1.value, ldr_ch1.voltage, ldr_ch2.value, ldr_ch2.voltage, ldr_ch3.value, ldr_ch3.voltage,
        ldr_ch4.value, ldr_ch4.voltage, ldr_ch5.value, ldr_ch5.voltage, ldr_ch6.value, ldr_ch6.voltage
    ))

    time.sleep(0.2)
    ledExt1.value = True
    time.sleep(0.1)
    ledExt1.value = False
    time.sleep(0.2)

#    print("{:>5}\t{:>5}\t{:>5}\t{:>5}\t{:>5}\t{:>5}".format(
#        cap_ch1.raw_value, cap_ch2.raw_value, cap_ch3.raw_value, cap_ch4.raw_value, cap_ch5.raw_value, cap_ch6.raw_value
#    ))

    print("{:>5}".format(
        cap_ch1.raw_value #, cap_ch2.raw_value, cap_ch3.raw_value, cap_ch4.raw_value, cap_ch5.raw_value, cap_ch6.raw_value
    ))

    time.sleep(0.2)
    ledExt2.value = True
    time.sleep(0.1)
    ledExt2.value = False
    time.sleep(0.2)
