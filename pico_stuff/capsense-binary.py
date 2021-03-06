import time
import board
import digitalio
import touchio
import busio

numTouch = 2
touchThresholdsAdd = [ 1880, 720 ]
touchPins = [ board.GP0, board.GP1 ]
ledPins = [ board.LED, board.GP15 ]

touchIns = []
leds = []
touchVals = []
for idx in range(numTouch):
    pin = touchPins[idx]
    touchIn = touchio.TouchIn(pin)
    touchIn.threshold += touchThresholdsAdd[idx]
    touchIns.append(touchIn)
    ledPin = ledPins[idx]
    led = digitalio.DigitalInOut(ledPin)
    led.direction = digitalio.Direction.OUTPUT
    leds.append(led)
    touchVals.append(0)

baudRate = 115200 # 9600
uart = busio.UART(tx=board.GP16, rx=board.GP17, baudrate=baudRate, bits=8, parity=None, stop=1) #  timeout: float = 1, receiver_buffer_size: int = 64

leds[0].value = True
time.sleep(1.0)
leds[0].value = False

# for some reason binary reception is not good unless we ensure
# newline characters are sent. thus, use 0xA as terminator, and
# then we have to special case sensor value bytes that are 0xA
bufSz = numTouch * 2 + 2
buf = bytearray(bufSz) # 2 bytes per sensor value, one start word byte, one stop word byte
buf[0]         = 0b10000000
buf[bufSz - 1] = 10 # test newline 0b10000001

bufStall = bytearray(bufSz)

# touchio library raw values clip at 10000 (14 bits)
while True:
    bi = 1
    for idx in range(numTouch):
        touch    = touchIns[idx]
        touchVal = touch.raw_value
        touched  = touchVal > touch.threshold
        led      = leds[idx]
        led.value = touched
        senseHi  = (touchVal >> 7) & 0b1111111
        if (senseHi >= 10):
            senseHi += 1 #test
        senseLo  =  touchVal       & 0b1111111
        if (senseLo >= 10):
            senseLo += 1 #test
        buf[bi]  = senseHi
        bi += 1
        buf[bi]  = senseLo
        bi += 1
        touchVals[idx] = touchVal
    
    # text = "A: {}, B: {}\n".format(touchVals[0], touchVals[0])
    # buf1 = bytes(text, "ascii")
    # uart.write(buf1)
    written = uart.write(buf)
    # if written is None:
    #     written = 0
    # 
    # if (written < bufSz):
    #     leds[0].value = True
    #     time.sleep(0.5)
    #     leds[0].value = False
    #     time.sleep(0.5)

    time.sleep(0.01)

deinit(uart)
