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

function download {
  rm -f $2
  http_code=`curl $1 --output $2 --write-out "%{http_code}" --connect-timeout 5 --silent --show-error`
  if [ "$?" != "0" ]; then
    exit 1
  elif [ "$1" == "http://*" ] && [ "$http_code" != "200" ]; then
    if [ -f $2 ]; then
      echo "Server returned: "; cat $2; rm $2; echo
    fi
    exit 1
  fi
}

echo "Download desired scripts"
download ${distribDirectoryUrl}/download-desired-version/scripts scripts.zip
unzip -o scripts.zip update.sh && rm -f scripts.zip && chmod +x update.sh || exit 1

. ./update.sh runServices "clientDirectoryUrl=${distribDirectoryUrl}" "instanceId=${instanceId}" "services=${services}"
