#!/bin/bash
cd "$(dirname "$0")"
cd ..
sbt common/assembly
