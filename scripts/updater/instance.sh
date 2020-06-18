#!/bin/bash -e

function exitUsage() {
  >&2 echo "Use: $0 name services distribDirectoryUrl"
  exit 1
}

if [ -z "$3" ]; then
  exitUsage
fi

name=$1
services=$2
distribDirectoryUrl=$3

serviceToSetup=updater
. update.sh

>&2 echo "Execute setup"
./updater_setup.sh Azure ${name} ${services} ${distribDirectoryUrl}