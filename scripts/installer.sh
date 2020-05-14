#!/bin/bash

updateService=installer

function download {
  rm -f $2
  http_code=`curl $1 --output $2 --write-out "%{http_code}" --connect-timeout 5 --silent --show-error`
  if [ "$?" != "0" ]; then
    exit 1
  elif [[ "$1" == http* ]] && [ "$http_code" != "200" ]; then
    if [ -f $2 ]; then
      echo -n "Server returned: "; cat $2; rm $2; echo
    fi
    exit 1
  fi
}

echo "Download desired scripts"
if [ -z "$distribDirectoryUrl" ]; then
    distribDirectoryUrl=`jq -r .developerDistributionUrl installer.json`
fi
download ${distribDirectoryUrl}/download-desired-version/scripts scripts.zip
unzip -o scripts.zip update.sh || exit 1
rm -f scripts.zip; chmod +x update.sh

. ./update.sh "$@"
