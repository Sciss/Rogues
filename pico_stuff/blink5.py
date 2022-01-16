import time
import board
import digitalio
 
ledInt = digitalio.DigitalInOut(board.LED)
ledInt.direction = digitalio.Direction.OUTPUT
ledExt = digitalio.DigitalInOut(board.GP5)
ledExt.direction = digitalio.Direction.OUTPUT

i = 0

while True:
    i = (i + 1) % 10
    if i == 0:
        ledInt.value = True
        time.sleep(0.2)
        ledInt.value = False
    else:
        ledExt.value = True
        time.sleep(0.2)
        ledExt.value = False

    time.sleep(0.5)
