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
download ${distribDirectoryUrl}/download-desired-version/scripts scripts.zip
unzip -o scripts.zip updater_setup.sh && chmod +x updater_setup.sh
echo "Execute setup"
./updater_setup.sh Azure ${name} ${services} ${distribDirectoryUrl}