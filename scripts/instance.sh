#!/bin/bash

function exitUsage() {
  echo "Use: $0 name services distribDirectoryUrl"
  exit 1
}

if [ -z "$3" ]
then
  exitUsage
fi

name=$1
services=$2
distribDirectoryUrl=$3

echo "Download desired scripts"
curl ${distribDirectoryUrl}/download-desired-version/scripts --output scripts.zip --retry 1000 --retry-delay 2 --connect-timeout 5
unzip -o scripts.zip updater_setup.sh && chmod +x updater_setup.sh
echo "Execute setup"
./updater_setup.sh Azure ${name} ${services} ${distribDirectoryUrl}