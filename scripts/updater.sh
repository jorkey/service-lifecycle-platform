#!/bin/bash

if [ -z "$3" ]
then
  echo "Use: $0 instanceId services distribDirectoryUrl"
  exit 1
fi

instanceId=$1
services=$2

updateService=updater
distribDirectoryUrl=$3

echo "Download desired scripts"
curl --output scripts.zip ${distribDirectoryUrl}/download-desired-version/scripts --retry 10 --retry-delay 2 --connect-timeout 5 --silent --show-error || exit 1
unzip -o scripts.zip update.sh && rm -f scripts.zip && chmod +x update.sh || exit 1

. ./update.sh runServices "clientDirectoryUrl=${distribDirectoryUrl}" "instanceId=${instanceId}" "services=${services}"
