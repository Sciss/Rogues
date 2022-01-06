import time
import board
import digitalio
import touchio
import busio

touch_threshold_adjust1 = 1880
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
led1.direction = digitalio.Direction.OUTPUT

led2 = digitalio.DigitalInOut(board.GP15)
led2.direction = digitalio.Direction.OUTPUT

baudRate = 115200 # 9600
uart = busio.UART(tx=board.GP16, rx=board.GP17, baudrate=baudRate, bits=8, parity=None, stop=1) #  imeout: float = 1, receiver_buffer_size: int = 64

led1.value = True
time.sleep(1.0)
led1.value = False

while True:
    touchVal1 = touch1.raw_value
    touchVal2 = touch2.raw_value
#    touched1 = touch1.value
#    touched2 = touch2.value
    touched1 = touchVal1 > touch1.threshold
    touched2 = touchVal2 > touch2.threshold

    if touched1:
        led1.value = True
    else:
        led1.value = False
    
    if touched2:
        led2.value = True
    else:
        led2.value = False
    # time.sleep(0.01)
    
    text = "A: {}, B: {}\n".format(touchVal1, touchVal2)
    buf = bytes(text, "ascii")
    uart.write(buf)
    time.sleep(0.01)

deinit(uart)
