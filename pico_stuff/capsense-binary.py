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
uart = busio.UART(tx=board.GP16, rx=board.GP17, baudrate=baudRate, bits=8, parity=None, stop=1) #  imeout: float = 1, receiver_buffer_size: int = 64

leds[0].value = True
time.sleep(1.0)
leds[0].value = False

while True:
    for idx in range(numTouch):
        touch    = touchIns[idx]
        touchVal = touch.raw_value    # touchio library clips at 10000, so fits into 16 bits
        touched  = touchVal > touch.threshold
        led      = leds[idx]
        led.value = touched
        touchVals[idx] = touchVal
    
    text = "A: {}, B: {}\n".format(touchVals[0], touchVals[0])
    buf = bytes(text, "ascii")
    uart.write(buf)
    time.sleep(0.01)

deinit(uart)
