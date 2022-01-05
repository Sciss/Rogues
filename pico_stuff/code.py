# build-in LED blink in Pi Pico using CircuitPython

import time
import board
import digitalio
import touchio

touch_threshold_adjust = 720

touch_pin = board.GP0

touchin = touchio.TouchIn(touch_pin)
touchin.threshold += touch_threshold_adjust
touch = touchin
#    touchs.append( touchin ) # Debouncer(touchin)

led = digitalio.DigitalInOut(board.LED)
led.direction = digitalio.Direction.OUTPUT # set the direction of the pin

led.value = True
time.sleep(1.0)
led.value = False

i = 0

while True:
    # touch.update()
    touched = touch.value
    i = (i + 1) % 100
    if touched: # or (i == 0):
        led.value = True
    elif (not touched): # or (i == 10):
        # if touch.fell:
        led.value = False

    # time.sleep(0.01)
    