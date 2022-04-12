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
ads1 = ADS.ADS1115(i2c, address=0x48)
ads2 = ADS.ADS1115(i2c, address=0x49)

# Create single-ended input on channel 0
ldr_ch1 = AnalogIn(ads1, ADS.P0)
ldr_ch2 = AnalogIn(ads1, ADS.P1)
ldr_ch3 = AnalogIn(ads1, ADS.P2)

ldr_ch4 = AnalogIn(ads2, ADS.P0)
ldr_ch5 = AnalogIn(ads2, ADS.P1)
ldr_ch6 = AnalogIn(ads2, ADS.P2)

numSensors = 6
sensors = [ldr_ch1, ldr_ch2, ldr_ch3, ldr_ch4, ldr_ch5, ldr_ch6]

# Create differential input between channel 0 and 1
#chan = AnalogIn(ads, ADS.P0, ADS.P1)

ledInt.value = True
time.sleep(0.5)
ledInt.value = False
time.sleep(0.5)

# cap_ch1 = touchio.TouchIn(board.GP2)
# cap_ch2 = touchio.TouchIn(board.GP3)
# cap_ch3 = touchio.TouchIn(board.GP4)

# cap_ch4 = touchio.TouchIn(board.GP6)
# cap_ch5 = touchio.TouchIn(board.GP7)
# cap_ch6 = touchio.TouchIn(board.GP8)

ledInt.value = True
time.sleep(0.5)
ledInt.value = False
time.sleep(0.5)

print("Hello from swap space, v3.")

# # On CircuitPlayground Express, and boards with built in status NeoPixel -> board.NEOPIXEL
# # Otherwise choose an open pin connected to the Data In of the NeoPixel strip, i.e. board.D1
# pixel_pin = board.GP16 # GP18

# # The number of NeoPixels
# num_pixels = 1

# # The order of the pixel colors - RGB or GRB. Some NeoPixels have red and green reversed!
# # For RGBW NeoPixels, simply change the ORDER to RGBW or GRBW.
# ORDER = neopixel.GRB

# pixels = neopixel.NeoPixel(
#     pixel_pin, num_pixels, brightness=0.2, auto_write=False, pixel_order=ORDER
# )

ledInt.value = True
time.sleep(0.5)
ledInt.value = False
time.sleep(0.5)

# numColors  = 4
# colorRed   = [255, 0, 0, 128]
# colorGreen = [0, 255, 0, 128]
# colorBlue  = [0, 0, 255, 128]
# colorIdx   = 0

# pixels.fill((0, 0, 0))
# pixels.show()

ledCnt = 0

writer = usb_cdc.data
writer.write_timeout = None

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
    for si in range(numSensors):
        value   = sensors[si].value
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

    writer.write(buf)

    time.sleep(samplePeriod)
    ledCnt = ledCnt + 1
    if (ledCnt == 20):
        ledExt1.value = True
    elif (ledCnt == 21):
        ledExt1.value = False
        ledCnt = 0


    # time.sleep(0.2)
    # ledInt.value = True
    # time.sleep(0.1)
    # ledInt.value = False
    # time.sleep(0.2)

    # print("{:>5}\t{:>5}\t{:>5}\t{:>5}\t{:>5}\t{:>5}".format(
    #     cap_ch1.raw_value, cap_ch2.raw_value, cap_ch3.raw_value, cap_ch4.raw_value, cap_ch5.raw_value, cap_ch6.raw_value
    # ))

    # print("{:>5}".format(
    #     cap_ch1.raw_value #, cap_ch2.raw_value, cap_ch3.raw_value, cap_ch4.raw_value, cap_ch5.raw_value, cap_ch6.raw_value
    # ))

    # time.sleep(0.225)
    # ledInt.value = True
    # time.sleep(0.05)
    # ledInt.value = False
    # time.sleep(0.225)

    # print("Ping")
    # time.sleep(0.2)

    # if (not butWait.value):
    #     ledExt1.value = True
    #     time.sleep(0.5)
    #     ledExt1.value = False

    # if (not butAux.value):
    #     # ledExt2.value = True
    #     # time.sleep(0.5)
    #     # ledExt2.value = False
    #     pixels.fill((colorRed[colorIdx], colorGreen[colorIdx], colorBlue[colorIdx]))
    #     colorIdx = (colorIdx + 1) % numColors
    #     # Uncomment this line if you have RGBW/GRBW NeoPixels
    #     # pixels.fill((255, 0, 0, 0))
    #     pixels.show()

    # if (not butOff.value):
    #     ledInt.value = True
    #     time.sleep(0.5)
    #     ledInt.value = False
        