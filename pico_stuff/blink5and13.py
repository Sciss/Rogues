import time
import board
import digitalio
 
ledInt = digitalio.DigitalInOut(board.LED)
ledInt.direction = digitalio.Direction.OUTPUT
ledExt1 = digitalio.DigitalInOut(board.GP5)
ledExt1.direction = digitalio.Direction.OUTPUT
ledExt2 = digitalio.DigitalInOut(board.GP13)
ledExt2.direction = digitalio.Direction.OUTPUT

i = 0
j = True

while True:
    i = (i + 1) % 10
    if i == 0:
        ledInt.value = True
        time.sleep(0.2)
        ledInt.value = False
    else:
        if j:
            ledExt1.value = True
            time.sleep(0.2)
            ledExt1.value = False
        else:
            ledExt2.value = True
            time.sleep(0.2)
            ledExt2.value = False
        j = not j

    time.sleep(0.5)
