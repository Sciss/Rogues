[![Build Status](https://github.com/Sciss/Rogues/workflows/Scala%20CI/badge.svg?branch=main)](https://github.com/Sciss/Rogues/actions?query=workflow%3A%22Scala+CI%22)

# Rogues

This repository contains code for an ongoing art project. See [Research Catalogue](https://www.researchcatalogue.net/view/1437680/1437681).

(C)opyright 2021–2022 by Hanns Holger Rutz. All rights reserved. This project is released under the
[GNU Affero General Public License](https://github.comt/Sciss/Rogues/blob/main/LICENSE) v3+ and
comes with absolutely no warranties.
To contact the author, send an e-mail to `contact at sciss.de`.

## building

Builds with sbt against Scala 3. See options: `sbt 'common/run --help'`. E.g.

    sbt 'common/runMain de.sciss.rogues.SteinerChain --margin 0 --radius 240 --full-screen --hide-envelope'

    sbt 'common/runMain de.sciss.rogues.Iris --num-blades 7 --full-screen'

    sbt common/assembly
    java -cp common/Rogues-common-assembly-0.1.0-SNAPSHOT.jar de.sciss.rogues.ReceiveLDRText

## cap test

__OBSOLETE__

pigpio must be installed first: http://abyz.me.uk/rpi/pigpio/download.html

    git clone https://github.com/joan2937/pigpio.git
    cd pigpio
    make
    sudo make install

The in `Rogues`:

    sbt common/assembly
    sudo java -cp common/Rogues-common-assembly-0.1.0-SNAPSHOT.jar de.sciss.rogues.CapSense

(yes, I need to get rid of the sudo; thanks pi4j)

## fix wiring-pi

__OBSOLETE__

__Important:__ Wiring-Pi is broken on the Pi 4. The pull up/down resistors cannot be configured.
See https://pi4j.com/1.3/install.html#WiringPi_Native_Library -- one needs to replace the installed versions
with an unofficial one!

    sudo apt remove wiringpi -y
    sudo apt install git-core gcc make
    cd ~/Documents/devel/
    git clone https://github.com/WiringPi/WiringPi --branch master --single-branch wiringpi
    cd wiringpi
    sudo ./build

## installing SuperCollider on the Pi

We build SC 3.10.4:

```
cd ~/Documents/devel
git clone https://github.com/supercollider/supercollider.git

sudo apt install libjack-jackd2-dev libsndfile1-dev libasound2-dev libavahi-client-dev \
libreadline-dev libfftw3-dev libxt-dev libudev-dev libncurses5-dev cmake git qttools5-dev qttools5-dev-tools \
qtdeclarative5-dev libqt5svg5-dev qjackctl

cd supercollider
git checkout -b 3.10.4 Version-3.10.4

git submodule update --init --recursive

mkdir build
cd build

cmake -DCMAKE_BUILD_TYPE=Release -DSUPERNOVA=OFF -DSC_ED=OFF -DSC_EL=OFF -DSC_VIM=ON -DNATIVE=ON -DSC_USE_QTWEBENGINE:BOOL=OFF ..

cmake --build . --config Release --target all -- -j3

sudo cmake --build . --config Release --target install
```

This installs in `/usr/local/bin`. If debian package has been installed, it will override through `/usr/bin`,
to remove use `sudo apt remove supercollider-server` (or `-common` I guess?).

We use `JPverb` thus also need `sc3-plugins`:

```
cd ~/Documents/devel
git clone https://github.com/supercollider/sc3-plugins.git

cd sc3-plugins
git checkout -b 3.10.4 643709850b2f22f68792372aaece5fc6512defc6

git submodule update --init --recursive

mkdir build
cd build

cmake -DSC_PATH=/home/pi/Documents/devel/supercollider/ -SC_PATH=/home/pi/.local/share/SuperCollider ..

cmake --build . --config Release

sudo cmake --build . --config Release --target install

```

## access TTL-to-USB via tinyUSB0 on Linux

To avoid root requirement, add user to dialout group:

    sudo usermod -a -G dialout $USER

Prepare reading, setting speed etc.

    stty -F /dev/ttyUSB0 speed 115200 cs8 -cstopb -parenb -echo

Dump output:

    cat /dev/ttyUSB0

Check if other process accesses the USB:

    lsof /dev/ttyUSB0

Running Python REPL in the terminal:

    screen /dev/ttyACM0 115200

(You may need to `sudo apt install screen`)

## Pn532 tests

https://www.waveshare.com/wiki/PN532_NFC_HAT

Code:

https://www.waveshare.com/w/upload/6/67/Pn532-nfc-hat-code.7z

To unzip: 

    sudo apt install p7zip-full
    mkdir out
    cd out
    7z x ../Pn532-nfc-hat-code.7z

Set both jumpers L0 and L1 to L(ow), to enable UART. Connect the first four pins
to 3v3 / GND / Rx / Tx. Flip Tx/Rx with respect to the TTL-to-USB adapter.

When connected via UART to USB:

```
pn532 = PN532_UART(debug=False, reset=20, dev='/dev/ttyUSB0')
```

## Install HyperPixel

    git clone https://github.com/pimoroni/hyperpixel2r

    cd hyperpixel2r
    sudo ./install.sh

Needs Raspbian Buster not Bullseye!

## Pis

- 192.168.77.35
- 192.168.77.40
- 192.168.77.45 "eve"
