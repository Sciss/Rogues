import usb_cdc
import time
import board
import digitalio
 
led = digitalio.DigitalInOut(board.LED)
led.direction = digitalio.Direction.OUTPUT
 
d = usb_cdc.data
count = 0
dt = 0.5
if d is None:
    dt = 0.1

while True:
    led.value = True
    time.sleep(0.5)
    led.value = False
    time.sleep(dt)
    count += 1
    text = "count: {}\n".format(count)
    buf = bytes(text, "ascii")
    if not (d is None):
        d.write(buf)
        d.flush()
