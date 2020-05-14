#!/bin/bash

updateService=builder

function download {
  rm -f $2
  http_code=`curl $1 --output $2 --write-out "%{http_code}" --connect-timeout 5 --silent --show-error`
  if [ $0 == 0 ] && [ $http_code != "200" ]; then
    if [ -f $2 ]; then
      cat $2; rm $2
    fi
    exit 1
  fi
}

echo "Download desired scripts"
distribDirectoryUrl=`jq -r .developerDistributionUrl builder.json`
download ${distribDirectoryUrl}/download-desired-version/scripts scripts.zip
unzip -o scripts.zip update.sh && rm -f scripts.zip && chmod +x update.sh || exit 1

. ./update.sh "$@"
