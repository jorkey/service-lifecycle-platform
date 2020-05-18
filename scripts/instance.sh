#!/bin/bash

function exitUsage() {
  echo "Use: $0 name services distribDirectoryUrl"
  exit 1
}

if [ -z "$3" ]; then
  exitUsage
fi

name=$1
services=$2
distribDirectoryUrl=$3

. ./update_scripts.sh

distribDirectoryUrl=`jq -r .developerDistributionUrl builder.json`
update_scripts ${distribDirectoryUrl} instance.sh updater_setup.sh

echo "Execute setup"
./updater_setup.sh Azure ${name} ${services} ${distribDirectoryUrl}