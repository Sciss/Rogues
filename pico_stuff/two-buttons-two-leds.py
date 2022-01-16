# these are the auxiliary LEDs and buttons on the prototype

import time
import board
import digitalio
 
ledInt = digitalio.DigitalInOut(board.LED)
ledInt.direction = digitalio.Direction.OUTPUT
ledExt1 = digitalio.DigitalInOut(board.GP5)
ledExt1.direction = digitalio.Direction.OUTPUT
ledExt2 = digitalio.DigitalInOut(board.GP13)
ledExt2.direction = digitalio.Direction.OUTPUT

but1 = digitalio.DigitalInOut(board.GP7)
but1.switch_to_input(pull=digitalio.Pull.UP) # they close to GND
but2 = digitalio.DigitalInOut(board.GP15)
but2.switch_to_input(pull=digitalio.Pull.UP) # they close to GND

ledInt.value = True
time.sleep(0.2)
ledInt.value = False

while True:
    ledExt1.value = not but1.value
    ledExt2.value = not but2.value
    time.sleep(0.01)
