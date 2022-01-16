import time
import board
import digitalio
import touchio
import busio
import usb_cdc

ledInt = digitalio.DigitalInOut(board.LED)
ledInt.direction = digitalio.Direction.OUTPUT

BLA = 0
while BLA < 2:
    ledInt.value = True
    time.sleep(0.25)
    ledInt.value = False
    time.sleep(0.25)
    BLA += 1

# touchThresholdsAdd  = [ 1880        , 720       , 720       , 720       , 720       , 720        ]
touchThresholds     = [ 1800        , 1700       , 1700 , 1700       , 1700       , 1700        ]
# touchPins           = [ board.GP0   , board.GP2 , board.GP4 , board.GP6 , board.GP8 , board.GP10 ]
touchPins           = [ board.GP1   , board.GP2 , board.GP4 , board.GP9 , board.GP10 , board.GP12 ]
# ledPins             = [ board.LED   , board.GP15, None      , None      , None      , None       ]
ledPins             = [ board.LED ,  board.GP5, board.GP13, None      , None      , None       ]
numTouch = 6 # 6 # touchPins.size

but1 = digitalio.DigitalInOut(board.GP7)
but1.switch_to_input(pull=digitalio.Pull.UP) # they close to GND
but2 = digitalio.DigitalInOut(board.GP15)
but2.switch_to_input(pull=digitalio.Pull.UP) # they close to GND

touchIns = []
leds = []
touchVals = []
for idx in range(numTouch):
    pin = touchPins[idx]
    touchIn = touchio.TouchIn(pin)
    touchIn.threshold = touchThresholds[idx]
    touchIns.append(touchIn)
    ledPin = ledPins[idx]
    led = None
    if not (ledPin is None):
        if ledPin == board.LED:
            led = ledInt
        else:
            led = digitalio.DigitalInOut(ledPin)
            led.direction = digitalio.Direction.OUTPUT
            # led.value = True
            # time.sleep(0.2)
            # led.value = False

    leds.append(led)
    touchVals.append(0)

ledInt.value = True
time.sleep(0.5)
ledInt.value = False
time.sleep(0.5)

# baudRate = 115200 # 9600
# writer = busio.UART(tx=board.GP16, rx=board.GP17, baudrate=baudRate, bits=8, parity=None, stop=1) #  timeout: float = 1, receiver_buffer_size: int = 64
writer = usb_cdc.data
writer.write_timeout = None

ledInt.value = True
time.sleep(0.5)
ledInt.value = False
time.sleep(0.5)

# for some reason binary reception is not good unless we ensure
# newline characters are sent. thus, use 0xA as terminator, and
# then we have to special case sensor value bytes that are 0xA
bufSz = numTouch * 2 + 2
buf = bytearray(bufSz) # 2 bytes per sensor value, one start word byte, one stop word byte
buf[0]         = 0b10000000
buf[bufSz - 1] = 0b10000001 # 10 # test newline 0b10000001

# bufStall = bytearray(bufSz)

# touchio library raw values clip at 10000 (14 bits)
while True:
    bi = 1
    for idx in range(numTouch):
        touch    = touchIns[idx]
        touchVal = touch.raw_value
        thresh   = touchThresholds[idx]
        touched  = touchVal > thresh # touch.threshold
        led      = leds[idx]
        if not (led is None):
            # if idx == 0:
            #     led.value = not but1.value
            # elif idx == 1:
            #     led.value = not but2.value
            # else:
            led.value = touched
            
        senseHi  = (touchVal >> 7) & 0b1111111
        # if (senseHi >= 10):
        #     senseHi += 1 #test
        senseLo  =  touchVal       & 0b1111111
        # if (senseLo >= 10):
        #     senseLo += 1 #test
        buf[bi]  = senseHi
        bi += 1
        buf[bi]  = senseLo
        bi += 1
        touchVals[idx] = touchVal
    
    # text = "A: {}, B: {}\n".format(touchVals[0], touchVals[0])
    # buf1 = bytes(text, "ascii")
    # uart.write(buf1)
    # written = 
    writer.write(buf)
    # writer.flush()
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
