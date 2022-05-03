import time
import board
import busio
import digitalio
import adafruit_ads1x15.ads1115 as ADS
import touchio
import neopixel
# import usb_cdc
from adafruit_ads1x15.analog_in import AnalogIn
import usb_cdc

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

samplePeriod = 0.05 # 20 Hz

butAux = digitalio.DigitalInOut(board.GP14)
butAux.switch_to_input(pull=digitalio.Pull.UP) # they close to GND

butOff = digitalio.DigitalInOut(board.GP13)
butOff.switch_to_input(pull=digitalio.Pull.UP) # they close to GND

ledExt1 = digitalio.DigitalInOut(board.GP0)
ledExt1.direction = digitalio.Direction.OUTPUT
ledExt2 = digitalio.DigitalInOut(board.GP1)
ledExt2.direction = digitalio.Direction.OUTPUT

# Create the I2C bus
pinSCA = board.GP26 # GP20 # GP16
pinSCL = board.GP27 # GP21 # GP17
i2c = busio.I2C(scl = pinSCL, sda = pinSCA)

# Create the ADC object using the I2C bus
# ads1 = ADS.ADS1115(i2c, address=0x48)
# ads2 = ADS.ADS1115(i2c, address=0x49)
ads1 = ADS.ADS1115(i2c, address=0x49)

# Create single-ended input on channel 0
ldr_ch1 = AnalogIn(ads1, ADS.P0)
ldr_ch2 = AnalogIn(ads1, ADS.P1)
ldr_ch3 = AnalogIn(ads1, ADS.P2)
ldr_ch4 = AnalogIn(ads1, ADS.P3)

numSensorsLDR = 4
sensorsLDR = [ldr_ch1, ldr_ch2, ldr_ch3, ldr_ch4]

ledInt.value = True
time.sleep(0.5)
ledInt.value = False
time.sleep(0.5)

numSensorsCap = 0
sensorsCap = []

ledInt.value = True
time.sleep(0.5)
ledInt.value = False
time.sleep(0.5)

print("Hello from swap space, ph, v1.")

ledInt.value = True
time.sleep(0.5)
ledInt.value = False
time.sleep(0.5)

ledCnt = 0

writer = usb_cdc.data
writer.write_timeout = None

numSensors = numSensorsLDR + numSensorsCap

bufSz = numSensors * 6
buf = bytearray(bufSz)
for si in range(numSensors):
    buf[si * 6 + 5] = 32 # space

buf[bufSz - 1] = 10 # newline

while True:
    # print("{} {} {} {} {} {}".format(
    #     ldr_ch1.value, ldr_ch2.value, ldr_ch3.value,
    #     ldr_ch4.value, ldr_ch5.value, ldr_ch6.value,
    # ))
    for si in range(numSensorsLDR):
        value   = sensorsLDR[si].value
        b5      = value // 10000
        value   = value % 10000
        bi      = si * 6
        buf[bi + 0] = b5 + 48
        b4      = value // 1000
        value   = value % 1000
        buf[bi + 1] = b4 + 48
        b3      = value // 100
        value   = value % 100
        buf[bi + 2] = b3 + 48
        b2      = value // 10
        value   = value % 10
        buf[bi + 3] = b2 + 48
        b1      = value
        buf[bi + 4] = b1 + 48

    # for si in range(numSensorsCap):
    #     value   = sensorsCap[si].raw_value
    #     b5      = value // 10000
    #     value   = value % 10000
    #     bi      = (si + numSensorsLDR) * 6
    #     buf[bi + 0] = b5 + 48
    #     b4      = value // 1000
    #     value   = value % 1000
    #     buf[bi + 1] = b4 + 48
    #     b3      = value // 100
    #     value   = value % 100
    #     buf[bi + 2] = b3 + 48
    #     b2      = value // 10
    #     value   = value % 10
    #     buf[bi + 3] = b2 + 48
    #     b1      = value
    #     buf[bi + 4] = b1 + 48

    writer.write(buf)

    time.sleep(samplePeriod)
    ledCnt = ledCnt + 1
    if (ledCnt == 20):
        ledExt1.value = True
    elif (ledCnt == 21):
        ledExt1.value = False
        ledCnt = 0

    # if (not butOff.value):
    #     ledInt.value = True
    #     time.sleep(0.5)
    #     ledInt.value = False
        