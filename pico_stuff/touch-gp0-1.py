# build-in LED blink in Pi Pico using CircuitPython

import time
import board
import digitalio
import touchio

# square "no 1": 720 to 800
touch_threshold_adjust1 = 1880 # 720
touch_threshold_adjust2 = 720

touch_pin1 = board.GP0
touch_pin2 = board.GP1

touchin1 = touchio.TouchIn(touch_pin1)
touchin2 = touchio.TouchIn(touch_pin2)
touchin1.threshold += touch_threshold_adjust1
touchin2.threshold += touch_threshold_adjust2
touch1 = touchin1
touch2 = touchin2

led1 = digitalio.DigitalInOut(board.LED)
led1.direction = digitalio.Direction.OUTPUT # set the direction of the pin

led2 = digitalio.DigitalInOut(board.GP15)
led2.direction = digitalio.Direction.OUTPUT # set the direction of the pin

led1.value = True
time.sleep(1.0)
led1.value = False

# i = 0

while True:
    # i = (i + 1) % 100
    # touch.update()
    touched1 = touch1.value
    touched2 = touch2.value
    if touched1: # or (i == 0):
        led1.value = True
    else: # or (i == 10):
        led1.value = False

    if touched2: # or (i == 0):
        led2.value = True
    else: # or (i == 10):
        led2.value = False
    # time.sleep(0.01)
    