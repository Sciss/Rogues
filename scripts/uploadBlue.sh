#!/bin/bash
cd "$(dirname "$0")"
cd ..
scp common/Rogues-common.jar pi@192.168.77.45:Documents/devel/Rogues/common/
