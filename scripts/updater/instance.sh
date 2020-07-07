#!/bin/bash -e

distribDirectoryUrl=$3
serviceToSetup=updater
. update.sh

echo "Execute setup"
./updater_setup.sh Azure $@