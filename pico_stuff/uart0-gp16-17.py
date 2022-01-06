import board
import busio
import time
import digitalio
 
led = digitalio.DigitalInOut(board.LED)
led.direction = digitalio.Direction.OUTPUT
 
uart = busio.UART(tx=board.GP16, rx=board.GP17, baudrate=9600, bits=8, parity=None, stop=1) #  imeout: float = 1, receiver_buffer_size: int = 64

led.value = True
time.sleep(0.5)
led.value = False
time.sleep(0.5)

while True:
    text = "Hello from Pico!\n"
    buf = bytes(text, "ascii")
    uart.write(buf)
    led.value = True
    time.sleep(0.1)
    led.value = False
    time.sleep(0.9)

deinit(uart)
