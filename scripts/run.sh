#!/bin/bash
cd "$(dirname "$0")"
cd ..
java -Dsun.java2d.opengl=true -cp common/Rogues-common.jar de.sciss.rogues.SwapRogue --margin 0 --full-screen "$@"
